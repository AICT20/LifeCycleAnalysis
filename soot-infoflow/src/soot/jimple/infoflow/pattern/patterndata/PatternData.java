package soot.jimple.infoflow.pattern.patterndata;

import heros.solver.Pair;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.*;

//TODO 大改，新版本下，analyse的分析主体改为PatternData内的entrypoints，而不再是flowdroid自身构建的main函数
public abstract class PatternData implements PatternInterface{
    protected BiDiInterproceduralCFG<Unit, SootMethod> icfg = null;
    protected Map<SootClass, PatternEntryData> involvedEntrypoints = null;
    protected Map<SootClass, PatternEntryData> initialInvolvedEntrypoints = null;

    public PatternData() {
        this.initialInvolvedEntrypoints = new HashMap<>();
        this.involvedEntrypoints = new HashMap<>();
    }
    public Map<SootClass, PatternEntryData> getInvolvedEntrypoints() {
        return this.involvedEntrypoints;
    }
    @Override
    public Set<SootMethod> getEntryMethods() {
        Set<SootMethod> entrymethods = new HashSet<>();
        if (!involvedEntrypoints.isEmpty()) {
            for (PatternEntryData data : involvedEntrypoints.values()) {
                entrymethods.addAll(data.getAllMethods());
            }
        }
        return entrymethods;
    }
    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, IInfoflowCFG icfg) {
        this.involvedEntrypoints.clear();
        this.initialInvolvedEntrypoints.clear();
        if (null == icfg) {
            throw new RuntimeException("CFG was not constructed at Pattern Adaption!");
        }

        this.initialInvolvedEntrypoints = getInitialEntryClasses(allEntrypoints, icfg);
        this.involvedEntrypoints.putAll(this.initialInvolvedEntrypoints);
        for (SootClass initalClass : this.initialInvolvedEntrypoints.keySet()) {
            Hierarchy h = Scene.v().getActiveHierarchy();
//            if (initalClass.isInterface()) {
//                for (SootClass impleClass : h.getImplementersOf(initalClass)) {
//                    this.involvedEntrypoints.put(impleClass, methodName);
//                }
//                for (SootClass subImle : h.getSubinterfacesOf(initalClass)) {
//                    for (SootClass impleClass : h.getImplementersOf(subImle)) {
//                        this.involvedEntrypoints.put(impleClass, methodName);
//                    }
//                }
//            } else
            if (initalClass.isConcrete() || initalClass.isAbstract()) {
                for (SootClass subClass : h.getSubclassesOf(initalClass)) {
                    PatternEntryData cData = involvedEntrypoints.get(subClass);
                    if (null == cData) {
                        cData = new PatternEntryData(subClass);
                        involvedEntrypoints.put(subClass, cData);
                    }
                    updateEntryDataWithLCMethods(subClass, cData);

                    this.involvedEntrypoints.put(subClass, cData);
                }
            }
        }
//        System.out.println();

    }
    protected abstract Map<SootClass, PatternEntryData> getInitialEntryClasses(Set<SootClass> allEntrypoints, IInfoflowCFG icfg);
    protected abstract void updateEntryDataWithLCMethods(SootClass cClass, PatternEntryData cData);
    public void clear() {
        this.involvedEntrypoints.clear();
        this.initialInvolvedEntrypoints.clear();
    }
}
