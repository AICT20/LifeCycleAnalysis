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
package soot.jimple.infoflow.data;

import java.util.*;

import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;
import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.collect.AtomicBitSet;
import soot.jimple.infoflow.pattern.patterndata.PatternData;
import soot.jimple.infoflow.pattern.patterndata.PatternDataConstant;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG.UnitContainer;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.util.MyComparativeFlyWightSet;
import soot.jimple.infoflow.util.MyOwnUtils;

/**
 * The abstraction class contains all information that is necessary to track the
 * taint.
 * 
 * @author Steven Arzt
 * @author Christian Fritz
 */
public class Abstraction implements Cloneable, FastSolverLinkedNode<Abstraction, Unit> {

	protected static boolean flowSensitiveAliasing = true;

	/**
	 * the access path contains the currently tainted variable or field
	 */
	//lifecycle-add 这里修改，改成一个accesspath的集合
	protected AccessPath accessPath;

	protected Abstraction predecessor = null;
	protected Set<Abstraction> neighbors = null; //neighbor是只有currentStmt不同的Abs，它们就相当于同一block里的不同语句
	protected Stmt currentStmt = null;
	protected Stmt correspondingCallSite = null;

	protected SourceContext sourceContext = null;
	//lifecycle-add
	protected Set<Stmt> killStmts = null;
	protected Set<Pair<IfStmt, Boolean>> ifkillStmts = null;
	protected boolean isfinishing = false;
	//新一轮调整
	protected SourceSinkDefinition tempdef = null;//这里的tempdef不进行hash以及equals的比较
	protected Set<String> tagnames = null;//用来记录当前的source的来源entrypoints
	public SourceSinkDefinition getOriginalDef() {
		return tempdef;
	}
	public void setOriginalDef(SourceSinkDefinition def) {
		tempdef = def;
	}

	/**
	 * Unit/Stmt which activates the taint when the abstraction passes it
	 */
	protected Unit activationUnit = null;//这个还是要保留在Abstraction里


	/**
	 * taint is thrown by an exception (is set to false when it reaches the
	 * catch-Stmt)
	 */
	protected boolean exceptionThrown = false;
	protected int hashCode = 0;
	protected int neighborHashCode = 0;

	/**
	 * The postdominators we need to pass in order to leave the current conditional
	 * branch. Do not use the synchronized Stack class here to avoid deadlocks.
	 */
	protected List<UnitContainer> postdominators = null;
	protected boolean isImplicit = false;

	/**
	 * Only valid for inactive abstractions. Specifies whether an access paths has
	 * been cut during alias analysis.
	 */
//	protected boolean dependsOnCutAP = false;  这个先删了，影响不大

	protected AtomicBitSet pathFlags = null;
	protected int propagationPathLength = 0;

	public static class NeighborHashingStrategy implements HashingStrategy<Abstraction> {

		private static final long serialVersionUID = 4836518478381414909L;
		private static final NeighborHashingStrategy INSTANCE = new NeighborHashingStrategy();

		@Override
		public int computeHashCode(Abstraction abs) {
			if (abs.neighborHashCode != 0)
				return abs.neighborHashCode;

			final int prime = 31;
			int result = 1;

			result = prime * result + abs.hashCode();
			result = prime * result + ((abs.predecessor == null) ? 0 : abs.predecessor.hashCode());

			abs.neighborHashCode = result;
			return result;
		}

		@Override
		public boolean equals(Abstraction abs1, Abstraction abs2) {
			if (abs1 == abs2)
				return true;
			if (abs1 == null || abs2 == null || abs1.getClass() != abs2.getClass())
				return false;

			// If we have already computed hash codes, we can use them for
			// comparison
			int hashCode1 = abs1.neighborHashCode;
			int hashCode2 = abs2.neighborHashCode;
			if (hashCode1 != 0 && hashCode2 != 0 && hashCode1 != hashCode2)
				return false;

			if (abs1.accessPath == null ) {
				if (abs2.accessPath != null)
					return false;
			} else if (!(abs1.accessPath.equals(abs2.accessPath)))
				return false;
			if (abs1.predecessor == null) {
				if (abs2.predecessor != null)
					return false;
			} else if (!abs1.predecessor.equals(abs2.predecessor))
				return false;
			if (abs1.currentStmt == null) {
				if (abs2.currentStmt != null)
					return false;
			} else if (!abs1.currentStmt.equals(abs2.currentStmt))
				return false;
			if (abs1.killStmts != abs2.killStmts) {
				return false;
			}
			if (abs1.ifkillStmts != abs2.ifkillStmts) {
				return false;
			}

			return abs1.localEquals(abs2);
		}

	}

