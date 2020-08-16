package soot.jimple.infoflow.pattern.problems;

import heros.FlowFunction;
import heros.FlowFunctions;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.cfg.FlowDroidEssentialMethodTag;
import soot.jimple.infoflow.pattern.mappingmethods.MappingMethodHelper;
import soot.jimple.infoflow.pattern.mappingmethods.SootMissingMethodHelper;
import soot.jimple.infoflow.pattern.patterndata.PatternDataHelper;
import soot.jimple.infoflow.pattern.result.LCMethodSummaryResult;
import soot.jimple.infoflow.pattern.rules.PatternPropagationRuleManager;

import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowProblem;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.SystemClassHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FWPatternInfoflowProblem extends PatternInfoflowProblem {
    private final PatternPropagationRuleManager propagationRules;
    private final LCMethodSummaryResult result;
    private final Set<SootMethod> mappingmethods;
    private final SootMissingMethodHelper missingMethodHelper = SootMissingMethodHelper.v();



    public FWPatternInfoflowProblem(PatternInfoflowManager manager) {
        super(manager);
        result = new LCMethodSummaryResult();
        propagationRules = new PatternPropagationRuleManager(manager, result);
        mappingmethods = MappingMethodHelper.v().getMappingmethods().keySet();
    }

    private FlowFunctions<Unit, NormalState,SootMethod> flowFunctions;
    @Override
    public final FlowFunctions<Unit,NormalState,SootMethod> flowFunctions() {
        if(flowFunctions==null) {
            flowFunctions = createFlowFunctionsFactory();
        }
        return flowFunctions;
    }
    //约束：我们永远保持在FlowFunction中只有单个NormalState在进行操作，有且仅有一个例外：zerovalue,它可以额外和正常的state一起传播
    public FlowFunctions<Unit, NormalState, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, NormalState, SootMethod>() {

            @Override
            public FlowFunction<NormalState> getNormalFlowFunction(Unit curr, Unit succ) {
                return source -> {
                    NormalState newState;
                    ByReferenceBoolean hasGeneratedNewState = new ByReferenceBoolean();
                    newState = source.isActive()?source:source.FWCheckCurrentStmt((Stmt)curr, hasGeneratedNewState);

                    // Apply the propagation rules
                    ByReferenceBoolean killAll = new ByReferenceBoolean();
                    newState = propagationRules.applyNormalFlowFunction(newState, (Stmt)curr,
                            (Stmt) succ, hasGeneratedNewState, killAll);
                    if (killAll.value) {
                        if (source.isZeroState()) {
                            return Collections.singleton(source.getZeroState());//就算被kill了，zeroState也要传播下去
                        } else {
                            return Collections.emptySet();
                        }
                    }

                    Set<NormalState> newSources = new HashSet<>();
                    newState.updateActiveAps();
                    newSources.add(newState);
                    if (source.isZeroState() && !newState.isZeroState()) {
                        newSources.add(newState.getZeroState());
                    }
                    return newSources;
                };
            }

            @Override
            public FlowFunction<NormalState> getCallFlowFunction(Unit callStmt, SootMethod dest) {
                return new SolverCallFlowFunction() {
                    @Override
                    public Set<NormalState> computeCallTargets(NormalState source, boolean isRelevant) {
                        //如果是Systemcall等等，就不需要跳进去了
                        if (isExcluded(dest))
                            return null;
                        Set<NormalState> newStates = new HashSet<>();
                        ByReferenceBoolean hasGeneratedNewState = new ByReferenceBoolean();
                        // We might need to activate the abstraction
                        NormalState newState = source.deriveCallState((Stmt)callStmt, hasGeneratedNewState);
                        if (newState.isZeroState()) {
                            newStates.add(newState);
                        }
                         newState = newState.isActive()?newState:newState.FWCheckCurrentStmt((Stmt)callStmt, hasGeneratedNewState);

                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        newState = propagationRules.applyCallFlowFunction(newState, (Stmt)callStmt, dest, hasGeneratedNewState, killAll);
                        if (killAll.value) {
                            return newStates;
                        }

                        if (null != newState) {
                            newState.updateActiveAps();
                            newStates.add(newState);
                        }
                        return newStates;
                    }

                };
            }

            @Override
            public FlowFunction<NormalState> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
                return source -> {
//                    if (source.isZeroState() && !source.shouldFinish(exitStmt))
//                        return Collections.singleton(source);
                    //这部分交给LCFinishPropagationRule来处理

                    ByReferenceBoolean hasGeneratedNewState = new ByReferenceBoolean();
                    NormalState newState = source;
                    Set<NormalState> newStates = new HashSet<>();
                    if (callSite != null && !newState.shouldFinish(exitStmt)) {//这里要做个例外：如果是Entrypoints的return的话，就没有对应的callsite
                        newState = newState.deriveReturnState((Stmt)callSite, hasGeneratedNewState);
                        if (null == newState) {return null;}//如果call和return的点对不上，直接去掉
                        if (newState.isZeroState()) {newStates.add(newState);}
                        newState = newState.isActive()?newState:newState.FWCheckCurrentStmt((Stmt)exitStmt, hasGeneratedNewState);
                    }


                    ByReferenceBoolean killAll = new ByReferenceBoolean();
                    newState = propagationRules.applyReturnFlowFunction(newState,
                            (Stmt) exitStmt, (Stmt) returnSite, (Stmt) callSite, calleeMethod, hasGeneratedNewState, killAll);
                    if (killAll.value)
                        return newStates;
                    // If we have no caller, we have nowhere to propagate.
                    // This can happen when leaving the main method.
                    if (callSite == null)
                        return null;

                    newState.updateActiveAps();
                    newStates.add(newState);
                    return newStates;
                };
            }

            @Override
            public FlowFunction<NormalState> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                return new SolverCallToReturnFlowFunction() {
                    @Override
                    public Set<NormalState> computeCallTargets(NormalState source, boolean isRelevant) {
                        if (!((Stmt)callSite).containsInvokeExpr()) {
                            return null;
                        }
                        InvokeExpr invExpr = ((Stmt) callSite).getInvokeExpr();
                        final SootMethod callee = invExpr.getMethod();//这里有个隐患，这个可能是个system定义的interface，然而实现却是用户实现
                        //如果是Systemcall等等，就需要跳过；而用户定义的函数则需要用callflowFunction而非本function
                        if (!isExcluded(callee))
                            return null;

                        ByReferenceBoolean hasGeneratedNewState = new ByReferenceBoolean();
                        // check inactive elements:
                        NormalState newState = source.isActive()?source:source.FWCheckCurrentStmt((Stmt)callSite, hasGeneratedNewState);

                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        newState = propagationRules.applyCallToReturnFlowFunction(newState, (Stmt)callSite, callee,
                                hasGeneratedNewState, killAll);
                        if (killAll.value)
                            return null;
                        newState.updateActiveAps();
                        return Collections.singleton(newState);
                    }
                };
            }
        };
    }


    protected boolean isExcluded(SootMethod sm) {
        // Is this an essential method?
        if (sm.hasTag(FlowDroidEssentialMethodTag.TAG_NAME))
            return false;

        // We can exclude Soot library classes
        if (sm.getDeclaringClass().isLibraryClass())
            return true;

        // We can ignore system classes according to FlowDroid's definition
        if (SystemClassHandler.isClassInSystemPackage(sm.getDeclaringClass().getName()))
            return true;

        if (missingMethodHelper.isMethodMissing(sm))
            return true;

        return mappingmethods.contains(sm);

//        if (isRelevant) {
//            return false;
//        }
//        if (methodsThatCannotSkip.contains(sm)) {
//            return false;
//        }
//        return true;
    }
}
