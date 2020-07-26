package soot.jimple.infoflow.pattern.solver;

import heros.FlowFunctions;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

import java.util.*;

public abstract class PatternInfoflowProblem {
    protected final PatternInfoflowManager manager;
    protected NormalState zeroValue = null;
    protected IInfoflowCFG icfg = null;
    protected final Map<Unit, Set<NormalState>> initialSeeds = new HashMap<>();


    public PatternInfoflowProblem(PatternInfoflowManager manager) {
        this.manager = manager;
        this.icfg = manager.getICFG();
    }

    public NormalState zeroValue(){
        if (null == zeroValue) {
            zeroValue = createZeroValue();
        }
        return zeroValue;
    }

    public NormalState createZeroValue() {
        if (zeroValue == null)
            zeroValue = NormalState.getInitialState();
        return zeroValue;
    }
    public IInfoflowCFG interproceduralCFG() {
        return this.icfg;
    }
    public int numThreads() {
        return Runtime.getRuntime().availableProcessors();
    }

    public void addInitialSeeds(Unit unit, Set<NormalState> initialStates) {
        if (!this.initialSeeds.containsKey(unit))
            this.initialSeeds.put(unit, initialStates);
    }

    public Map<Unit, Set<NormalState>> initialSeeds() {
        return this.initialSeeds;
    }

    abstract public FlowFunctions<Unit, NormalState, SootMethod> flowFunctions();

}
