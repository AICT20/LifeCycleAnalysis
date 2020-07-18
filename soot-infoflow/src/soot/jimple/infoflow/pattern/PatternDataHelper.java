package soot.jimple.infoflow.pattern;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.pattern.patterndata.*;
import soot.jimple.infoflow.pattern.patterntag.LCLifeCycleMethodTag;
import soot.jimple.infoflow.resourceleak.AndroidEntryPointConstantsCopy;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.tagkit.Tag;
import java.util.*;

public class PatternDataHelper implements PatternInterface {
    public static String[] testPattern = new String[]{"1"};
    public static boolean adaptAllEntrypoints = false;

    String[] tags = null;
    Map<Integer, PatternData> currentPatterns = null;


    private static PatternDataHelper instance = new PatternDataHelper();
    private PatternDataHelper() {
        this.currentPatterns = new HashMap<>();
    }
    public static PatternDataHelper v() {
        return instance;
    }

    //2020.7.12修正可达性算法——》不再更新全部calltrace，只找从finish开始的

    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints,  IInfoflowCFG icfg) {
        IInfoflowCFG nowicfg = icfg;
        if (null == nowicfg) {
            DefaultBiDiICFGFactory fa = new DefaultBiDiICFGFactory();
            fa.setIsAndroid(true);
            nowicfg = fa.buildBiDirICFG(null, true);
        }

        for (PatternData pattern : currentPatterns.values()) {
            pattern.updateInvolvedEntrypoints(allEntrypoints, nowicfg);
        }
        nowicfg.purge();
    }

    @Override
    public Set<SootClass> getEntrypoints() {
        Set<SootClass> allEntrypoints = new HashSet<>();
        for (PatternData pattern : currentPatterns.values()) {
            allEntrypoints.addAll(pattern.getEntrypoints());
        }
        return allEntrypoints;
    }

    private Set<Tag> allLCMethodTags = null;
    //这里更新所有Component的lifecycle函数的tag
    public void updateEntryLifeCycleMethodsTags(Set<SootClass> allentrypoints) {
        allLCMethodTags = new HashSet();
        Set<SootClass> allInvolvedEntrypoints = getEntrypoints();
        Map<Tag, Set<SootClass>> tags2entrypoints = new HashMap<>();//注意，完全有可能一个sootclass包含多个tag
        //
        total: for (SootClass currentE : allInvolvedEntrypoints) {
            List<SootClass> subs = Scene.v().getActiveHierarchy().getSubclassesOf(currentE);
            for (SootClass temp : allInvolvedEntrypoints) {
                if (subs.contains(temp) && temp != currentE) {
                    continue total;
                }
            }
            //能到这里说明它在entrypoints中没有子类了，它是最小的一个
            LCLifeCycleMethodTag newTag = new LCLifeCycleMethodTag(currentE.getName());
            Set<SootClass> allSupers = new HashSet<>();
            allSupers.add(currentE);
            allSupers.addAll(Scene.v().getActiveHierarchy().getSuperclassesOf(currentE));
            tags2entrypoints.put(newTag, allSupers);
        }

        Set<SootClass> notInvolvedEntrypoints = new HashSet<>(allentrypoints);
        notInvolvedEntrypoints.removeAll(allInvolvedEntrypoints);
        tags2entrypoints.put(new LCLifeCycleMethodTag("none"), notInvolvedEntrypoints);

        allLCMethodTags = tags2entrypoints.keySet();

        for (Tag keytak : tags2entrypoints.keySet()) {
            Set<SootClass> classes = tags2entrypoints.get(keytak);
            for (SootClass currentClass : classes) {
                for (String subsig : AndroidEntryPointConstantsCopy.getAllLCMethods()) {
                    SootMethod m = currentClass.getMethodUnsafe(subsig);
                    if (null != m && !m.hasTag(keytak.getName())) {
                        m.addTag(keytak);
                    }
                }
            }
        }
        System.out.println();
    }


    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints) {
        for (PatternData pattern : currentPatterns.values()) {
            pattern.updateInvolvedEntrypoints(allEntrypoints, null);
        }

    }



