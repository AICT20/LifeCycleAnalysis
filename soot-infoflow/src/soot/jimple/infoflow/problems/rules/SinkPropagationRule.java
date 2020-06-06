package soot.jimple.infoflow.problems.rules;

import java.util.Collection;

import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for recording abstractions that arrive at sinks
 * 
 * @author Steven Arzt
 */
//lifecycle-add 很显然，我们的sink只有在return才会出现，因此其他都不要管，只管return就行了
public class SinkPropagationRule extends AbstractTaintPropagationRule {

//	private boolean killState = false;

	public SinkPropagationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
//		if (stmt instanceof ReturnStmt) {
//			final ReturnStmt returnStmt = (ReturnStmt) stmt;
//			checkForSink(d1, source, stmt, returnStmt.getOp());
//		} else if (stmt instanceof IfStmt) {
//			final IfStmt ifStmt = (IfStmt) stmt;
//			checkForSink(d1, source, stmt, ifStmt.getCondition());
//		} else if (stmt instanceof LookupSwitchStmt) {
//			final LookupSwitchStmt switchStmt = (LookupSwitchStmt) stmt;
//			checkForSink(d1, source, stmt, switchStmt.getKey());
//		} else if (stmt instanceof TableSwitchStmt) {
//			final TableSwitchStmt switchStmt = (TableSwitchStmt) stmt;
//			checkForSink(d1, source, stmt, switchStmt.getKey());
//		} else if (stmt instanceof AssignStmt) {
//			final AssignStmt assignStmt = (AssignStmt) stmt;
//			checkForSink(d1, source, stmt, assignStmt.getRightOp());
//		}

		return null;
	}



	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		// If we are in the kill state, we stop the analysis
//		if (killAll != null)
//			killAll.value |= killState;

		return null;
	}

	/**
	 * Checks whether the given taint is visible inside the method called at the
	 * given call site
	 * 
	 * @param stmt   A call site where a sink method is called
	 * @param source The taint that has arrived at the given statement
	 * @return True if the callee has access to the tainted value, false otherwise
	 */
	protected boolean isTaintVisibleInCallee(Stmt stmt, Abstraction source) {
		// If we don't have an alias analysis anymore, we probably in the shutdown phase
		// anyway
		if (getAliasing() == null)
			return false;

		InvokeExpr iexpr = stmt.getInvokeExpr();
		boolean found = false;

		// Is an argument tainted?
		final Value apBaseValue = source.getAccessPath().getPlainValue();
		if (apBaseValue != null) {
			for (int i = 0; i < iexpr.getArgCount(); i++) {
				if (getAliasing().mayAlias(iexpr.getArg(i), apBaseValue)) {
					if (source.getAccessPath().getTaintSubFields() || source.getAccessPath().isLocal())
						return true;
				}
			}
		}

		// Is the base object tainted?
		if (!found && iexpr instanceof InstanceInvokeExpr) {
			if (((InstanceInvokeExpr) iexpr).getBase() == source.getAccessPath().getPlainValue())
				return true;
		}

		return false;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
//		// We only report leaks for active taints, not for alias queries
//		if (source.isAbstractionActive() && !source.getAccessPath().isStaticFieldRef()) {
//			// Is the taint even visible inside the callee?
//			if (!stmt.containsInvokeExpr() || isTaintVisibleInCallee(stmt, source)) {
//				// Is this a sink?
//				if (getManager().getSourceSinkManager() != null) {
//					// Get the sink descriptor
//					SinkInfo sinkInfo = getManager().getSourceSinkManager().getSinkInfo(stmt, getManager(),
//							source.getAccessPath());
//
//					// If we have already seen the same taint at the same sink, there is no need to
//					// propagate this taint any further.
//					if (sinkInfo != null
//							&& !getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinition(), source, stmt))) {
//						killState = true;
//					}
//				}
//			}
//		}
//
//		// If we are in the kill state, we stop the analysis
//		if (killAll != null)
//			killAll.value |= killState;

		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
//		// Check whether this return is treated as a sink
//		if (stmt instanceof ReturnStmt) {
//			final ReturnStmt returnStmt = (ReturnStmt) stmt;
//			boolean matches = source.getAccessPath().isLocal() || source.getAccessPath().getTaintSubFields();
//			if (matches && source.isAbstractionActive() && getManager().getSourceSinkManager() != null
//					&& getAliasing().mayAlias(source.getAccessPath().getPlainValue(), returnStmt.getOp())) {
//				SinkInfo sinkInfo = getManager().getSourceSinkManager().getSinkInfo(returnStmt, getManager(),
//						source.getAccessPath());
//				if (sinkInfo != null
//						&& !getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinition(), source, returnStmt)))
//					killState = true;
//			}
//		}
		//lifecycle-add，在判断时，不需要关注return x里的这个x是哪个，只要关注这个是不是最后的return语句就行了
			final ISourceSinkManager sourceSinkManager = getManager().getSourceSinkManager();
			if (sourceSinkManager != null && source.isAbstractionActive()) {
				SinkInfo sinkInfo = sourceSinkManager.getSinkInfo(stmt, getManager(), source.getAccessPath());
				if (sinkInfo != null) {
					if (!getResults().addResult(new AbstractionAtSink(sinkInfo.getDefinition(), source, stmt))) {
						killAll.value = true;
					} else {
//						System.out.println();
					}
				}
			}

		return null;
	}

}
