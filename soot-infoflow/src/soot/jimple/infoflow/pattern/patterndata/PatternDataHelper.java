package soot.jimple.infoflow.pattern.patterndata;

import soot.*;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

import java.util.*;

public class PatternDataHelper implements PatternInterface {
    public static String[] testPattern = new String[]{"2"};
//    public static boolean adaptAllEntrypoints = false;

    String[] tags = null;
    Map<Integer, PatternData> currentPatterns = null;


    private static PatternDataHelper instance = new PatternDataHelper();
    private PatternDataHelper() {
        this.currentPatterns = new HashMap<>();
        this.allEntrypoints = new HashMap<>();
        this.allEntryMethods = new HashSet<>();
    }
    public static PatternDataHelper v() {
        return instance;
    }

    private Map<SootClass, PatternEntryData> allEntrypoints = null;
    private Set<SootMethod> allEntryMethods = null;

    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntryClasses,  IInfoflowCFG icfg) {
        IInfoflowCFG nowicfg = icfg;
        if (null == nowicfg) {
            DefaultBiDiICFGFactory fa = new DefaultBiDiICFGFactory();
            fa.setIsAndroid(true);
            nowicfg = fa.buildBiDirICFG(null, true);
        }


        for (PatternData pattern : currentPatterns.values()) {
            pattern.updateInvolvedEntrypoints(allEntryClasses, nowicfg);
        }
        readEntrypointsFromPatterns();
        nowicfg.purge();
    }

    private void readEntrypointsFromPatterns() {
        allEntrypoints.clear();
        allEntryMethods.clear();
        for (PatternData pattern : currentPatterns.values()) {
            for (SootClass cClass : pattern.getInvolvedEntrypoints().keySet()) {
                PatternEntryData cData = allEntrypoints.get(cClass);
                if (cData == null) {
                    cData = new PatternEntryData(cClass);
                    allEntrypoints.put(cClass, cData);
                }
                cData.merge(pattern.getInvolvedEntrypoints().get(cClass));
            }
        }
        for (PatternEntryData entrypoints : allEntrypoints.values()) {
            allEntryMethods.addAll(entrypoints.getAllMethods());
        }
    }

    @Override
    public Set<SootMethod> getEntryMethods() {
        if (null != allEntryMethods && !allEntryMethods.isEmpty()) {
            return allEntryMethods;
        }
        readEntrypointsFromPatterns();
        return allEntryMethods;
    }

    @Override
    public Map<SootClass, PatternEntryData> getInvolvedEntrypoints() {
        if (null != allEntrypoints && !allEntrypoints.isEmpty()) {
            return allEntrypoints;
        }
        readEntrypointsFromPatterns();
        return allEntrypoints;
    }


//    private Set<Tag> allLCMethodTags = null;
    //这里更新所有Component的lifecycle函数的tag--废弃，7.22后没用了
//    public void updateEntryLifeCycleMethodsTags(Set<SootClass> allentrypoints) {
//        allLCMethodTags = new HashSet();
//        Set<SootClass> allInvolvedEntrypoints = getEntrypoints();
//        Map<Tag, Set<SootClass>> tags2entrypoints = new HashMap<>();//注意，完全有可能一个sootclass包含多个tag
//        //
//        total: for (SootClass currentE : allInvolvedEntrypoints) {
//            List<SootClass> subs = Scene.v().getActiveHierarchy().getSubclassesOf(currentE);
//            for (SootClass temp : allInvolvedEntrypoints) {
//                if (subs.contains(temp) && temp != currentE) {
//                    continue total;
//                }
//            }
//            //能到这里说明它在entrypoints中没有子类了，它是最小的一个
//            LCLifeCycleMethodTag newTag = new LCLifeCycleMethodTag(currentE.getName());
//            Set<SootClass> allSupers = new HashSet<>();
//            allSupers.add(currentE);
//            allSupers.addAll(Scene.v().getActiveHierarchy().getSuperclassesOf(currentE));
//            tags2entrypoints.put(newTag, allSupers);
//        }
//
//        Set<SootClass> notInvolvedEntrypoints = new HashSet<>(allentrypoints);
//        notInvolvedEntrypoints.removeAll(allInvolvedEntrypoints);
//        tags2entrypoints.put(new LCLifeCycleMethodTag("none"), notInvolvedEntrypoints);
//
//        allLCMethodTags = tags2entrypoints.keySet();
//
//        for (Tag keytak : tags2entrypoints.keySet()) {
//            Set<SootClass> classes = tags2entrypoints.get(keytak);
//            for (SootClass currentClass : classes) {
//                for (String subsig : AndroidEntryPointConstantsCopy.getAllLCMethods()) {
//                    SootMethod m = currentClass.getMethodUnsafe(subsig);
//                    if (null != m && !m.hasTag(keytak.getName())) {
//                        m.addTag(keytak);
//                    }
//                }
//            }
//        }
//        System.out.println();
//    }


//    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints) {
//        for (PatternData pattern : currentPatterns.values()) {
//            pattern.updateInvolvedEntrypoints(allEntrypoints, null);
//        }
//
//    }


    @Override
    public void clear() {
        this.currentPatterns.clear();
        this.allEntrypoints.clear();
        this.allEntryMethods.clear();
//        PatternDataConstant.clear();
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
