package soot.jimple.infoflow.pattern.rules;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.*;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.patternresource.LCResourceOPList;
import soot.jimple.infoflow.pattern.result.LCMethodSummaryResult;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.pattern.sourceandsink.PatternSourceInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.HashSet;
import java.util.Set;

//这部分主要处理strong update，即 r0.field = a;    r0.field taint的情况
public class LCStrongUpdatePropagationRule extends AbstractLCStatePropagationRule{
    public LCStrongUpdatePropagationRule(PatternInfoflowManager manager, LCMethodSummaryResult results) {
        super(manager, results);
    }

    @Override
    NormalState propagateNormalFlow(NormalState source, Stmt stmt, Stmt destStmt, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        if (!(stmt instanceof AssignStmt) || source.isZeroState())
            return source;
        AssignStmt assignStmt = (AssignStmt) stmt;

        // if leftvalue contains the tainted value -> it is overwritten - remove taint:
        // but not for arrayRefs:
        // x[i] = y --> taint is preserved since we do not distinguish between elements
        // of collections
        // because we do not use a MUST-Alias analysis, we cannot delete aliases of
        // taints
        if (assignStmt.getLeftOp() instanceof ArrayRef)
            return source;

        //如果右边已经是taint，说明我们之前的状态中肯定处理过了，那么这里就是 a = b, b taint的情况，不需要做strong update了
        Value rightValue = assignStmt.getRightOp();
        for (AccessPath ap : source.getAps()) {
            if (null != getAliasing().mayAlias(ap, rightValue)) {
                return source;
            }
        }

        //这条rule主要处理 a = b, a taint的情况，但是需要注意:
        //  1. 如果我们 a = b, a, b均为taint的话就保持不变了
        //  2. 如果是 r0.field = b, b并非已知的ap， field是我们的source，那么所有的ap, op都要清空重新开始

        Value leftValue = assignStmt.getLeftOp();
        if (leftValue instanceof InstanceFieldRef || leftValue instanceof StaticFieldRef) {
            if (((FieldRef) leftValue).getField() == source.getDef().getField()) {
                final SourceInfo sourceInfo = getManager().getSourceSinkManager().getSourceInfo(stmt, getManager(), source.getEntryClass());
                // Is this a source?
                if (sourceInfo != null) {
                    //重新算AP
                    NormalState newstate = source.deriveNewState(sourceInfo.getAccessPaths(), hasGeneratedNewState, stmt);
                    //重新算OP
                    LCResourceOPList newoplist = manager.getLCResourceOPHelper().merge(stmt, newstate.getAps(), newstate.getDef(), LCResourceOPList.getInitialList(), getAliasing(), null);
                    newstate = newstate.deriveNewOPListState(hasGeneratedNewState, newoplist, stmt);
                    //这里和source一样，也要向前计算OPalias
                    PatternSourceInfo pSourceInfo = (PatternSourceInfo)sourceInfo;
                    AccessPath apToSlice = pSourceInfo.getApToSlice();
                    if (null != apToSlice) {
                        if (getAliasing().computeAliases(newstate, apToSlice, stmt, true)) {
                            killAll.value = true;
                            return null;
                        }
                    }

                    return newstate;
                }
            }
        }



        Set<AccessPath> removeAps = new HashSet<>();
        for (AccessPath ap : source.getAps()) {
            if (ap.isInstanceFieldRef()) {
                // Data Propagation: x.f = y && x.f tainted --> no taint propagated
                // Alias Propagation: Only kill the alias if we directly overwrite it,
                // otherwise it might just be the creation of yet another alias
                if (leftValue instanceof InstanceFieldRef) {
                    InstanceFieldRef leftRef = (InstanceFieldRef) leftValue;
                    boolean baseAliases = getAliasing().mustAlias((Local) leftRef.getBase(),
                                ap.getPlainValue(), assignStmt);
                    if (baseAliases) {
                        if (getAliasing().mustAlias(leftRef.getField(), ap.getFirstField())) {
                            removeAps.add(ap);
                        }
                    }
                }
                // x = y && x.f tainted -> no taint propagated. This must only check the precise
                // variable which gets replaced, but not any potential strong aliases
                else if (leftValue instanceof Local) {
                        if (getAliasing().mustAlias((Local) leftValue, ap.getPlainValue(),
                                stmt)) {
                            removeAps.add(ap);
                        }
                }
            }
            // X.f = y && X.f tainted -> no taint propagated. Kills are allowed even if
            // static field tracking is disabled
            else if (ap.isStaticFieldRef()) {
                if (leftValue instanceof StaticFieldRef && getAliasing().mustAlias(
                        ((StaticFieldRef) leftValue).getField(), ap.getFirstField())) {
                    removeAps.add(ap);
                }
            }
            // when the fields of an object are tainted, but the base object is overwritten
            // then the fields should not be tainted any more
            // x = y && x tainted -> no taint propagated
            else if (ap.isLocal() && assignStmt.getLeftOp() instanceof Local
                    && assignStmt.getLeftOp() == ap.getPlainValue()) {
                removeAps.add(ap);
            }
        }
        if (removeAps.isEmpty()) {
            return source;
        } else {
            Set<AccessPath> newaps = new HashSet<>(source.getAps());
            newaps.removeAll(removeAps);
            return source.deriveNewState(newaps, hasGeneratedNewState, assignStmt);
        }
    }

    @Override
    NormalState propagateCallFlow(NormalState source, Stmt stmt, SootMethod dest, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        return source;
    }

    @Override
    NormalState propagateCallToReturnFlow(NormalState source, Stmt stmt, SootMethod callee, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        return source;
    }

    @Override
    NormalState propagateReturnFlow(NormalState source, Stmt exitStmt, Stmt retSite, Stmt callSite,SootMethod calleeMethod, ByReferenceBoolean hasGeneratedNewState, ByReferenceBoolean killAll) {
        return source;
    }
}
