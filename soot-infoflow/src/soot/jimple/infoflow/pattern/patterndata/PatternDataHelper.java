package soot.jimple.infoflow.pattern.patterndata;
import heros.solver.Pair;
import soot.*;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.pattern.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.pattern.patterntag.LCLifeCycleMethodTag;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.tagkit.Tag;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

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
        this.tagsForAllEntryMethods = new HashMap<>();
        this.notRelatedTag = LCLifeCycleMethodTag.getNotRelatedInstance();
        this.notRelatedTagString =  this.notRelatedTag.getName();
        this.notRelatedTagList = new LinkedList<>();
        this.notRelatedTagList.add(this.notRelatedTag);
    }
    public static PatternDataHelper v() {
        return instance;
    }

    private Tag notRelatedTag = null;
    private List<Tag> notRelatedTagList = null;
    private String notRelatedTagString = null;
    private Map<SootClass, PatternEntryData> allEntrypoints = null;
    private Set<SootMethod> allEntryMethods = null;
    private Map<SootMethod, List<Tag>> tagsForAllEntryMethods = null;

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
        upadateTagsForInvolvedEntrypoints();
        performTagsAddition();
        nowicfg.purge();
    }

    private void upadateTagsForInvolvedEntrypoints() {
        Map<SootClass, List<SootClass>> parentClassMap = new HashMap<>();//这是记录的从子Activity->父Activity的map
        Hierarchy h = Scene.v().getActiveHierarchy();
        for (SootClass cClass : allEntrypoints.keySet()) {
            List<SootClass> parents = new LinkedList<>(h.getSuperclassesOf(cClass));
            Iterator<SootClass> it = parents.iterator();
            while (it.hasNext()) {
                SootClass currentClass = it.next();
                if (AndroidEntryPointConstants.isLifecycleClass(currentClass.getName())) {
                    it.remove();
                }
            }
            if (!parents.isEmpty()) {parentClassMap.put(cClass, parents);}
        }
        for (SootClass cClass : allEntrypoints.keySet()) {
            PatternEntryData cdata = allEntrypoints.get(cClass);
            List<SootClass> parents = parentClassMap.get(cClass);
            for (Pair<String, SootMethod> pair : cdata.getEntrypoints()) {
                SootMethod currentMethod = pair.getO2();
                String methodsig = pair.getO1();
                Tag currentTag = new LCLifeCycleMethodTag(methodsig + "_" + cClass.getName());
                List<Tag> tempTags = tagsForAllEntryMethods.get(currentMethod);
                if (null == tempTags) {
                    tempTags = new LinkedList<>();
                    tagsForAllEntryMethods.put(currentMethod, tempTags);
                }
                tempTags.add(currentTag);
                if (null != parents) {
                    for (SootClass parent : parents) {
                        SootMethod parentMethod = parent.getMethodUnsafe(methodsig);
                        if (null != parentMethod) {
                            List<Tag> tempparentTags = tagsForAllEntryMethods.get(parentMethod);
                            if (null == tempparentTags) {
                                tempparentTags = new LinkedList<>();
                                tagsForAllEntryMethods.put(parentMethod, tempparentTags);
                            }
                            if (!tempparentTags.contains(currentTag))
                                tempparentTags.add(currentTag);
                        }
                    }
                }
            }
        }
    }

    private void performTagsAddition() {
        Set<String> allLCMethodSbusigs = AndroidEntryPointConstants.getAllLCMethods();
        ReachableMethods reachableMethods = Scene.v().getReachableMethods();
        reachableMethods.update();
        for (Iterator<MethodOrMethodContext> iter = reachableMethods.listener(); iter.hasNext();) {
            SootMethod sm = iter.next().method();
            if (allLCMethodSbusigs.contains(sm.getSubSignature())) {
                for (Tag tag : getDirectLCMetodTag(sm)) {
                    sm.addTag(tag);
                }
            }
        }
    }

    public List<Tag> getDirectLCMetodTag(SootMethod givenMethod) {
        List<Tag> resultTags = this.tagsForAllEntryMethods.get(givenMethod);
        if (null == resultTags){
            return this.notRelatedTagList;
        } else {
            return resultTags;
        }
    }

    // 需注意，这里需要先确保givenMethod是生命周期函数才行
    public List<Tag> getLCMethodTag(SootMethod givenMethod, List<Tag> currentTags, ByReferenceBoolean shouldKill) {
        if (givenMethod.hasTag(notRelatedTagString)) {
            //说明是不相关Activity,Serivce或者等等的LC函数，那么直接kill
            shouldKill.value = true;
            return null;
        }
        List<Tag> givenMethodTags = givenMethod.getTags();
        if (givenMethodTags.containsAll(currentTags)) {
            return currentTags;//这个一般说明是当前的method是当前state开始的method的父节点，所以保持不变
        } else {
            List<Tag> givenMethodLCTags = this.tagsForAllEntryMethods.get(givenMethod);
            if (currentTags.containsAll(givenMethodLCTags)) {
                return givenMethodLCTags;//这个一般说明当前的method是当前state开始的method的子节点，需要彻底变更成这个的子类的tag
            } else {
                shouldKill.value = true;
                return null;//否则说明这个Activity虽然是involved的，但是却和当前所在的Activity没有父子关系，所以去除
            }
        }

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
    public MultiMap<SootClass, SootField> getEntryFields() {
        MultiMap<SootClass, SootField> fields = new HashMultiMap<>();
        for (PatternData pattern : currentPatterns.values()) {
            fields.putAll(pattern.getEntryFields());
        }
        return fields;
    }

    @Override
    public Map<SootClass, PatternEntryData> getInvolvedEntrypoints() {
        if (null != allEntrypoints && !allEntrypoints.isEmpty()) {
            return allEntrypoints;
        }
        readEntrypointsFromPatterns();
        return allEntrypoints;
    }

    Set<SootMethod> cannotSkilMethods = null;
    @Override
    public Set<SootMethod> getCannotSkipMethods() {
        if (null == cannotSkilMethods) {
            cannotSkilMethods = new HashSet<>();
            for (PatternData pattern : currentPatterns.values()) {
                cannotSkilMethods.addAll(pattern.getCannotSkipMethods());
            }
        }
        return cannotSkilMethods;
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
        this.cannotSkilMethods = null;
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