	public Abstraction(SourceSinkDefinition definition, AccessPath sourceVal, Stmt sourceStmt, Object userData,
			boolean exceptionThrown, boolean isImplicit) {
		this(sourceVal, new SourceContext(definition, sourceVal, sourceStmt, userData), exceptionThrown, isImplicit);
	}

	protected Abstraction(AccessPath apToTaint, SourceContext sourceContext, boolean exceptionThrown,
			boolean isImplicit) {
		this.sourceContext = sourceContext;
		this.accessPath = apToTaint;
		this.activationUnit = null;
		this.exceptionThrown = exceptionThrown;
		this.killStmts = null;
		this.ifkillStmts = null;
		this.neighbors = null;
		this.isImplicit = isImplicit;
		this.currentStmt = sourceContext == null ? null : sourceContext.getStmt();
		if (null != sourceContext) {
			this.tempdef = sourceContext.definition;
		}
	}

	/**
	 * Creates an abstraction as a copy of an existing abstraction, only exchanging
	 * the access path. -> only used by AbstractionWithPath
	 * 
	 * @param p        The access path for the new abstraction
	 * @param original The original abstraction to copy
	 */
	protected Abstraction(AccessPath p, Abstraction original) {
		if (original == null) {
			sourceContext = null;
			exceptionThrown = false;
			activationUnit = null;
			isImplicit = false;
		} else {
			sourceContext = original.sourceContext;
			exceptionThrown = original.exceptionThrown;
			activationUnit = original.activationUnit;
			isfinishing = original.isfinishing;
			killStmts = original.killStmts;
			ifkillStmts = original.ifkillStmts;
			assert activationUnit == null || flowSensitiveAliasing;

			postdominators = original.postdominators == null ? null
					: new ArrayList<UnitContainer>(original.postdominators);

//			dependsOnCutAP = original.dependsOnCutAP;
			tempdef = original.tempdef;
			isImplicit = original.isImplicit;
			tagnames = original.tagnames;
		}
		accessPath = p;
		neighbors = null;
		currentStmt = null;
	}

	/**
	 * Initializes the configuration for building new abstractions
	 * 
	 * @param config The configuration of the data flow solver
	 */
	public static void initialize(InfoflowConfiguration config) {
		flowSensitiveAliasing = config.getFlowSensitiveAliasing();
	}

	//这个可以复用
	public Abstraction deriveInactiveAbstraction(Stmt activationUnit) {
		if (!flowSensitiveAliasing) {
			assert this.isAbstractionActive();
			return this;
		}

		// If this abstraction is already inactive, we keep it
		if (!this.isAbstractionActive())
			return this;

		Abstraction a = deriveNewAbstractionMutable(accessPath, null);
		if (a == null)
			return null;

		a.postdominators = null;
		a.activationUnit = activationUnit;
//		a.dependsOnCutAP |= a.getAccessPath().isCutOffApproximation();
		return a;
	}

	//lifecycle-add
	public Abstraction deriveNewAbstractionOnKill(Stmt killStmt) {
		if (null != this.killStmts && this.killStmts.contains(killStmt)) {
			return this;
		}
		Abstraction abs = clone();
		abs.currentStmt = killStmt;
		abs.sourceContext = null;
		if (null == abs.killStmts) {
			abs.killStmts = MyComparativeFlyWightSet.getKillSetGenerator().getSet(tempdef, Collections.EMPTY_SET, killStmt);
		} else {
			abs.killStmts = MyComparativeFlyWightSet.getKillSetGenerator().getSet(tempdef, abs.killStmts, killStmt);
		}
		return abs;
	}


