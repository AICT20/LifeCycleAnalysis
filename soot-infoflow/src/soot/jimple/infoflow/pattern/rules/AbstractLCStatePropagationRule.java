package soot.jimple.infoflow.pattern.rules;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.pattern.result.LCMethodSummaryResult;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.ITaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;

abstract public class AbstractLCStatePropagationRule {

    protected final PatternInfoflowManager manager;
    protected final LCMethodSummaryResult results;

    public AbstractLCStatePropagationRule(PatternInfoflowManager manager, LCMethodSummaryResult results) {
        this.manager = manager;
        this.results = results;
    }

    protected PatternInfoflowManager getManager() {
        return this.manager;
    }

    protected Aliasing getAliasing() {
        return this.manager.getAliasing();
    }

    protected LCMethodSummaryResult getResults() {
        return this.results;
    }

    abstract NormalState propagateNormalFlow(NormalState source, Stmt stmt, Stmt destStmt,
                                    ByReferenceBoolean hasGeneratedNewState,
                                    ByReferenceBoolean killAll);

    abstract NormalState propagateCallFlow(NormalState source, Stmt stmt, SootMethod dest,
                                           ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll);

    abstract NormalState propagateCallToReturnFlow(NormalState source, Stmt stmt,
                                                   ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll);

    abstract NormalState propagateReturnFlow(NormalState source, Stmt retSite, Stmt callSite,
                                             ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll);

}
