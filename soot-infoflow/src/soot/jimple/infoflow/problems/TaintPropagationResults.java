package soot.jimple.infoflow.problems;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.solver.memory.IMemoryManager;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.util.MyOwnUtils;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * Class for storing the results of the forward taint propagation
 * 
 * @author Steven Arzt
 *
 */
public class TaintPropagationResults {

	/**
	 * Handler interface that is invoked when new taint propagation results are
	 * added to the result object
	 */
	public interface OnTaintPropagationResultAdded {

		/**
		 * Called when a new abstraction has reached a sink statement
		 * 
		 * @param abs
		 *            The abstraction at the sink
		 * @return True if the data flow analysis shall continue, otherwise
		 *         false
		 */
		public boolean onResultAvailable(AbstractionAtSink abs);

	}

	protected final InfoflowManager manager;
	protected MyConcurrentHashMap<AbstractionAtSink, Abstraction> results = new MyConcurrentHashMap<AbstractionAtSink, Abstraction>();

	protected final Set<OnTaintPropagationResultAdded> resultAddedHandlers = new HashSet<>();

	/**
	 * Creates a new instance of the TaintPropagationResults class
	 * 
	 * @param manager
	 *            A reference to the manager class used during taint propagation
	 */
	TaintPropagationResults(InfoflowManager manager) {
		this.manager = manager;
	}

	/**
	 * Adds a new result of the data flow analysis to the collection
	 * 
	 * @param resultAbs
	 *            The abstraction at the sink instruction
	 * @return True if the data flow analysis shall continue, otherwise false
	 */
	public boolean addResult(AbstractionAtSink resultAbs) {
		// Check whether we need to filter a result in a system package
		if (manager.getConfig().getIgnoreFlowsInSystemPackages() && SystemClassHandler.isClassInSystemPackage(
				manager.getICFG().getMethodOf(resultAbs.getSinkStmt()).getDeclaringClass().getName()))
			return true;

		// Construct the abstraction at the sink
		Abstraction abs = resultAbs.getAbstraction();
		abs = abs.deriveNewAbstraction(abs.getAccessPath(), resultAbs.getSinkStmt());
		abs.setCorrespondingCallSite(resultAbs.getSinkStmt());

		// Reduce the incoming abstraction
		IMemoryManager<Abstraction, Unit> memoryManager = manager.getForwardSolver().getMemoryManager();
		if (memoryManager != null) {
			abs = memoryManager.handleMemoryObject(abs);
			if (abs == null)
				return true;
		}

		// Record the result
		resultAbs = new AbstractionAtSink(resultAbs.getSinkDefinition(), abs, resultAbs.getSinkStmt());
		Abstraction newAbs = this.results.putIfAbsentElseGet(resultAbs, resultAbs.getAbstraction());
		if (newAbs != resultAbs.getAbstraction())
			newAbs.addNeighbor(resultAbs.getAbstraction());

		// Notify the handlers
		boolean continueAnalysis = true;
		for (OnTaintPropagationResultAdded handler : resultAddedHandlers)
			if (!handler.onResultAvailable(resultAbs))
				continueAnalysis = false;
		return continueAnalysis;
	}

	/**
	 * Checks whether this result object is empty
	 * 
	 * @return True if this result object is empty, i.e., there are no results
	 *         yet, otherwise false
	 * @return
	 */
	public boolean isEmpty() {
		return this.results.isEmpty();
	}

	/**
	 * Gets all results collected in this data object
	 * 
	 * @return All data flow results collected in this object
	 */
	public Set<AbstractionAtSink> getResults() {
		return this.results.keySet();
	}

	/**
	 * Adds a new handler that is invoked when a new data flow result is added
	 * to this data object
	 * 
	 * @param handler
	 *            The handler implementation to add
	 */
	public void addResultAvailableHandler(OnTaintPropagationResultAdded handler) {
		this.resultAddedHandlers.add(handler);
	}