	public Abstraction deriveNewAbstractionOnIfKill(IfStmt killStmt, boolean isjump) {
		if (null != this.ifkillStmts && this.ifkillStmts.contains(new Pair<>(killStmt, isjump))) {
			return this;
		}
		Abstraction abs = clone();
		abs.currentStmt = killStmt;
		abs.sourceContext = null;
		if (null == abs.ifkillStmts) {
			abs.ifkillStmts = MyComparativeFlyWightSet.getIfKillSetGenerator().getSet(tempdef, Collections.EMPTY_SET, new Pair<>(killStmt, isjump));
		} else {
			abs.ifkillStmts = MyComparativeFlyWightSet.getIfKillSetGenerator().getSet(tempdef, abs.ifkillStmts, new Pair<>(killStmt, isjump));
		}
		return abs;
	}

	public Abstraction deriveNewFinishingAbstraction(Stmt currentStmt) {
		if (this.isfinishing) {
			return this;
		}
		Abstraction abs = clone();
		abs.currentStmt = currentStmt;
		abs.sourceContext = null;
		abs.isfinishing = true;
		return abs;
	}

	public Abstraction deriveExitFinishingAbstraction(Stmt currentStmt) {
		if (!this.isfinishing) {
			return this;
		}
		Abstraction abs = clone();
		abs.currentStmt = currentStmt;
		abs.sourceContext = null;
		abs.isfinishing = false;
		return abs;
	}

	//lifecycle-add
	public Abstraction deriveNewAbstractionOnCallAndReturn(Stmt callorreturnStmt) {
		Abstraction abs = clone();
		abs.currentStmt = callorreturnStmt;
		abs.sourceContext = null;
		return abs;
	}

	//lifecycle-add
	public Abstraction deriveNewAbstractionWithNewTagSets(Set<String> newtagnames) {
		if (newtagnames.isEmpty()) {
			//一般不会出现空的情况
			return this;
		}
		for (String tag : newtagnames) {//如果是不相关的component的生命周期函数，直接去掉
			if (tag.equals(PatternDataConstant.LCMETHODNOTINSUFFIX)) {
				return null;
			}
		}

		if (this.tagnames == null || this.tagnames.isEmpty()) {
			Abstraction abs = clone();
			abs.sourceContext = null;
			abs.currentStmt = this.currentStmt;
			abs.tagnames = newtagnames;
			return abs;
		}
		boolean isAllContained = true;//指包含当前method的所有tag
		boolean isContainingAll = true;//指当前method的tag包含当前Abstraction的所有tag
		for (String cn : this.tagnames) {
			if (!newtagnames.contains(cn)) {
				isAllContained = false;
				break;
			}
		}
		for (String cn : newtagnames) {
			if (!this.tagnames.contains(cn)) {
				isContainingAll = false;
				break;
			}
		}
		if (!isAllContained && !isContainingAll) {
			return null;
		} else if (isAllContained) {
			return this;
		} else {
			Abstraction abs = clone();
			abs.sourceContext = null;
			abs.currentStmt = this.currentStmt;
			abs.tagnames = newtagnames;
			return abs;
		}
	}

	public Abstraction deriveNewAbstraction(AccessPath p, Stmt currentStmt) {
		return deriveNewAbstraction(p, currentStmt, isImplicit);
	}

	public Abstraction deriveNewAbstraction(AccessPath p, Stmt currentStmt, boolean isImplicit) {
		// If the new abstraction looks exactly like the current one, there is
		// no need to create a new object
		if (this.accessPath.equals(p) && this.currentStmt == currentStmt && this.isImplicit == isImplicit)
			return this;

		Abstraction abs = deriveNewAbstractionMutable(p, currentStmt);
		if (abs == null)
			return null;

		abs.isImplicit = isImplicit;
		return abs;
	}

