package soot.jimple.infoflow.pattern.rules;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.pattern.result.LCMethodSummaryResult;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;

public class LCOperationPropagationRule extends AbstractLCStatePropagationRule {
    public LCOperationPropagationRule(PatternInfoflowManager manager, LCMethodSummaryResult results) {
        super(manager, results);
    }

    @Override
    NormalState propagateNormalFlow(NormalState source, Stmt stmt, Stmt destStmt, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    NormalState propagateCallFlow(NormalState source, Stmt stmt, SootMethod dest, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    NormalState propagateCallToReturnFlow(NormalState source, Stmt stmt, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    NormalState propagateReturnFlow(NormalState source, Stmt retSite, Stmt callSite, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        return null;
    }

}
