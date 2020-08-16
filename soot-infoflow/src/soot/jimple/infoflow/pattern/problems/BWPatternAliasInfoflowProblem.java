package soot.jimple.infoflow.pattern.problems;

import heros.FlowFunction;
import heros.FlowFunctions;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.cfg.FlowDroidEssentialMethodTag;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.alias.PatternAliasing;
import soot.jimple.infoflow.pattern.exceptions.TypeNotMatchException;
import soot.jimple.infoflow.pattern.mappingmethods.MappingMethodHelper;
import soot.jimple.infoflow.pattern.mappingmethods.SootMissingMethodHelper;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowProblem;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//这边既要计算slice(一波到底)，又要计算Alias
//这里static应该不用处理， 因为在正向的传播中全部处理完了
public class BWPatternAliasInfoflowProblem extends PatternInfoflowProblem {
    public BWPatternAliasInfoflowProblem(PatternInfoflowManager manager) {
        super(manager);
        mappingmethods = MappingMethodHelper.v().getMappingmethods().keySet();
        missingMethodHelper = SootMissingMethodHelper.v();
    }
    private FlowFunctions<Unit, NormalState,SootMethod> flowFunctions;
    @Override
    public final FlowFunctions<Unit,NormalState,SootMethod> flowFunctions() {
        if(flowFunctions==null) {
            flowFunctions = createFlowFunctionsFactory();
        }
        return flowFunctions;
    }

    protected SootMissingMethodHelper missingMethodHelper;
    protected Set<SootMethod> mappingmethods;

