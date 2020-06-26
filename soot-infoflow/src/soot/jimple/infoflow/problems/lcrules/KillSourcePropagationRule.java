package soot.jimple.infoflow.problems.lcrules;

import soot.*;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.resourceleak.ResourceLeakConstants;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.MyOwnUtils;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JNeExpr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class KillSourcePropagationRule extends AbstractTaintPropagationRule {
    public KillSourcePropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    @Override
    public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        AccessPath ap = source.getAccessPath();
        assert ap != null;
        Set<Abstraction> res = null;

        if (stmt.containsInvokeExpr()) {
            InvokeExpr exp = stmt.getInvokeExpr();
            if (exp instanceof InstanceInvokeExpr) {
                String sig = exp.getMethod().getSignature();
                ISourceSinkManager sourcesinkmanager = getManager().getSourceSinkManager();
                if (sourcesinkmanager.isKillStmt(sig)) {
                    SourceSinkDefinition def = MyOwnUtils.getOriginalSource(source);
                    if (!sourcesinkmanager.shouldKillCurrentSource(sig, def)) {
                        return null;
                    }
                    InstanceInvokeExpr baseexp = (InstanceInvokeExpr) exp;
                    Value base = baseexp.getBase();
                    //调用方法时，都是用的local
                    if (ap.isLocal() && getManager().getAliasing().mayAlias(base, ap.getPlainValue())) {
                        //说明是当前taint直接被kill了
                        TaintPropagationResults.addKillStmts(def, stmt);
                        killAll.value = true;
                        return null;
                    } else {
                        //说明，虽然遇到了kill语句，但是不清楚当前的ap与kill的base是否为alias
                        if (TaintPropagationResults.shouldBeKilledForKillStmts(def, stmt)) {
                            //这里是，同一source的taint，它有个alias是在这个stmt被kill的，那我们也直接kill了
                            TaintPropagationResults.addKillStmts(def, stmt);
                            killAll.value = true;
                            return null;
                        } else {
                            //如果还是不匹配，还有可能是TaintPropagationResults中更新还不全，那就
                            Abstraction newAbs = source.deriveNewAbstractionOnKill(stmt);
                            killSource.value = true;//原Source需要被kill
                            if (null ==res) {
                                res = new HashSet<>();
                            }
                            res.add(newAbs);
                        }
                    }

                }

            }

            //额外的，如果是跳过ContentProvider以及Service的onCreate方法时，也全断了
//            SootMethod m = exp.getMethod();
//            if (isContentProviderOrServiceOnCreate(m)) {
//                killAll.value = true;
//            }
        }
        return res;
    }






    //针对 a = taint
    //   if (a == null)
    //进行kill
    //TODO 这里可加缓存加速传播
    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        if (stmt instanceof IfStmt) {
            Value condition = ((IfStmt) stmt).getCondition();
            Value checkingVal = null;
            Stmt target = ((IfStmt) stmt).getTarget();
            boolean isEquals = false;
            if (condition instanceof JEqExpr) {
                Value left = ((JEqExpr) condition).getOp1();
                Value right = ((JEqExpr) condition).getOp2();
                isEquals = true;
                if (left.getType() instanceof NullType && !(right.getType() instanceof PrimType)) {
                    checkingVal = right;
                } else if (right.getType() instanceof NullType && !(left.getType() instanceof PrimType)) {
                    checkingVal = left;
                }
            } else if (condition instanceof JNeExpr) {
                Value left = ((JNeExpr) condition).getOp1();
                Value right = ((JNeExpr) condition).getOp2();
                isEquals = false;
                if (left.getType() instanceof NullType && !(right.getType() instanceof PrimType)) {
                    checkingVal = right;
                } else if (right.getType() instanceof NullType && !(left.getType() instanceof PrimType)) {
                    checkingVal = left;
                }
            }
            if (null == checkingVal) {
                return null;
            }

            // 这里加个类型检查
            SourceSinkDefinition def = MyOwnUtils.getOriginalSource(source);

            ISourceSinkManager sourcesinkmanager = getManager().getSourceSinkManager();
            if (!sourcesinkmanager.canBeLeakObjects(checkingVal.getType(), def)) {
                return null;
            }

            //先更新一下killifstmts
            AccessPath ap = source.getAccessPath();
            if (null == ap) {
                return null;
            }
            if (getAliasing().mayAlias(checkingVal, ap.getPlainValue())) {
                //这里不一定kill，因为走的可能不是a==null的这个跳转
                TaintPropagationResults.addKillIfStmts(def, (IfStmt)stmt, isEquals);
            }

            //说明，这个时候走的是 a == null的分支，先检查是否需要被kill
            if (TaintPropagationResults.shouldBeKilledForIfStmts(def, (IfStmt)stmt, target == destStmt)) {
                killAll.value = true;
                return null;
            } else {
                //最后剩下的情况下，就需要修正原有source，让它带有ifcheck
                killSource.value = true;
                Abstraction newSource = source.deriveNewAbstractionOnIfKill((IfStmt)stmt, target == destStmt);
                return Collections.singleton(newSource);
            }
        }
        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest, ByReferenceBoolean killAll) {
        return null;
    }



    //这是针对ContentProvider的情况（ContentProvider的database资源leak是不用管的）
    //作法：在离开ContentProvider的onCreate方法时，进行kill ----修正，这部分也需要在calltoreturn中进行
    @Override
    public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
//        if (null == callSite || !callSite.containsInvokeExpr()) {
//            return null;
//        }
//        SootMethod m = callSite.getInvokeExpr().getMethod();
//        if (isContentProviderOrServiceOnCreate(m)) {
//            killAll.value = true;
//        }

        return null;
    }

    private boolean isContentProviderOrServiceOnCreate(SootMethod m) {
        String subsig = m.getSubSignature();
        SootClass declareClass = m.getDeclaringClass();
        if ((subsig.equals(ResourceLeakConstants.ContentProviderOnCreateMethodSubSig) && ResourceLeakConstants.isContentProvider(declareClass))
                || (subsig.equals(ResourceLeakConstants.ServiceOnCreateMethodSubSig) && ResourceLeakConstants.isService(declareClass))) {
            return true;
        }
        return false;
    }
}
