package soot.jimple.infoflow.pattern.patternresource.definition;

import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.pattern.patternresource.LCResourceOPConstant;
import soot.jimple.infoflow.pattern.patternresource.LCResourceOPType;
import soot.jimple.infoflow.pattern.patternresource.enumeration.MergeStrategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LCResourceOPDefinition {
    protected String category;
    protected LCResourceOPType opType;//如EDIT, RELEASE等等最终输出的形式，也是判断路径是否有问题的依据
//    protected Value[] opValue = null;//这个还要再仔细考虑一下
    protected Set<LCMethodDefinition> methodDefs;//一个definition可能包含多个method，而每个method只能转为同一个definition
    protected MergeStrategy mergeStrategy;//用于在加入这个def生成的op时，对oplist应当如何操作；

    public LCResourceOPDefinition(String category, LCResourceOPType opType, Set<LCMethodDefinition> methodDefs, MergeStrategy mergeStrategy) {
        this.category = category;
        this.opType = opType;
        this.methodDefs = methodDefs;
        this.mergeStrategy = mergeStrategy;
    }

    public String getCategory() {
        return category;
    }

    public boolean isDefaultCategory() {return LCResourceOPConstant.DEFAULT.equals(category);}

    public LCResourceOPType getOpType() {
        return opType;
    }

    public Set<LCMethodDefinition> getMethodDefs() {
        return methodDefs;
    }

    public LCMethodDefinition getMethodDef(SootMethod method) {
        for (LCMethodDefinition methodDef : methodDefs) {
            if (methodDef.method == method) {
                return methodDef;
            }
        }
        return null;
    }

    public MergeStrategy getMergeStrategy() {
        return mergeStrategy;
    }
}