    public FlowFunctions<Unit, NormalState, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, NormalState, SootMethod>() {

            @Override
            public FlowFunction<NormalState> getNormalFlowFunction(Unit curr, Unit succ) {
                return source -> {
                    ByReferenceBoolean hasGeneratedNewState = new ByReferenceBoolean();
                    NormalState newState = source.BWCheckCurrentStmt((Stmt)curr, hasGeneratedNewState);
                    if (curr instanceof AssignStmt) {
                        PatternAliasing aliasing = manager.getAliasing();
                        AssignStmt assign = (AssignStmt) curr;
                        AccessPath aliasAp = newState.getAliasAp();
                        Stmt activationStmt = newState.getActivationStmt();
                        AccessPath newAliasAp = aliasAp;
                        MultiMap<Stmt, AccessPath> inactiveApMap = newState.getInActivationMap();

                        Value leftVal = assign.getLeftOp();
                        Value rightVal = assign.getRightOp();
                        Type newtype = null;
                        if (rightVal instanceof CastExpr) {
                            newtype = ((CastExpr) rightVal).getCastType();
                            rightVal = ((CastExpr) rightVal).getOp();
                        }
                        MultiMap<Stmt, AccessPath> newInActiveApMap = null;
                        if (activationStmt == null) {
                            boolean shouldCutFirstField = false;
                            if (PatternAliasing.baseMatches(leftVal, aliasAp)) {
                                if (null == newInActiveApMap) {
                                    newInActiveApMap = new HashMultiMap<>(inactiveApMap);
                                }
                                shouldCutFirstField = aliasAp.isFieldRef();
                                newInActiveApMap.put(assign, aliasAp);
                                if (!(rightVal instanceof Constant) && !(rightVal instanceof AnyNewExpr)) {

                                    Type nowtype = newtype == null ? (shouldCutFirstField ? aliasAp.getFirstFieldType() : aliasAp.getBaseType()) : newtype;
                                    newAliasAp = manager.getAccessPathFactory().copyWithNewValue(aliasAp, rightVal, nowtype, shouldCutFirstField);
                                } else {
                                    newAliasAp = null;
                                }
                            }
                        } else {
                            if (aliasAp.isFieldRef()) {
                                if (rightVal instanceof Local && aliasing.mayAlias(aliasAp.getPlainValue(), rightVal)) {
                                    //处理 a = b, b.c (a.d = b, b.c)在未来taint,
                                    if (null == newInActiveApMap) {newInActiveApMap = new HashMultiMap<>(inactiveApMap);}
                                    AccessPath newInactiveAp = manager.getAccessPathFactory().copyWithNewValue(aliasAp, leftVal);
                                    if (null != newInactiveAp) {
                                        newInActiveApMap.put(activationStmt, newInactiveAp);
                                    }
                                }
                                if (leftVal instanceof Local && aliasing.mayAlias(aliasAp.getPlainValue(), leftVal)) {
                                    //处理 b = a, b.c (b = a.d, b.c)在未来taint,
                                    if (null == newInActiveApMap) {newInActiveApMap = new HashMultiMap<>(inactiveApMap);}
                                    newAliasAp = manager.getAccessPathFactory().copyWithNewValue(aliasAp, rightVal);
                                    if (null != newAliasAp) {
                                        newInActiveApMap.put(activationStmt, newAliasAp);
                                    }
                                }
                            }
                        }
                        if ( newAliasAp != aliasAp || newInActiveApMap != null) {
                            newState = newState.deriveNewAliasState(newAliasAp, newInActiveApMap, hasGeneratedNewState, assign);
                        }
                        //如果直接, newAliasAp == null，那么可以直接回传了
                        if (newAliasAp == null) {
                            NormalState fwstate = newState.deriveNormalFromAliasState(assign, hasGeneratedNewState);
                            if (null != fwstate)
                                manager.getForwardSolver().propagate(assign, fwstate, null);
                            return null;
                        }
                    }
                    return Collections.singleton(newState);
                };
            }

            //这里处理a = method(x1,x2) -> return, a, x1, x2.c taint的情况
            protected boolean mapCallee(PatternAliasing aliasing, Stmt stmt, Value leftVal, Value rightVal, AccessPathWrapper aliasApWrapper, Stmt activationStmt, MultiMap<Stmt, AccessPath> newInactiveApMaps)  {
                boolean haschanged = false;
                AccessPath aliasAp = aliasApWrapper.ap;
                if (null == aliasAp){return false;}
                if (null != activationStmt && aliasAp.isFieldRef() && aliasing.mayAlias(aliasAp.getPlainValue(), leftVal)) {
                    //处理 a = method(),  a或者a.c在未来taint的情况
                    AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(aliasAp, rightVal);
                    aliasApWrapper.ap = newAp;
                    if (null != newAp) {
                        newInactiveApMaps.put(activationStmt, newAp);
                    }

                    haschanged = true;
                } else if (PatternAliasing.baseMatches(leftVal, aliasAp) && null == activationStmt) {
                    AccessPath newAp = manager.getAccessPathFactory().copyWithNewValue(aliasAp, rightVal);
                    aliasApWrapper.ap = newAp;
                    haschanged = true;
                }
                return haschanged;
            }

                 class AccessPathWrapper{
                public AccessPath ap = null;
                public AccessPathWrapper(AccessPath ap) {
                    this.ap = ap;
                }
            }

            @Override
            public FlowFunction<NormalState> getCallFlowFunction(Unit callStmt, SootMethod callee) {
				final Value[] paramLocals = new Value[callee.getParameterCount()];
				for (int i = 0; i < callee.getParameterCount(); i++)
					paramLocals[i] = callee.getActiveBody().getParameterLocal(i);
				final Stmt stmt = (Stmt) callStmt;
				final InvokeExpr ie = (stmt != null && stmt.containsInvokeExpr()) ? stmt.getInvokeExpr() : null;
//				final boolean isReflectiveCallSite = interproceduralCFG().isReflectiveCallSite(ie);
//				// This is not cached by Soot, so accesses are more expensive
//				// than one might think
				final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
                return new BWSolverCallFlowFunction() {
                    @Override
                    public Set<NormalState> computeCallTargets(NormalState source, Unit returnstmt, boolean isRelevant) {
                        //如果是Systemcall等等，就不需要跳进去了
                        if (isExcluded(callee))
                            return null;
                        ByReferenceBoolean hasGeneratedNewState = new ByReferenceBoolean();
                        //这里也是和FW相同
                        NormalState newState = source.deriveCallState((Stmt)callStmt, hasGeneratedNewState);
                        PatternAliasing aliasing = getManager().getAliasing();
                        newState = newState.BWCheckCurrentStmt((Stmt)callStmt, hasGeneratedNewState);
                        AccessPath aliasAp = newState.getAliasAp();
                        MultiMap<Stmt, AccessPath> inactiveApMap = newState.getInActivationMap();
                        MultiMap<Stmt, AccessPath> newInactiveApMap = new HashMultiMap<>(inactiveApMap);
                        AccessPathWrapper aliasApWrapper = new AccessPathWrapper(aliasAp);

                        boolean haschanged = false;
                        // if the returned value is tainted - taint values from
                        // return statements
                        if (callStmt instanceof DefinitionStmt && returnstmt instanceof ReturnStmt) {

                            DefinitionStmt defnStmt = (DefinitionStmt) callStmt;
                            Value leftOp = defnStmt.getLeftOp();
                            ReturnStmt rStmt = (ReturnStmt) returnstmt;
                            //还需要特别处理一下 return null, return true这样返回Constant的情况
                            Value rightOp = rStmt.getOp();
                            if (rightOp instanceof Constant) {
                                Stmt activationStmt = newState.getActivationStmt();
                                if (null != activationStmt && aliasAp.isFieldRef() && aliasing.mayAlias(aliasAp.getPlainValue(), leftOp)) {
                                    haschanged = true;
                                } else if (PatternAliasing.baseMatches(leftOp, aliasAp) && null == activationStmt) {
                                    newInactiveApMap.put(rStmt, aliasAp);
                                    aliasApWrapper.ap = null;
                                    haschanged = true;
                                }
                                if (haschanged) {
                                    newState = newState.deriveNewAliasState(aliasApWrapper.ap, newInactiveApMap, hasGeneratedNewState, (Stmt)callStmt);
                                    NormalState fwstate = newState.deriveNormalFromAliasState((Stmt)callStmt, hasGeneratedNewState);
                                    if (null != fwstate)
                                        manager.getForwardSolver().propagate(returnstmt, fwstate, null);
                                    return null;
                                }
                            } else {
                                haschanged |= mapCallee(aliasing, (Stmt)returnstmt, leftOp, rStmt.getOp(), aliasApWrapper, source.getActivationStmt(), newInactiveApMap);
                            }
                        }


                        // checks: this/fields
                        if (!callee.isStatic()) {
                            InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
                            Value callBase = iIExpr.getBase();
                            haschanged |= mapCallee(aliasing, (Stmt)returnstmt, callBase, thisLocal, aliasApWrapper, newState.getActivationStmt(), newInactiveApMap);
                        }

                        if (ie != null && callee.getParameterCount() > 0) {
                            assert callee.getParameterCount() == ie.getArgCount();
                            // check if param is tainted:
                            for (int i = 0; i < ie.getArgCount(); i++) {
                                Value leftVal = ie.getArg(i);
                                Value rightVal = paramLocals[i];
                                haschanged |= mapCallee(aliasing, (Stmt)returnstmt, leftVal, rightVal, aliasApWrapper, newState.getActivationStmt(), newInactiveApMap);
                            }
                        }

                        if (aliasApWrapper.ap == null) {
                            //如果无法生成新的ap，说明type检查时有问题，这条路径有问题，直接退出
                            return null;
                        }
                        if (haschanged) {
                            newState = newState.deriveNewAliasState(aliasApWrapper.ap, newInactiveApMap, hasGeneratedNewState, (Stmt)callStmt);
                        }

                        return Collections.singleton(newState);
                    }
                };
            }

            @Override
            public FlowFunction<NormalState> getReturnFlowFunction(Unit callSite, SootMethod callee, final Unit exitStmt, Unit returnSite) {
                final Value[] paramLocals = new Value[callee.getParameterCount()];
                for (int i = 0; i < callee.getParameterCount(); i++)
                    paramLocals[i] = callee.getActiveBody().getParameterLocal(i);
                final Stmt stmt = (Stmt) callSite;
                final InvokeExpr ie = (stmt != null && stmt.containsInvokeExpr()) ? stmt.getInvokeExpr() : null;
//				final boolean isReflectiveCallSite = interproceduralCFG().isReflectiveCallSite(ie);
//				// This is not cached by Soot, so accesses are more expensive
//				// than one might think
                final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
                return source -> {
//						// If we have no caller, we have nowhere to propagate.
//						// This can happen when leaving the main method.
                    ByReferenceBoolean hasGeneratedNewState = new ByReferenceBoolean();
                    //这里是离开主函数，BW分析需要从这里转成正向分析
                    if (callSite == null) {
                        NormalState fwstate = source.deriveNormalFromAliasState(stmt, hasGeneratedNewState);
                        if (null != fwstate)
                            manager.getForwardSolver().propagate(exitStmt, fwstate, null);
                        return null;
                    }
                    //这里需要和FW反过来，相当于跳出去
                    NormalState newState = source.deriveCallState((Stmt)callSite, hasGeneratedNewState);
                    if (null == newState) {return null;}
                    newState = newState.BWCheckCurrentStmt((Stmt)callSite, hasGeneratedNewState);
                    PatternAliasing aliasing = getManager().getAliasing();
                    AccessPath aliasAp = newState.getAliasAp();
                    AccessPathWrapper aliasApWrapper = new AccessPathWrapper(aliasAp);
                    MultiMap<Stmt, AccessPath> inactiveApMap = newState.getInActivationMap();
                    MultiMap<Stmt, AccessPath> newInactiveApMap = new HashMultiMap<>(inactiveApMap);

                    boolean haschanged = false;

                    // Map the "this" local
                    if (!callee.isStatic() && callSite instanceof Stmt) {
                        Value leftVal = thisLocal;
                        Stmt callstmt = (Stmt) callSite;
                        if (callstmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                            InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) callstmt.getInvokeExpr();
                            Value callerBaseLocal = iIExpr.getBase();
                            haschanged |= mapCallee(aliasing, (Stmt)callSite, leftVal, callerBaseLocal, aliasApWrapper, newState.getActivationStmt(), newInactiveApMap);
                        }
                    }
                    for (int i = 0; i < paramLocals.length; i++) {
                        if (callSite instanceof Stmt) {
                            Value originalCallArg = ie.getArg(i);
                            // If this is a constant parameter, we
                            // can safely ignore it
                            if (!AccessPath.canContainValue(originalCallArg))
                                continue;

                            // If the variable was overwritten
                            // somewehere in the callee, we assume
                            // it to overwritten on all paths (yeah,
                            // I know ...) Otherwise, we need SSA
                            // or lots of bookkeeping to avoid FPs
                            // (BytecodeTests.flowSensitivityTest1).
                            if (interproceduralCFG().methodWritesValue(callee, paramLocals[i]))
                                continue;
                            haschanged |= mapCallee(aliasing, (Stmt)callSite, paramLocals[i], originalCallArg, aliasApWrapper, newState.getActivationStmt(), newInactiveApMap);
                        }
                    }
                    if (aliasApWrapper.ap == null) {
                        //如果无法生成新的ap，说明type检查时有问题，这条路径有问题，直接退出
                        return null;
                    }

                    if (haschanged) {
                        newState = newState.deriveNewAliasState(aliasApWrapper.ap, newInactiveApMap, hasGeneratedNewState, (Stmt)callSite);
                    } else {
                        Stmt activationStmt = newState.getActivationStmt();
                        if (null != activationStmt && icfg.getMethodOf(activationStmt) == callee) {
                            //如果没有参数映射，并且当前的函数就是Alias开始的函数，那么直接开始正向传播
                            NormalState fwstate = newState.deriveNormalFromAliasState((Stmt)exitStmt, hasGeneratedNewState);
                            if (null != fwstate)
                                manager.getForwardSolver().propagate((Stmt)exitStmt, fwstate, null);
                            return null;
                        }
                    }
                    return Collections.singleton(newState);
                };
            }

