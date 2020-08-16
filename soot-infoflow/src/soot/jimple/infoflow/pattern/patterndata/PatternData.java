package soot.jimple.infoflow.pattern.patterndata;

import heros.solver.Pair;
import soot.*;
import soot.jimple.FieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.Chain;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

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
                    }
                    updateEntryDataWithLCMethods(subClass, cData);
                    boolean hasInvolvedFields =  updateInvolvedFieldsInEntryDatas(icfg, subClass, cData);
                    if (!hasInvolvedFields) {
                        this.involvedEntrypoints.remove(subClass);
                    } else {
                        this.involvedEntrypoints.put(subClass, cData);
                    }
                }
            }
        }
//        System.out.println();

    }
    protected abstract Map<SootClass, PatternEntryData> getInitialEntryClasses(Set<SootClass> allEntrypoints, IInfoflowCFG icfg);
    protected abstract void updateEntryDataWithLCMethods(SootClass cClass, PatternEntryData cData);
    protected boolean updateInvolvedFieldsInEntryDatas(IInfoflowCFG icfg, SootClass entryClass, PatternEntryData entryData) {
        Chain<SootField> fields =  entryClass.getFields();
        Set<SootField> involvedFields = new HashSet<>();
        for (SootField f : fields) {
            involvedFields.add(f);
        }
        for (SootMethod m : entryData.getAllMethods()) {
            involvedFields = getUsedFieldInsideMethod(icfg, m, involvedFields);
            if (involvedFields.isEmpty()){break;}
        }
        if (involvedFields.isEmpty()) {
            //如果为空那就直接删掉了
            return false;
        } else {
            entryData.updateInvolvedFields(involvedFields);
            return true;
        }
    }

    protected Set<SootField> getUsedFieldInsideMethod(IInfoflowCFG icfg, SootMethod m, Set<SootField> componentFields) {
        if (componentFields.isEmpty()){return componentFields;}

        Stack<SootMethod> currentCalledMethods = new Stack<>();
        Set<SootMethod> allCalledMethods = new HashSet<>();
        Set<SootField> allUsedFields = new HashSet<>();
        currentCalledMethods.add(m);
        allCalledMethods.add(m);
        while (!currentCalledMethods.isEmpty()) {
            SootMethod currentM = currentCalledMethods.pop();
            if (currentM.hasActiveBody()) {
                for (Unit s : currentM.getActiveBody().getUnits()) {
                    for (ValueBox box : s.getUseAndDefBoxes()) {
                        Value v = box.getValue();
                        if (v instanceof FieldRef){
                            SootField f = ((FieldRef) v).getField();
                            if (componentFields.contains(f)) {
                                allUsedFields.add(f);
                            }
                        } else if (s instanceof Stmt && ((Stmt)s).containsInvokeExpr()) {
                            Collection<SootMethod> invokedMethods = icfg.getCalleesOfCallAt(s);
                            for (SootMethod innerm : invokedMethods) {
                                if (!allCalledMethods.contains(innerm)) {
                                    allCalledMethods.add(innerm);
                                    currentCalledMethods.add(innerm);
                                }
                            }
                        }
                    }
                }
            }
        }
        return allUsedFields;
    }
    public void clear() {
        this.involvedEntrypoints.clear();
        this.initialInvolvedEntrypoints.clear();
    }
    @Override
    public MultiMap<SootClass, SootField> getEntryFields() {
        MultiMap<SootClass, SootField> entryfields = new HashMultiMap<>();
        for (SootClass entryClass : this.involvedEntrypoints.keySet()) {
            entryfields.putAll(entryClass, this.involvedEntrypoints.get(entryClass).getInvolvedFields());
        }
        return entryfields;
    }

    public Set<SootMethod> getCannotSkipMethods(){
        return Collections.EMPTY_SET;
    }
}
