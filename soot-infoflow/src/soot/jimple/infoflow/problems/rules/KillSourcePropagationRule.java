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
import soot.jimple.infoflow.util.MyOwnUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class KillSourcePropagationRule extends AbstractTaintPropagationRule{
    public KillSourcePropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }

    @Override
    public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        AccessPath ap = source.getAccessPath();
        assert ap != null;
        Set<Abstraction> res = null;

        //针对Database.close的修正
        if (stmt.containsInvokeExpr()) {
            InvokeExpr exp = stmt.getInvokeExpr();
            if (exp instanceof InstanceInvokeExpr) {
                if (exp.getMethod().getSignature().contains("close")) {
                    SourceSinkDefinition def = MyOwnUtils.getOriginalSource(source);
                    if (null == def || !(def instanceof ExitSourceSinkDefinition)) {
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
        }
        return res;
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
