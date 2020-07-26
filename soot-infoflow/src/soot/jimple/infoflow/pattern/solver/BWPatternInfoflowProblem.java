package soot.jimple.infoflow.pattern.solver;

import heros.FlowFunctions;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;

public class BWPatternInfoflowProblem extends PatternInfoflowProblem{
    public BWPatternInfoflowProblem(PatternInfoflowManager manager) {
        super(manager);
    }

    @Override
    public FlowFunctions<Unit, NormalState, SootMethod> flowFunctions() {
        return null;
    }
}
