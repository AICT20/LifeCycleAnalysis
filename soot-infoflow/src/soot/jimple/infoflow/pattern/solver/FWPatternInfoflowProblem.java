package soot.jimple.infoflow.pattern.solver;

import heros.FlowFunction;
import heros.FlowFunctions;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.pattern.result.LCMethodSummaryResult;
import soot.jimple.infoflow.pattern.rules.PatternPropagationRuleManager;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collections;
import java.util.Set;

public class FWPatternInfoflowProblem extends PatternInfoflowProblem{
    private final PatternPropagationRuleManager propagationRules;
    private final LCMethodSummaryResult result;


    public FWPatternInfoflowProblem(PatternInfoflowManager manager) {
        super(manager);
        result = new LCMethodSummaryResult();
        propagationRules = new PatternPropagationRuleManager(manager, result);
    }

    private FlowFunctions<Unit,NormalState,SootMethod> flowFunctions;
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
                return new FlowFunction<NormalState>() {
                    @Override
                    public Set<NormalState> computeTargets(NormalState source) {
                        NormalState newSource;
                        ByReferenceBoolean hasGeneratedNewSource = new ByReferenceBoolean();
                        if (!source.isAbstractionActive() && curr == source.getActivationUnit()) {
                            newSource = source.getActiveCopy();
                            hasGeneratedNewSource.value = true;
                        }
                        else
                            newSource = source;

                        // Apply the propagation rules
                        ByReferenceBoolean killAll = new ByReferenceBoolean();
                        newSource = propagationRules.applyNormalFlowFunction(newSource, (Stmt)curr,
                                (Stmt) succ, hasGeneratedNewSource, killAll);
                        if (killAll.value)
                            return Collections.emptySet();

                        Set<NormalState> newSources = Collections.singleton(newSource);
                        if (source == zeroValue && newSource != zeroValue) {
                            newSources.add(zeroValue);
                        }
                        return newSources;
                    }
                };
            }

            @Override
            public FlowFunction<NormalState> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod) {
                return new FlowFunction<NormalState>() {
                    @Override
                    public Set<NormalState> computeTargets(NormalState source) {
                        return null;
                    }
                };
            }

            @Override
            public FlowFunction<NormalState> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
                return new FlowFunction<NormalState>() {
                    @Override
                    public Set<NormalState> computeTargets(NormalState source) {
                        return null;
                    }
                };
            }

            @Override
            public FlowFunction<NormalState> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                return new FlowFunction<NormalState>() {
                    @Override
                    public Set<NormalState> computeTargets(NormalState source) {
                        return null;
                    }
                };
            }
        };
    }

}