	protected Abstraction deriveNewAbstractionMutable(AccessPath p, Stmt currentStmt) {
		// An abstraction needs an access path
		if (p == null)
			return null;

		if (this.accessPath.equals(p) && this.currentStmt == currentStmt) {
			Abstraction abs = clone();
			abs.currentStmt = currentStmt;
			return abs;
		}

		Abstraction abs = new Abstraction(p, this);
		abs.predecessor = this;
		abs.currentStmt = currentStmt;
		abs.propagationPathLength = propagationPathLength + 1;

		if (abs.accessPath == null)
			abs.postdominators = null;
//		if (!abs.isAbstractionActive())
//			abs.dependsOnCutAP = abs.dependsOnCutAP || p.isCutOffApproximation();

		abs.sourceContext = null;
		return abs;
	}

	/**
	 * Derives a new abstraction that models the current local being thrown as an
	 * exception
	 * 
	 * @param throwStmt The statement at which the exception was thrown
	 * @return The newly derived abstraction
	 */
	public Abstraction deriveNewAbstractionOnThrow(Stmt throwStmt) {
		Abstraction abs = clone();

		abs.currentStmt = throwStmt;
		abs.sourceContext = null;
		abs.exceptionThrown = true;
		return abs;
	}

	/**
	 * Derives a new abstraction that models the current local being caught as an
	 * exception
	 * 
	 * @param ap The access path in which the tainted exception is stored
	 * @return The newly derived abstraction
	 */
	public Abstraction deriveNewAbstractionOnCatch(AccessPath ap) {
		assert this.exceptionThrown;
		Abstraction abs = deriveNewAbstractionMutable(ap, null);
		if (abs == null)
			return null;

		abs.exceptionThrown = false;
		return abs;
	}

	public boolean isAbstractionActive() {
		return activationUnit == null;
	}

	public boolean isImplicit() {
		return isImplicit;
	}

	@Override
	public String toString() {
		String apstr = "";
		if (null != accessPath) {
			apstr += accessPath.toString();
		}
		return (isAbstractionActive() ? "" : "_") + apstr
				+ (activationUnit == null ? "" : activationUnit.toString()) + ">>";
	}

	public AccessPath getAccessPath() {
		return accessPath;
	}

	public Unit getActivationUnit() {
		return this.activationUnit;
	}

	public Set<Stmt> getKillStmts() {
		return this.killStmts;
	}

	public Set<Pair<IfStmt, Boolean>> getIfKillStmts() {
		return this.ifkillStmts;
	}

	public Abstraction getActiveCopy() {
		if (this.isAbstractionActive())
			return this;


		Abstraction a = clone();
		a.sourceContext = null;
		a.activationUnit = null;
		return a;
	}

	/**
	 * Gets whether this value has been thrown as an exception
	 * 
	 * @return True if this value has been thrown as an exception, otherwise false
	 */
	public boolean getExceptionThrown() {
		return this.exceptionThrown;
	}

	public Abstraction deriveConditionalAbstractionEnter(UnitContainer postdom, Stmt conditionalUnit) {
		assert this.isAbstractionActive();

		if (postdominators != null && postdominators.contains(postdom))
			return this;

		Abstraction abs = deriveNewAbstractionMutable(AccessPath.getEmptyAccessPath(), conditionalUnit);
		if (abs == null)
			return null;

		if (abs.postdominators == null)
			abs.postdominators = Collections.singletonList(postdom);
		else
			abs.postdominators.add(0, postdom);
		return abs;
	}

	public Abstraction deriveConditionalAbstractionCall(Unit conditionalCallSite) {
		assert this.isAbstractionActive();
		assert conditionalCallSite != null;

		Abstraction abs = deriveNewAbstractionMutable(AccessPath.getEmptyAccessPath(), (Stmt) conditionalCallSite);
		if (abs == null)
			return null;

		// Postdominators are only kept intraprocedurally in order to not
		// mess up the summary functions with caller-side information
		abs.postdominators = null;

		return abs;
	}

	public Abstraction dropTopPostdominator() {
		if (postdominators == null || postdominators.isEmpty())
			return this;

		Abstraction abs = clone();
		abs.sourceContext = null;
		abs.postdominators.remove(0);
		return abs;
	}

	public UnitContainer getTopPostdominator() {
		if (postdominators == null || postdominators.isEmpty())
			return null;
		return this.postdominators.get(0);
	}

	public boolean isTopPostdominator(Unit u) {
		UnitContainer uc = getTopPostdominator();
		if (uc == null)
			return false;
		return uc.getUnit() == u;
	}

