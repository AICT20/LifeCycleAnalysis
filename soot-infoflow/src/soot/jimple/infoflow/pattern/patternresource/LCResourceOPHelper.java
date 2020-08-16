package soot.jimple.infoflow.pattern.patternresource;

import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.*;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.alias.PatternAliasing;
import soot.jimple.infoflow.pattern.patternresource.definition.LCEditOPDefinition;
import soot.jimple.infoflow.pattern.patternresource.definition.LCMethodDefinition;
import soot.jimple.infoflow.pattern.patternresource.definition.LCResourceOPDefinition;
import soot.jimple.infoflow.pattern.patternresource.definition.LCSPMethodDefinition;
import soot.jimple.infoflow.pattern.sourceandsink.PatternFieldSourceSinkDefinition;
import soot.jimple.infoflow.pattern.sourceandsink.PatternOpDataProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LCResourceOPHelper {
    private static LCResourceOPHelper instance = null;
    protected Set<LCResourceOPDefinition> defs = null;
    protected LCEditOPDefinition editdef = null;
    protected Map<SootMethod, LCResourceOPDefinition> methodDefMaps = null;
    private LCResourceOPHelper(PatternOpDataProvider provider) {
        defs = provider.getAllDefs();
        for (LCResourceOPDefinition def : defs) {
            if (def instanceof  LCEditOPDefinition){editdef = (LCEditOPDefinition)def; break;}
        }
        methodDefMaps = new HashMap<>();
        for (LCResourceOPDefinition def :defs) {
            Set<LCMethodDefinition> methoddefs = def.getMethodDefs();
            if (null != methoddefs && !methoddefs.isEmpty()) {
                for (LCMethodDefinition methoddef : methoddefs) {
                    methodDefMaps.put(methoddef.getMethod(), def);
                }
            }
        }
        spmethodDefMap = new ConcurrentHashMap<>();
    }
    public static LCResourceOPHelper init(PatternOpDataProvider provider) {
        instance = new LCResourceOPHelper(provider);
        return instance;
    }

    //extraVal是特别为a = method(){return null;}的情况准备的, a 即为extraVal
    public LCResourceOPList merge(Stmt stmt, Set<AccessPath> aps, PatternFieldSourceSinkDefinition sourceDef, LCResourceOPList originalList, PatternAliasing aliasing, Value extraVal) {
        boolean isrelevant = false;
        LCResourceOPDefinition def = null;
        String category = null;
        if (stmt.containsInvokeExpr()) {
            //这里Method类型
            InvokeExpr expr = stmt.getInvokeExpr();
            def = methodDefMaps.get(expr.getMethod());
            if (null == def) {return originalList;}
            String originalCategory = originalList.getCategory();
            String newCategory = def.getCategory();
            if (!originalCategory.equals(newCategory)) {
                if (!originalCategory.equals(LCResourceOPConstant.DEFAULT) && ! newCategory.equals(LCResourceOPConstant.DEFAULT)) {
                    return originalList;
                } else if (originalCategory.equals(LCResourceOPConstant.DEFAULT)) {
                    category = newCategory;
                } else if (newCategory.equals(LCResourceOPConstant.DEFAULT)) {
                    category = originalCategory;
                } else {
                    assert false;//这里是绝对不可能出现的
                }
            } else {
                category = originalCategory;
            }

            LCMethodDefinition methoddef = def.getMethodDef(expr.getMethod());
            isrelevant = isMethodInvocationRelevant(stmt, expr, aps, methoddef, aliasing);

        } else if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            isrelevant = isAssignmentRelevant(leftOp, aps, sourceDef, aliasing);
            def = editdef;
            category = originalList.getCategory();
        } else if (stmt instanceof ReturnStmt) {
            Value leftOp = extraVal;
            isrelevant = isAssignmentRelevant(leftOp, aps, sourceDef, aliasing);
            def = editdef;
            category = originalList.getCategory();
        }

        if (isrelevant) {
            LCResourceOP newop = new LCResourceOP(stmt, def.getOpType(), def);//这里可以弄个享元模式，减少内存消耗
            LCResourceOPList newlist = originalList.merge(newop, def.getMergeStrategy(), category);
            return newlist;
        }
        return originalList;
    }

    //为SP method的调用而特殊准备的，这里不需要alias匹配，因为在外围我们已经匹配好了
    protected Map<SootMethod, LCSPMethodDefinition> spmethodDefMap = null;
    public LCResourceOPList mergeSPMethodCall(Stmt stmt, SootMethod m, LCResourceOPList originalList) {
        LCSPMethodDefinition spdef = spmethodDefMap.computeIfAbsent(m, k-> LCSPMethodDefinition.getLCSPMethodDefinition(m));
        LCResourceOP newop = new LCResourceOP(stmt, spdef.getOpType(), spdef);
        LCResourceOPList newlist = originalList.merge(newop, spdef.getMergeStrategy(), originalList.getCategory());
        return newlist;
    }

    //这里leftop肯定不为null
    private boolean isMethodInvocationRelevant(Stmt stmt, InvokeExpr expr, Set<AccessPath> aps, LCMethodDefinition methoddef, PatternAliasing aliasing) {
        if (methoddef.needNotCheck()) {
            return true;
        }

        Value leftop = stmt instanceof  AssignStmt ? ((AssignStmt) stmt).getLeftOp():null;
        Value base = expr instanceof InstanceInvokeExpr ? ((InstanceInvokeExpr) expr).getBase():null;
        for (AccessPath ap : aps) {
            if (methoddef.needCheckAssignedValue()) {
                if (null != aliasing.mayAlias(ap, leftop)) {
                    return true;
                }
            }
            if (methoddef.needCheckBase()) {
                if (null != aliasing.mayAlias(ap, base)) {
                    return true;
                }
            }
            if (null != methoddef.getCheckParams()) {
                for (int paramNum : methoddef.getCheckParams()) {
                    if (null != aliasing.mayAlias(ap, expr.getArg(paramNum))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isAssignmentRelevant(Value leftOp, Set<AccessPath> aps, PatternFieldSourceSinkDefinition sourceDef, PatternAliasing aliasing) {
        if (aps.isEmpty() || sourceDef == null) {return false;}//只有当是zeroValue的时候才会出现
        //只有当是field类型的赋值时 r0.field = xx，才会计入EDIT行为
        //同时需要注意可能存在的alias
        if (leftOp instanceof InstanceFieldRef || leftOp instanceof StaticFieldRef) {
            SootField field = ((FieldRef) leftOp).getField();
            if (field == sourceDef.getField()) {
                return true;
            }
            for (AccessPath ap : aps) {
                if (null != aliasing.mayAlias(ap, leftOp)) {
                    return true;
                }
            }
        }
        return false;
    }
}