	//lifecycle-add 增加对results的初步处理
	public void initialDataProcessing() {
		MyConcurrentHashMap<AbstractionAtSink, Abstraction> newresults = new MyConcurrentHashMap();
		//预处理下
		Map<Abstraction, SourceSinkDefinition> sourceDefMap = new HashMap<>();
		for (Map.Entry<AbstractionAtSink, Abstraction> entry : this.results.entrySet()) {
			SourceSinkDefinition sourceDef = MyOwnUtils.getOriginalSource(entry.getValue());
			sourceDefMap.put(entry.getValue(), sourceDef);
		}


		for (Map.Entry<AbstractionAtSink, Abstraction> entry : this.results.entrySet()) {
			AbstractionAtSink absink = entry.getKey();
			Abstraction abs = entry.getValue();
			SourceSinkDefinition sourceDef = sourceDefMap.get(abs);
			//先处理killStmts
			Set<Stmt> currentkillStmts = abs.getKillStmts();
			if (null != currentkillStmts && !currentkillStmts.isEmpty()) {
				Set<Stmt> defkillStmts = allkillStmts.get(sourceDef);
				if (null != defkillStmts && !defkillStmts.isEmpty()) {
					Set<Stmt> tempSet = new HashSet(currentkillStmts);
					tempSet.retainAll(defkillStmts);
					if (!tempSet.isEmpty()) {
						//如果有交集，说明当前的这个记录是错误的，应当删除
						continue;
					}
				}

			}
			//再处理returnstmt
			Set<Stmt> defreturnStmts = allreturnStmts.get(sourceDef);
			if (null != defreturnStmts && defreturnStmts.contains(absink.getSinkStmt())) {
				continue;
			}

			newresults.put(absink, abs);
		}
//		for (Map.Entry<AbstractionAtSink, Abstraction> entry : newresults.entrySet()) {
//			Abstraction abs = entry.getValue();
//			LinkedList<SootMethod> preMethods = new LinkedList();
//			LinkedList<Stmt> preStmts = new LinkedList();
//			Abstraction currentAbs = abs;
//			while (null != currentAbs) {
//				preStmts.add(currentAbs.getCurrentStmt());
//				preMethods.add(manager.getICFG().getMethodOf(currentAbs.getCurrentStmt()));
//				currentAbs = currentAbs.getPredecessor();
//			}
//
//			if (abs.getKillStmts() == null) {
//				System.out.println();
//			}
//		}

		this.results = newresults;
	}


	//lifecycle-add 我们加一点对于killstmt的全局内容 注意！！！这个allkillStmts仅针对当前的单个taint
	public static void initLCResults() {
		allkillStmts = new ConcurrentHashMap<>();
		allreturnStmts = new ConcurrentHashMap<>();
	}
	public static void clearLCResults() {
		allkillStmts.clear();
		allreturnStmts.clear();
	}
	protected static Map<SourceSinkDefinition, Set<Stmt>> allkillStmts = null;//保存所有的kill操作的位置
	public static boolean addKillStmts(SourceSinkDefinition def, Stmt stmt) {
		Set<Stmt> killstmts = allkillStmts.get(def);
		if (null == killstmts) {
			killstmts = new ConcurrentHashSet<>();
			allkillStmts.put(def, killstmts);
		}
		return killstmts.add(stmt);
	}
	public static boolean shouldBeKilledForKillStmts(SourceSinkDefinition def, Stmt stmt) {
		Set<Stmt> killstmts = allkillStmts.get(def);
		if (null == killstmts) {
			return false;
		}
		if (!killstmts.contains(stmt)) {
			return false;
		}
		return true;
	}

	protected static Map<SourceSinkDefinition, Set<Stmt>> allreturnStmts = null;//保存所有taint能正常传递的return语句
	public static boolean addReturnStmts(SourceSinkDefinition def, Stmt stmt) {
		Set<Stmt> returnstmts = allreturnStmts.get(def);
		if (null == returnstmts) {
			returnstmts = new ConcurrentHashSet<>();
			allreturnStmts.put(def, returnstmts);
		}
		return returnstmts.add(stmt);
	}

	public static boolean shouldBeKilledForReturnStmts(SourceSinkDefinition def, Stmt stmt) {
		Set<Stmt> returnstmts = allreturnStmts.get(def);
		if (null == returnstmts) {
			return false;
		}
		if (!returnstmts.contains(stmt)) {
			return false;
		}
		return true;
	}




}