	public boolean isTopPostdominator(SootMethod sm) {
		UnitContainer uc = getTopPostdominator();
		if (uc == null)
			return false;
		return uc.getMethod() == sm;
	}

	@Override
	public Abstraction clone() {
		Abstraction abs = new Abstraction(this.accessPath, this);
		abs.predecessor = this;
		abs.neighbors = null;
		abs.currentStmt = null;
		abs.correspondingCallSite = null;
		abs.propagationPathLength = propagationPathLength + 1;

		assert abs.equals(this);
		return abs;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Abstraction other = (Abstraction) obj;

		// If we have already computed hash codes, we can use them for
		// comparison
		if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
			return false;

		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!(accessPath.equals(other.accessPath)))
			return false;
		if (killStmts != other.killStmts) {
			return false;
		}
		if (ifkillStmts != other.ifkillStmts) {
			return false;
		}
		return localEquals(other);
	}

	/**
	 * Checks whether this object locally equals the given object, i.e. the both are
	 * equal modulo the access path
	 * 
	 * @param other The object to compare this object with
	 * @return True if this object is locally equal to the given one, otherwise
	 *         false
	 */
	private boolean localEquals(Abstraction other) {
		// deliberately ignore prevAbs
		if (sourceContext == null) {
			if (other.sourceContext != null)
				return false;
		} else if (!sourceContext.equals(other.sourceContext))
			return false;
		if (activationUnit == null) {
			if (other.activationUnit != null)
				return false;
		} else if (!activationUnit.equals(other.activationUnit))
			return false;
		if (this.exceptionThrown != other.exceptionThrown)
			return false;
		if (postdominators == null) {
			if (other.postdominators != null)
				return false;
		} else if (!postdominators.equals(other.postdominators))
			return false;
//		if (this.dependsOnCutAP != other.dependsOnCutAP)
//			return false;
		if (this.isImplicit != other.isImplicit)
			return false;
		return this.isfinishing == other.isfinishing;
	}

	@Override
	public int hashCode() {
		if (this.hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = 1;

		// deliberately ignore prevAbs
		result = prime * result + ((sourceContext == null) ? 0 : sourceContext.hashCode());
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((activationUnit == null) ? 0 : activationUnit.hashCode());
		result = prime * result + (exceptionThrown ? 1231 : 1237);
		result = prime * result + ((postdominators == null) ? 0 : postdominators.hashCode());
//		result = prime * result + (dependsOnCutAP ? 1231 : 1237);
		result = prime * result + (isImplicit ? 1231 : 1237);
		result = prime * result + (isfinishing ? 1231 : 1237);
		//再额外添加一个killStmts的
		if (null != killStmts && !killStmts.isEmpty()) {
			int killhash = 0;
			for (Stmt s : killStmts) {
				killhash += s.hashCode();
			}
			result = prime * result + killhash;
		}
		if (null != ifkillStmts && !ifkillStmts.isEmpty()) {
			int killhash = 0;
			for (Pair<IfStmt, Boolean> s : ifkillStmts) {
				killhash += s.hashCode();
			}
			result = prime * result + killhash;
		}

		this.hashCode = result;

		return this.hashCode;
	}

//	/**
//	 * Checks whether this abstraction entails the given abstraction, i.e. this
//	 * taint also taints everything that is tainted by the given taint.
//	 *
//	 * @param other The other taint abstraction
//	 * @return True if this object at least taints everything that is also tainted
//	 *         by the given object
//	 */
	public boolean entails(Abstraction other) {
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.entails(other.accessPath))
			return false;
		return localEquals(other);
	}

	/**
	 * Gets the context of the taint, i.e. the statement and value of the source
	 * 
	 * @return The statement and value of the source
	 */
	public SourceContext getSourceContext() {
		return sourceContext;
	}

