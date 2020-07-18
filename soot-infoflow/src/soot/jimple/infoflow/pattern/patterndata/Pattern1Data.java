package soot.jimple.infoflow.pattern.patterndata;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.*;

//pattern1 是在onCreate方法中调用finish()函数时将会跳过onStart、onResume、onPause和onStop方法的执行，直接跳到onDestroy
//TODO 在onStart方法中执行finish()时也会跳过onResume和onPause
public class Pattern1Data extends PatternData {
    private Map<SootClass, String> initialInvolvedEntrypoints = null;
    public Pattern1Data(){
        super();
        initialInvolvedEntrypoints = new HashMap<>();
    }

    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
//        seedMethods = searchForSeedMethods(icfg, "void onDestroy()");
    }

    public Map<SootClass, String> getInvolvedEntrypoints() {
        return this.involvedEntrypoints;
    }

    public Map<SootClass, String> getInitialInvolvedEntrypoints(){
        return this.initialInvolvedEntrypoints;
    }

    //TODO  这里错了！！！ 在同一个Activity， finish()应该既可以在onCreate也可以在onStart中执行
    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, IInfoflowCFG icfg) {
        this.involvedEntrypoints.clear();
        this.initialInvolvedEntrypoints.clear();
        if (null == icfg) {
            this.involvedEntrypoints = new HashMap<>();
            for (SootClass nowclass : allEntrypoints) {
                this.involvedEntrypoints.put(nowclass, PatternDataConstant.ONCREATESUBSIG);
            }
            return;
        }

        this.initialInvolvedEntrypoints = getInitialEntryClasses(icfg);
        this.involvedEntrypoints.putAll(this.initialInvolvedEntrypoints);
        for (SootClass initalClass : this.initialInvolvedEntrypoints.keySet()) {
            String methodName = this.initialInvolvedEntrypoints.get(initalClass);
            Hierarchy h = Scene.v().getActiveHierarchy();
            if (initalClass.isInterface()) {
                for (SootClass impleClass : h.getImplementersOf(initalClass)) {
                    this.involvedEntrypoints.put(impleClass, methodName);
                }
                for (SootClass subImle : h.getSubinterfacesOf(initalClass)) {
                    for (SootClass impleClass : h.getImplementersOf(subImle)) {
                        this.involvedEntrypoints.put(impleClass, methodName);
                    }
                }
            } else if (initalClass.isConcrete() || initalClass.isAbstract()) {
                for (SootClass subClass : h.getSubclassesOf(initalClass)) {
                    this.involvedEntrypoints.put(subClass, methodName);
                }
            }
        }
//        System.out.println();

    }

    private Map<SootClass, String> getInitialEntryClasses( IInfoflowCFG icfg) {
        SootMethod finishM = Scene.v().getMethod(PatternDataConstant.FINISHMETHODSIG);
        if (null == finishM) {
            return Collections.EMPTY_MAP;
        }
        Set<SootMethod> callFinishMethod = new HashSet<>();
        Collection<Unit> finishInvocations = icfg.getCallersOf(finishM);
        for (Unit u : finishInvocations) {
            callFinishMethod.add(icfg.getMethodOf(u));
        }

        Stack<SootMethod> currentCallingMs = new Stack<>();
        currentCallingMs.addAll(callFinishMethod);
        while (!currentCallingMs.isEmpty()) {
            SootMethod cM = currentCallingMs.pop();
            Collection<Unit> invocations = icfg.getCallersOf(cM);
            for (Unit u : invocations) {
                SootMethod m  = icfg.getMethodOf(u);
                if (!callFinishMethod.contains(m)) {
                    callFinishMethod.add(m);
                    currentCallingMs.add(m);
                }
            }
        }
        Map<SootClass, String> initalEntrypoints = new HashMap<>();
        for (SootMethod m : callFinishMethod) {
            if (m.getSubSignature().equals(PatternDataConstant.ONCREATESUBSIG) || m.getSubSignature().equals(PatternDataConstant.ONSTARTSUBSIG)) {
                initalEntrypoints.put(m.getDeclaringClass(), m.getSubSignature());
            }
        }
        return  initalEntrypoints;
    }

    @Override
    public Set<SootClass> getEntrypoints() {
        return involvedEntrypoints.keySet();
    }

    public void clear() {
        this.involvedEntrypoints.clear();
        this.initialInvolvedEntrypoints.clear();
    }

//    @Override
//    public void updateDummyMainMethod(SootMethod dummyMainMethod) {
//        Body b = dummyMainMethod.getActiveBody();
//        for (Unit u : b.getUnits()) {
//            if (u instanceof Stmt && ((Stmt) u).containsInvokeExpr()) {
//                InvokeExpr exp = ((Stmt) u).getInvokeExpr();
//                SootMethod currentMainM = exp.getMethod();
//                String content = involvedEntrypoints.get(currentMainM.getDeclaringClass());
//                if (null == content) {
//                    continue;
//                }
//
//                if (content.equals(PatternDataConstant.ONCREATESUBSIG)) {
//
//                }
//            }
//        }
//    }
}
