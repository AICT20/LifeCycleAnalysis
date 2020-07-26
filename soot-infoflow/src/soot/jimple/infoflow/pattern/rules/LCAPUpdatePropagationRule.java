package soot.jimple.infoflow.pattern.rules;

import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.pattern.result.LCMethodSummaryResult;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;

public class LCAPUpdatePropagationRule extends AbstractLCStatePropagationRule {
    public LCAPUpdatePropagationRule(PatternInfoflowManager manager, LCMethodSummaryResult results) {
        super(manager, results);
    }

    @Override
    NormalState propagateNormalFlow(NormalState source, Stmt curr, Stmt destStmt, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        if (curr instanceof AssignStmt) {
            final AssignStmt assignStmt = (AssignStmt) curr;
            final Value right = assignStmt.getRightOp();
            final Value[] rightVals = BaseSelector.selectBaseList(right, true);

            // Create the new taints that may be created by this
            // assignment
            NormalState newSource = createNewTaintOnAssignment(assignStmt, rightVals, source, hasGeneratedNewState);
            return newSource;
        }
        return source;
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