//	public boolean dependsOnCutAP() {
//		return dependsOnCutAP;
//	}

	@Override
	public Abstraction getPredecessor() {
		return this.predecessor;
	}

	public Set<Abstraction> getNeighbors() {
		return this.neighbors;
	}

	public Stmt getCurrentStmt() {
		return this.currentStmt;
	}

	@Override
	public boolean addNeighbor(Abstraction originalAbstraction) {
		// We should not register ourselves as a neighbor
		if (originalAbstraction == this)
			return false;

		//lifecycle-test
//		if (null != this.getCorrespondingCallSite() && this.getCorrespondingCallSite().toString().contains("onReceive")) {
//			if (null != originalAbstraction.getCorrespondingCallSite() && originalAbstraction.getCorrespondingCallSite().toString().contains("UnregisterReceiver: void <init>")) {
//				System.out.println();
//			}
//		}

		// We should not add identical nodes as neighbors
		if (this.predecessor == originalAbstraction.predecessor && this.currentStmt == originalAbstraction.currentStmt)
			return false;

		synchronized (this) {
			if (neighbors == null)
				neighbors = new TCustomHashSet<Abstraction>(NeighborHashingStrategy.INSTANCE);
			else if (InfoflowConfiguration.getMergeNeighbors()) {
				// Check if we already have an identical neighbor
				for (Abstraction nb : neighbors) {
					if (nb == originalAbstraction)
						return false;
					if (originalAbstraction.predecessor == nb.predecessor
							&& originalAbstraction.currentStmt == nb.currentStmt
							&& originalAbstraction.correspondingCallSite == nb.correspondingCallSite) {
						return false;
					}
				}
			}
			return this.neighbors.add(originalAbstraction);
		}
	}

	public void setCorrespondingCallSite(Stmt callSite) {
		this.correspondingCallSite = callSite;
	}

	public Stmt getCorrespondingCallSite() {
		return this.correspondingCallSite;
	}

	public static Abstraction getZeroAbstraction(boolean flowSensitiveAliasing) {
		Abstraction zeroValue = new Abstraction(AccessPath.getZeroAccessPath(), null, false, false);
		Abstraction.flowSensitiveAliasing = flowSensitiveAliasing;
		return zeroValue;
	}

	@Override
	public void setPredecessor(Abstraction predecessor) {
		this.predecessor = predecessor;
		assert this.predecessor != this;

		this.neighborHashCode = 0;
	}

	/**
	 * Only use this method if you really need to fake a source context and know
	 * what you are doing.
	 * 
	 * @param sourceContext The new source context
	 */
	public void setSourceContext(SourceContext sourceContext) {
		this.sourceContext = sourceContext;
		this.hashCode = 0;
		this.neighborHashCode = 0;
	}

	/**
	 * Registers that a worker thread with the given ID has already processed this
	 * abstraction
	 * 
	 * @param id The ID of the worker thread
	 * @return True if the worker thread with the given ID has not been registered
	 *         before, otherwise false
	 */
	public boolean registerPathFlag(int id, int maxSize) {
		if (pathFlags == null || pathFlags.size() < maxSize) {
			synchronized (this) {
				if (pathFlags == null) {
					// Make sure that the field is set only after the
					// constructor
					// is done and the object is fully usable
					AtomicBitSet pf = new AtomicBitSet(maxSize);
					pathFlags = pf;
				} else if (pathFlags.size() < maxSize) {
					AtomicBitSet pf = new AtomicBitSet(maxSize);
					for (int i = 0; i < pathFlags.size(); i++) {
						if (pathFlags.get(i))
							pf.set(i);
					}
					pathFlags = pf;
				}
			}
		}
		return pathFlags.set(id);
	}

	public Abstraction injectSourceContext(SourceContext sourceContext) {
		if (this.sourceContext != null && this.sourceContext.equals(sourceContext))
			return this;

		Abstraction abs = clone();
		abs.predecessor = null;
		abs.neighbors = null;
		abs.sourceContext = sourceContext;
		abs.currentStmt = this.currentStmt;
		return abs;
	}

	void setCurrentStmt(Stmt currentStmt) {
		this.currentStmt = currentStmt;
	}

	@Override
	public int getNeighborCount() {
		return neighbors == null ? 0 : neighbors.size();
	}

	@Override
	public int getPathLength() {
		return propagationPathLength;
	}

	public boolean isIsfinishing() {
		return this.isfinishing;
	}

}
