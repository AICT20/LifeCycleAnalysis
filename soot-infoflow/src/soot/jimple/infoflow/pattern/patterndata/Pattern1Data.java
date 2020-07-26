package soot.jimple.infoflow.pattern.patterndata;

import heros.solver.Pair;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.*;

//pattern1 是在onCreate方法中调用finish()函数时将会跳过onStart、onResume、onPause和onStop方法的执行，直接跳到onDestroy
//这样的话，Pattern1应该包含的LCmethod有：onCreate(), onStart(), onStop(), onDestroy()
//争议点：onPause()以及onResume()是否也需要加入计算？

public class Pattern1Data extends PatternData {
    public Pattern1Data(){
        super();
    }






    protected Map<SootClass, PatternEntryData> getInitialEntryClasses(Set<SootClass> allEntrypoints, IInfoflowCFG icfg) {
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
        Map<SootClass, PatternEntryData> initalEntrypoints = new HashMap<>();
        for (SootMethod m : callFinishMethod) {
            String finishTag = m.getSubSignature();
            if (finishTag.equals(PatternDataConstant.ACTIVITY_ONCREATE) || finishTag.equals(PatternDataConstant.ACTIVITY_ONSTART)) {
                SootClass cClass = m.getDeclaringClass();
                PatternEntryData cData = initalEntrypoints.get(cClass);
                if (null == cData) {
                    cData = new PatternEntryData(cClass);
                    initalEntrypoints.put(cClass, cData);
                }
                updateEntryDataWithLCMethods(cClass, cData);
            }
        }
        return  initalEntrypoints;
    }

    protected void updateEntryDataWithLCMethods(SootClass cClass, PatternEntryData cData) {
        //这部分到时候再减少一些
        String[] methodsigs = new String[] {PatternDataConstant.ACTIVITY_ONCREATE, PatternDataConstant.ACTIVITY_ONSTART, PatternDataConstant.ACTIVITY_ONRESUME,
                PatternDataConstant.ACTIVITY_ONPAUSE, PatternDataConstant.ACTIVITY_ONSTOP, PatternDataConstant.ACTIVITY_ONDESTROY};
        for (String methodsig : methodsigs) {
            SootMethod lcmethod = cClass.getMethodUnsafe(methodsig);
            if (null != lcmethod) {
                Pair<String, SootMethod> pair = new Pair<>(methodsig, lcmethod);
                cData.add(pair);
            }
        }
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