//    private void buildTotalInvokeMap() {
//        for (SootMethod m : methodDirectInvokeMap.keySet()) {
//            Stack<SootMethod> currentMethods = new Stack<>();
//            Set<SootMethod> allMethods = new HashSet<>(methodDirectInvokeMap.get(m));
//            currentMethods.addAll(methodDirectInvokeMap.get(m));
//            while (!currentMethods.isEmpty()) {
//                SootMethod currentMethod = currentMethods.pop();
//                Set<SootMethod> currentInvokeMethods = methodDirectInvokeMap.get(currentMethod);
//                if (null != currentInvokeMethods && !currentInvokeMethods.isEmpty()) {
//                    for (SootMethod innerM : currentInvokeMethods) {
//                        if (!allMethods.contains(innerM)) {
//                            allMethods.add(innerM);
//                            currentMethods.add(innerM);
//                        }
//                    }
//                }
//            }
//            methodTotalInvokeMap.put(m, allMethods);
//        }
//    }
//
//    private void buildDirectInvokeMap(Set<SootClass> allEntrypoints) {
//        Stack<SootMethod> currentMethods = new Stack<>();
//        Set<SootMethod> allMethods = new HashSet<>();
//        for (SootClass c : allEntrypoints) {
//            for (SootMethod m : c.getMethods()) {
//                if (m.hasActiveBody()) {
//                    currentMethods.add(m);
//                    allMethods.add(m);
//                }
//            }
//        }
//
//        while (!currentMethods.isEmpty()){
//            SootMethod m = currentMethods.pop();
//            Set<SootMethod> invokedMethods = getAllInvokedMethodsInGivenMethod(m);
//            Set<SootMethod> allInvokedMethods = methodDirectInvokeMap.get(m);
//            if (null == allInvokedMethods) {
//                allInvokedMethods = new HashSet<>();
//                methodDirectInvokeMap.put(m, allInvokedMethods);
//            }
//            allInvokedMethods.addAll(invokedMethods);
//            for (SootMethod innerM : invokedMethods) {
//                if (!allMethods.contains(innerM)) {
//                    allMethods.add(innerM);
//                    currentMethods.push(innerM);
//                }
//            }
//        }
//    }

//    private Set<SootMethod> getAllInvokedMethodsInGivenMethod(SootMethod givenM) {
//        Set<SootMethod> returnMethods = new HashSet<>();
//        Body b = null;
//        try {
//            b = givenM.getActiveBody();
//        } catch (Exception e) {
//            return returnMethods;
//        }
//
//        for (Unit u : b.getUnits()) {
//            if (u instanceof Stmt && ((Stmt)u).containsInvokeExpr()) {
//                InvokeExpr exp = ((Stmt) u).getInvokeExpr();
//                SootMethod innerM = exp.getMethod();
//                returnMethods.add(innerM);
//                //这里还要搞个多态
//                addPolymorphicMethods(returnMethods, innerM);
//            }
//        }
//        return returnMethods;
//    }
//
//    private void addPolymorphicMethods(Set<SootMethod> results, SootMethod givenInnerM) {
//        SootClass currentClass = givenInnerM.getDeclaringClass();
//        Set<SootClass> allSubClass = new HashSet<>();
//        Hierarchy h = Scene.v().getActiveHierarchy();
//        if (currentClass.isInterface()) {
//            try {
//                allSubClass.addAll(h.getImplementersOf(currentClass));
//            } catch (NullPointerException e) {
//            }
//            for (SootClass superInterface : h.getSubinterfacesOf(currentClass)) {
//                allSubClass.addAll(h.getImplementersOf(superInterface));
//            }
//        } else if (currentClass.isConcrete() || currentClass.isAbstract()) {
//            allSubClass.addAll(h.getSubclassesOf(currentClass));
//        }
//        if (allSubClass.isEmpty()){
//            return;
//        }
//        String subsig = givenInnerM.getSubSignature();
//        for (SootClass nowSubClass : allSubClass) {
//            SootMethod m = nowSubClass.getMethodUnsafe(subsig);
//            if (null != m) {
//                results.add(m);
//            }
//        }
//
//    }


    @Override
    public void clear() {
        PatternDataConstant.clear();
        for (PatternData pattern : currentPatterns.values()) {
            pattern.clear();
        }
    }

    boolean hasPattern1 = false;
    boolean hasPattern2 = false;
    boolean hasPattern3 = false;


    public void init(String[] args, int targetSdk, int minSdk){
        if (null != args && args.length != 0) {
            tags = args;
            for (String arg : args) {
                if (arg.equals("1")) {
                    currentPatterns.put(1, new Pattern1Data());
                    hasPattern1 = true;
                } else if (arg.equals("2")){
                    currentPatterns.put(2, new Pattern2Data(targetSdk, minSdk));
                    hasPattern2 = true;
                } else if (arg.equals("3")) {
                    currentPatterns.put(3, new Pattern3Data());
                    hasPattern3 = true;
                }
            }
        }
    }

    public Pattern1Data getPattern1() {
        PatternData data = currentPatterns.get(1);
        return (null==data)?null:(Pattern1Data)data;
    }

    public boolean hasPattern1() {
        return hasPattern1;
    }

    public Pattern2Data getPattern2() {
        PatternData data = currentPatterns.get(2);
        return (null==data)?null:(Pattern2Data)data;
    }

    public boolean hasPattern2() {
        return hasPattern2;
    }
    public Pattern3Data getPattern3() {
        PatternData data = currentPatterns.get(3);
        return (null==data)?null:(Pattern3Data)data;
    }
    public boolean hasPattern3() {
        return hasPattern3;
    }
}
