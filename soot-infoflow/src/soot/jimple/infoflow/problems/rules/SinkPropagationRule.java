package soot.jimple.infoflow.problems.rules;

import java.util.Collection;

import soot.SootMethod;
import soot.Value;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.problems.TaintPropagationResults;
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
		if (source.isAbstractionActive()) {
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
//						System.out.println();
			}
		}

		boolean isTaintReturned = true;
		if (retSite instanceof ReturnStmt) {
			Value returnop = ((ReturnStmt)retSite).getOp();
			isTaintReturned = getAliasing().mayAlias(returnop, ap.getPlainValue());
		} else if (retSite instanceof ReturnVoidStmt) {
			isTaintReturned = false;
		}
		SourceSinkDefinition def = MyOwnUtils.getOriginalSource(source);
		//下面的是第二种，且需要注意，staticfield只能用第一条来处理，因此需要进行检查
		if (!isTaintReturned && !isTaintVisibleInCallee(callSite, source) && !ap.isStaticFieldRef()) {
			//此时ap值无法传播至函数外
			if (TaintPropagationResults.shouldBeKilledForReturnStmts(def, stmt)) {
				//说明其他alias的taint可以传递出这个函数，那么这个就是假的leak，不需要加进result
				killAll.value = true;
				return null;
			} else {
				SinkInfo sink2Info = sourceSinkManager.getSPSinkInfo(stmt, getManager(), "exit");
				if (!getResults().addResult(new AbstractionAtSink(sink2Info.getDefinition(), source, stmt))) {
					killAll.value = true;
					return null;
				}
			}

		} else {
			//此时说明，它的值还是传递到了函数外
			TaintPropagationResults.addReturnStmts(def, stmt);
		}
		//如果两个

		return null;
	}

}
