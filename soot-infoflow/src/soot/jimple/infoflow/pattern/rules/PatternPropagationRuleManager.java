package soot.jimple.infoflow.pattern.rules;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.pattern.result.LCMethodSummaryResult;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.*;

public class PatternPropagationRuleManager {
    protected final PatternInfoflowManager manager;
    protected final LCMethodSummaryResult results;
    protected final ArrayList<AbstractLCStatePropagationRule> rules;

    public PatternPropagationRuleManager(PatternInfoflowManager manager, LCMethodSummaryResult results) {
        this.manager = manager;
        this.results = results;

        ArrayList<AbstractLCStatePropagationRule> ruleList = new ArrayList<>();

        //注意！！！！这里的顺序是由讲究的，不能乱排！！！！！！！！！
        ruleList.add(new LCSourcePropagationRule(manager, results));
        ruleList.add(new LCOperationPropagationRule(manager, results));
        ruleList.add(new LCFinishPropagationRule(manager, results));
        ruleList.add(new LCAPUpdatePropagationRule(manager, results));
        ruleList.add(new LCExceptionPropagationRule(manager, results));
        this.rules = ruleList;
    }

    public NormalState applyNormalFlowFunction(NormalState source, Stmt stmt, Stmt destStmt,
                                               ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        NormalState newState = source;
        for (AbstractLCStatePropagationRule rule : rules) {
            newState = rule.propagateNormalFlow(newState, stmt, destStmt, hasGeneratedNewState, killAll);
            if (killAll != null && killAll.value || newState == null)
                return null;
        }
        return newState;
    }

    public NormalState applyCallFlowFunction(NormalState source, Stmt stmt, SootMethod destMethod,
                                               ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        NormalState newState = source;
        for (AbstractLCStatePropagationRule rule : rules) {
            newState = rule.propagateCallFlow(newState, stmt, destMethod, hasGeneratedNewState, killAll);
            if (killAll != null && killAll.value || newState == null)
                return null;
        }
        return newState;
    }

    public NormalState applyCallToReturnFlowFunction(NormalState source, Stmt stmt,
                                             ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        NormalState newState = source;
        for (AbstractLCStatePropagationRule rule : rules) {
            newState = rule.propagateCallToReturnFlow(newState, stmt, hasGeneratedNewState, killAll);
            if (killAll != null && killAll.value || newState == null)
                return null;
        }
        return newState;
    }

    public NormalState applyReturnFlowFunction(NormalState source, Stmt exitstmt, Stmt callmethod,
                                                     ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        NormalState newState = source;
        for (AbstractLCStatePropagationRule rule : rules) {
            newState = rule.propagateReturnFlow(newState, exitstmt, callmethod, hasGeneratedNewState, killAll);
            if (killAll != null && killAll.value || newState == null)
                return null;
        }
        return newState;
    }

}
