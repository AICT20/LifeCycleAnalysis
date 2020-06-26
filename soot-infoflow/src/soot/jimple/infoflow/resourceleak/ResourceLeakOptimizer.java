package soot.jimple.infoflow.resourceleak;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;

import java.util.*;

public class ResourceLeakOptimizer {
    private static ResourceLeakOptimizer instance = new ResourceLeakOptimizer();
    private ISourceSinkManager sourceSinkManager = null;
    public static ResourceLeakOptimizer v() {
        return instance;
    }
    public void initKillingMethods(ISourceSinkManager sourceSinkManager) {
        this.sourceSinkManager = sourceSinkManager;
    }

    private Set<SootMethod> allNotRelatedMs = null;
    private Set<SootMethod> allRelatedMs = null;
    public void jumpIntoMethodsOptimization(IInfoflowCFG icfg, Collection<SootMethod> entrypoints) {
        Stack<SootMethod> currentAllMs = new Stack<>();
        currentAllMs.addAll(entrypoints);

        Set<SootMethod> allMs = new HashSet<>(entrypoints);
        allNotRelatedMs = new HashSet<>();
        allRelatedMs = new HashSet<>();

        //先获得所有的SootMethod
        while (!currentAllMs.isEmpty()) {
            SootMethod currentM = currentAllMs.pop();
            Set<SootMethod> currentCalledMs = getAllCalledSootMethods(currentM, icfg);
            for (SootMethod innerM : currentCalledMs) {
                if (!allMs.contains(innerM)) {
                    currentAllMs.push(innerM);
                    allMs.add(innerM);
                }
            }
        }

        //再整理所有的method是否与killStmt有关
        currentAllMs.addAll(allMs);
        Set<SootMethod> nextTurnCurrentAllMs = null;
        boolean hasReachedFixPoint = false;
        do {
            int preAllNotRelatedMNum = allNotRelatedMs.size();
            int preAllRelatedMNum = allRelatedMs.size();

            nextTurnCurrentAllMs = new HashSet<>();
            while (!currentAllMs.isEmpty()) {
                SootMethod currentM = currentAllMs.pop();
                boolean shouldSave = true;
                boolean isNotRelated = true;
//                if (currentM.getName().contains("finish")) {
//                    System.out.println();
//                }
                Set<SootMethod> currentCalledMs = getAllCalledSootMethods(currentM, icfg);
                for (SootMethod innerM : currentCalledMs) {
                    //如果直接是kill函数，可以直接结束
                    if (sourceSinkManager.isKillStmt(innerM.getSignature()) || allRelatedMs.contains(innerM)) {
                        shouldSave = false;
                        isNotRelated = false;
                        allRelatedMs.add(currentM);
                        break;
                    }
                    if (!allNotRelatedMs.contains(innerM)) {
                        isNotRelated = false;
                    }
                }
                if (isNotRelated) {
                    shouldSave = false;
                    allNotRelatedMs.add(currentM);
                }
                if (shouldSave) {
                    nextTurnCurrentAllMs.add(currentM);
                }

            }
            int currentAllNotRelatedMNum = allNotRelatedMs.size();
            int currentAllRelatedMNum = allRelatedMs.size();
            if (preAllNotRelatedMNum == currentAllNotRelatedMNum && preAllRelatedMNum == currentAllRelatedMNum) {
                hasReachedFixPoint = true;
            }
            currentAllMs.addAll(nextTurnCurrentAllMs);
        } while (!nextTurnCurrentAllMs.isEmpty() && !hasReachedFixPoint);
        //最后，如果nextTurnCurrentAllMs不为空而达到了不动点，那么说明nextTurnCurrentAllMs里的全是循环调用，应该都是不相关的
        if (!nextTurnCurrentAllMs.isEmpty()) {
            allNotRelatedMs.addAll(nextTurnCurrentAllMs);
        }

//        for (SootMethod m : allNotRelatedMs) {
//            if (m.getSignature().contains("com.klinker.android.twitter.activities.compose.Compose: void onStop")) {
//                System.out.println();
//            }
//        }
//        for (SootMethod m : allRelatedMs) {
//            if (m.getSignature().contains("com.klinker.android.twitter.activities.compose.Compose: void onStop")) {
//                System.out.println();
//            }
//        }
    }


    private Set<SootMethod> getAllCalledSootMethods(SootMethod givenM, IInfoflowCFG icfg) {
        Set<SootMethod> results = new HashSet<>();
        if (givenM.hasActiveBody()) {
            for (Unit u :givenM.getActiveBody().getUnits()) {
                Stmt s = (Stmt)u;
                if (s.containsInvokeExpr()) {
                    //这里自身的abstract的调用也加上
                    results.add(s.getInvokeExpr().getMethod());
                    results.addAll(icfg.getCalleesOfCallAt(s));
                }
            }
        }
        return results;
    }

    public boolean shouldJumpIntoForKill(SootMethod givenM) {
        //和kill有关的method肯定需要跳进去看一下
        if (allRelatedMs.contains(givenM)) {
            return true;
        }
        if (allNotRelatedMs.contains(givenM)) {
            return false;
        }
        return true;
    }
}
