package soot.jimple.infoflow.pattern.problems;

import heros.FlowFunction;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.pattern.solver.NormalState;

import java.util.Set;

public abstract class SolverCallFlowFunction implements FlowFunction<NormalState> {
    @Override
    public Set<NormalState> computeTargets(NormalState source) {
        return null;
    }

    //额外提供一个提前计算好的，invocation与当前状态的aps的是否alias的关系
    public abstract Set<NormalState> computeCallTargets(NormalState source, boolean isRelevant);
}
