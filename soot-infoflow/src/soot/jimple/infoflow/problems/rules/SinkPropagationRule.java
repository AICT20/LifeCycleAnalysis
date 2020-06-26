package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.MyOwnUtils;

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
//		if (isTaintVisibleInCallee(stmt, source) && source.isAbstractionActive()) {
//			//这个时候，这个是 method(a) a taint的情况，因此当这个method return时，a也始终可在函数外被kill
//			//需要将这个method的所有return语句加入allreturnstmts
//			SourceSinkDefinition def = MyOwnUtils.getOriginalSource(source);
//			if (null != def) {
//				IInfoflowCFG icfg = getManager().getICFG();
////				if (dest.getName().contains("onException")) {
////					System.out.println();
////				}
//				Collection<Unit> returnStmts = icfg.getEndPointsOf(dest);
//				for (Unit u: returnStmts) {
//					TaintPropagationResults.addReturnStmts(def, (Stmt)u);
//				}
//
//			}
//		}

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
            return ((InstanceInvokeExpr) iexpr).getBase() == source.getAccessPath().getPlainValue();
		}

		return false;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return null;
	}


	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt,
			Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
//		// Check whether this return is treated as a sink
		//这里的sink主要有2个，一个是到达最后一行，另一个是当前的aps无法传递至函数外
		AccessPath ap = source.getAccessPath();
		if (null == ap) {
			killAll.value = true;
			return null;
			//ap为null肯定有问题
		}
		if (!source.isAbstractionActive()) {
			return null;
		}
		//这个是第一种
		final ISourceSinkManager sourceSinkManager = getManager().getSourceSinkManager();

		SinkInfo sink1Info = sourceSinkManager.getSinkInfo(stmt, getManager(), null);
		if (sink1Info != null) {
			if (!getResults().addResult(new AbstractionAtSink(sink1Info.getDefinition(), source, stmt))) {
				killAll.value = true;
				return null;
			} else {
				//如果是intracomponent的话，传递不能超过component的最后一句，而这最后一句就是这个情况下的sink点
				if (getManager().getConfig().isLCIntraComponent()) {
					killAll.value = true;
					return null;
				}
//						System.out.println();
			}
		}

		//现在只使用第一种，因为第二种还是有个Alias的问题解决不了
//		SourceSinkDefinition def = MyOwnUtils.getOriginalSource(source);
//		//下面的是第二种，且需要注意，staticfield只能用第一条来处理，因此需要进行检查
//		if (!ap.isStaticFieldRef() && !canTaintExitFromReturn(callSite, stmt, ap)) {
//			//此时taint无法传播至函数外
//			if (TaintPropagationResults.shouldBeKilledForReturnStmts(def, stmt)) {
//				//说明其他alias的taint可以传递出这个函数，那么这个就是假的leak，不需要加进result
//				killAll.value = true;
//				return null;
//			} else {
//				SinkInfo sink2Info = sourceSinkManager.getSPSinkInfo(stmt, getManager(), "exit");
//				if (!getResults().addResult(new AbstractionAtSink(sink2Info.getDefinition(), source, stmt))) {
//					killAll.value = true;
//					return null;
//				}
//			}
//
//		} else {
//			//此时说明，它的值还是传递到了函数外
//			TaintPropagationResults.addReturnStmts(def, stmt);
//		}

		return null;
	}

	public boolean canTaintExitFromReturn(Stmt callSite, Stmt currentReturnStmt, AccessPath ap) {
		if (callSite instanceof DefinitionStmt && currentReturnStmt instanceof ReturnStmt) {
			//这里是 a = b.method(x){return d taint} 或者 a = method(x)的情况；
			Value returnop = ((ReturnStmt)currentReturnStmt).getOp();
			if (getAliasing().mayAlias(returnop, ap.getPlainValue())) {
				return true;
			}
		}
		InvokeExpr expr = callSite.getInvokeExpr();
		if (expr instanceof InstanceInvokeExpr) {
			//这里处理a = b.method(x){b.m = taint; return;}或者 a = method(x){x.m = taint; return;}的情况；
			SootMethod m = getManager().getICFG().getMethodOf(currentReturnStmt);
			if (!m.isStatic()) {
				if(getAliasing().mayAlias(m.getActiveBody().getThisLocal(), ap.getPlainValue())) {
					return true;
				}
			}
			for (Local l : m.getActiveBody().getParameterLocals()) {
				if(getAliasing().mayAlias(l, ap.getPlainValue())) {
					return true;
				}
			}
		}
		//其他的a = b.method(x)    b.m, x.m taint的情况

		return false;
	}
}
