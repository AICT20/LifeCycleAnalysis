package soot.jimple.infoflow.problems.rules;

import soot.SootMethod;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.sourcesSinks.definitions.ExitSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;
import java.util.Set;

public class KillSourcePropagationRule extends AbstractTaintPropagationRule{
    //lifecycle add  这个目前仅仅
    protected final boolean computingAlias = false;
    public KillSourcePropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    @Override
    public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        Set<Abstraction> res = null;
        //针对Database.close的修正
        if (stmt.containsInvokeExpr()) {
            InvokeExpr exp = stmt.getInvokeExpr();
            if (exp instanceof InstanceInvokeExpr) {
                if (exp.getMethod().getSignature().contains("close")) {
                    SourceSinkDefinition def = getOriginalSource(source);
                    if (null == def || !(def instanceof ExitSourceSinkDefinition)) {
                        return KillResult.NOTRELATED;
                    }
                    InstanceInvokeExpr baseexp = (InstanceInvokeExpr) exp;
                    Value base = baseexp.getBase();
                    for (AccessPath ap : source.getAccessPaths()) {
                        //调用方法时，都是用的local
                        if (ap.isLocal() && getManager().getAliasing().mayAlias(base, ap.getPlainValue())) {
                            return KillResult.KILLED;
                        }
                    }
                    //这个地方说明这个kill method的执行base与taint不对应，应当回去查查Alias
                    return KillResult.CHECKALIAS;
                }
            }
        }
        return res;
    }


    private SourceSinkDefinition getOriginalSource(Abstraction abs) {
        while (abs.getSourceContext() == null) {
            Abstraction temp = abs.getPredecessor();
            if (null != temp) {
                abs = temp;
            } else {
                return null;
            }
        }
        return abs.getSourceContext().getDefinition();
    }


    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest, ByReferenceBoolean killAll) {
        return null;
    }


    @Override
    public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
        return null;
    }
}
