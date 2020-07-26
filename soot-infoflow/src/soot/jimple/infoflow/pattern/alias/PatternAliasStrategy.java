package soot.jimple.infoflow.pattern.alias;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.pattern.solver.NormalSolver;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.solver.IInfoflowSolver;

public class PatternAliasStrategy implements IPatternAliasingStrategy{
    protected PatternInfoflowManager manager = null;
    protected NormalSolver bSolver = null;
    public PatternAliasStrategy(PatternInfoflowManager manager, NormalSolver backwardsSolver) {
        this.manager = manager;
        this.bSolver = backwardsSolver;
    }

    @Override
    public void computeAliasTaints(NormalState source, Stmt src, SootMethod method) {

    }

    @Override
    public NormalSolver getSolver() {
        return null;
    }

    @Override
    public void cleanup() {

    }
}