            @Override
            public FlowFunction<NormalState> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                return source -> {
                    if (!((Stmt)callSite).containsInvokeExpr()) {
                        return null;
                    }
                    InvokeExpr invExpr = ((Stmt) callSite).getInvokeExpr();
                    final SootMethod callee = invExpr.getMethod();//这里有个隐患，这个可能是个system定义的interface，然而实现却是用户实现
                    //如果是Systemcall等等，就需要跳过；而用户定义的函数则需要用callflowFunction而非本function
                    if (!isExcluded(callee))
                        return null;
                    ByReferenceBoolean hasGeneratedNewState = new ByReferenceBoolean();
                    NormalState newState = source.BWCheckCurrentStmt((Stmt)callSite, hasGeneratedNewState);
                    //注意，现在新加了MappingMethod这条rule后，源可能会从正向的CallToReturn里边产生，因此这里也需要对应一下
                    if (callSite instanceof DefinitionStmt) {
                        Value leftVal = ((DefinitionStmt) callSite).getLeftOp();
                        AccessPath aliasAp = newState.getAliasAp();
                        if (PatternAliasing.baseMatches(leftVal, aliasAp)) {
                            Stmt activationStmt = newState.getActivationStmt();
                            if (null == activationStmt) {
                                MultiMap<Stmt, AccessPath> newInactiveMaps = new HashMultiMap<>(newState.getInActivationMap());
                                newInactiveMaps.put((Stmt)callSite, aliasAp);
                                newState = newState.deriveNewAliasState(null, newInactiveMaps, hasGeneratedNewState, (Stmt)callSite);
                            }
                            NormalState fwState = newState.deriveNormalFromAliasState((Stmt)callSite, hasGeneratedNewState);
                            if (null != fwState) {
                                manager.getForwardSolver().propagate(callSite, fwState, null);
                            }
                            return null;
                        }
                    }

                    return Collections.singleton(newState);
                };
            }
        };
    }

    protected boolean isExcluded(SootMethod sm) {
        if (null == sm) {
            System.out.println();
        }
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
    }

    //演示和参考的，不实际使用
    private void originalUpdateActiveAndInactiveAps(NormalState newState, Stmt curr) {
        PatternAliasing aliasing = manager.getAliasing();
        AssignStmt assign = (AssignStmt)curr;
        Set<AccessPath> activeAps = newState.getAps();
        Set<AccessPath> inactiveAps = newState.getInactiveAps();
        Set<AccessPath> allAps = new HashSet<>();
        allAps.addAll(activeAps);
        allAps.addAll(inactiveAps);

        Value leftVal = assign.getLeftOp();
        Value rightVal = assign.getRightOp();
        Type newtype = null;
        if (rightVal instanceof  CastExpr) {
            rightVal = ((CastExpr)rightVal).getOp();
            newtype = rightVal.getType();
        }
        //这里可能需要用到 BaseSelector.selectBase();来处理一下特殊情况
        Set<AccessPath> newActiveAps = new HashSet<>(activeAps);
        MultiMap<Stmt, AccessPath> newInActiveApMap = new HashMultiMap<>(newState.getInActivationMap());
        boolean hasChanged = false;

        for (AccessPath ap : allAps) {
            AccessPath mappedAp = null;
            AccessPath newAp = null;

            if ((mappedAp = aliasing.mayAlias(ap, leftVal)) != null) {
                //处理 a.b = c; a.b taint, 以及 a = c; a taint (a = c.b, a taint),的情况，
                if (activeAps.contains(ap)) {
                    // 如果ap为active型，此时aps中去掉当前ap，并且把当前该ap加入到inactivemap里
                    newActiveAps.remove(ap);
                    newInActiveApMap.put(assign, ap);
                    if (!(rightVal instanceof  Constant) && !(rightVal instanceof AnyNewExpr)) {
                        if (mappedAp.isFieldRef() && rightVal instanceof Local) {
                            newAp = manager.getAccessPathFactory().copyWithNewValue(mappedAp, rightVal, mappedAp.getBaseType(), true);
                        } else {
                            newAp = manager.getAccessPathFactory().copyWithNewValue(mappedAp, rightVal);
                        }
                        newActiveAps.add(newAp);
                    }
                } else {
                    //如果ap为inactive型就不用管了
                }
            } else if (!ap.isLocal() && aliasing.mayAlias(ap.getPlainValue(), leftVal)) {
                //处理a = c; a.b taint, (a = c.x; a.b taint)的情况
                if (activeAps.contains(ap)) {
                    // 如果ap为active型，此时aps中去掉当前ap，并且把当前该ap加入到inactivemap里, 同上
                    newActiveAps.remove(ap);
                    newInActiveApMap.put(assign, ap);
                    if (!(rightVal instanceof  Constant) && !(rightVal instanceof AnyNewExpr)) {
                        newAp = manager.getAccessPathFactory().copyWithNewValue(ap, rightVal);
                        newActiveAps.add(newAp);
                    }
                } else {
                    //如果ap为inaction，也是什么都不做
                }
            }

            if ((mappedAp = aliasing.mayAlias(ap, rightVal)) != null) {
                //遇到 b = a.c; a.c taint,或者 b = a; a taint, 或者b.c = a a taint的情况
                if (activeAps.contains(ap)) {
                    //如果ap为active的情况，把左边的加入inactivemap——》这个正向的时候已经包含在内了
                } else {
                    //如果ap为inactive的情况，也什么都不做
                }
            } else if (!ap.isLocal() && aliasing.mayAlias(ap.getPlainValue(), rightVal)) {
                //遇到b = a, a.c taint, b.x = a, a.c taint的情况
                if (activeAps.contains(ap)) {
                    //如果ap为active的情况，把左边的加入inactivemap——》这个正向的时候已经包含在内了
                } else {
                    //如果ap为inactive的情况，要处理b = a a.c taint的情况
                    Stmt activeStmt = null;
                    for (Stmt s : newState.getInActivationMap().keySet()) {
                        Set<AccessPath> tempaps = newState.getInactiveAps();
                        if (tempaps.contains(s)) {
                            activeStmt = s;
                            break;
                        }
                    }
                    assert activeStmt!=null;
                    AccessPath newap = manager.getAccessPathFactory().copyWithNewValue(ap, rightVal);
                    newInActiveApMap.put(activeStmt, newap);
                }

            }
        }
    }
}
