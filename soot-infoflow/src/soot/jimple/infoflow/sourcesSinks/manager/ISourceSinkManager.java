/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.sourcesSinks.manager;

import soot.RefType;
import soot.SootClass;
import soot.Type;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;

/**
 * the SourceSinkManager can tell if a statement contains a source or a sink
 */
public interface ISourceSinkManager {

	/**
	 * Initialization method that is called after the Soot instance has been
	 * created and before the actual data flow tracking is started.
	 */
    void initialize(boolean isIntraComponent);

	/**
	 * Determines if a method called by the Stmt is a source method or not. If
	 * so, additional information is returned
	 * 
	 * @param sCallSite
	 *            a Stmt which should include an invokeExrp calling a method
	 * @param manager
	 *            The manager object for interacting with the solver
	 * @return A SourceInfo object containing additional information if this
	 *         call is a source, otherwise null
	 */
    SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager);

	/**
	 * Checks if the given access path at this statement will leak.
	 * 
	 * @param sCallSite
	 *            The call site to check
	 * @param manager
	 *            The manager object for interacting with the solver
	 * @param ap
	 *            The access path to check. Pass null to check whether the given
	 *            statement can be a sink for any given access path.
	 * @return A SinkInfo object containing additional information if this call
	 *         is a sink, otherwise null
	 */
    SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap);


	void updateSinkInfoWithICFG(IInfoflowCFG icfg, boolean isIntraComponent);
	SinkInfo getSPSinkInfo(Stmt returnSite, InfoflowManager manager, String tag);

	//lifecycle-add 我们自行添加，用于判断当前的kill和source是否对应
    boolean isKillStmt(String killMethodSig);//这条勇来加速的
	boolean shouldKillCurrentSource(String killMethodSig, SourceSinkDefinition sourceDef);
	boolean canBeLeakObjects(Type currentType, SourceSinkDefinition def);


}
