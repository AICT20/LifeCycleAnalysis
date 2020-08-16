package soot.jimple.infoflow.pattern.entryPointCreators;

import heros.solver.Pair;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.pattern.patterndata.PatternEntryData;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.util.*;

public class PatternMainEntryPointCreator {
    protected Map<SootClass, PatternBaseEntryPointCreator> creators = null;
    protected Set<Pair<SootMethod, SootMethod>> entrypoints = null; //前面的是原本的onCreate主体，后者则是调用的主函数
    protected AndroidEntryPointUtils entryPointUtils = null;
    protected String applicationName = null;
    protected SootClass applicationClass = null;
    protected Set<SootClass> allComponents = null;
    protected MultiMap<SootClass, SootMethod> callbackFunctions = new HashMultiMap<>();
    private MultiMap<SootClass, String> activityLifecycleCallbacks = new HashMultiMap<>();

    public void resetAndRemoveAllGeneratedClasses() {
        if (!entrypoints.isEmpty()) {
            for (Pair<SootMethod, SootMethod> pair : entrypoints) {
                SootMethod generatedMethod = pair.getO2();
                generatedMethod.getDeclaringClass().removeMethod(generatedMethod);
            }
            entrypoints.clear();
        }
        allComponents.clear();
        callbackFunctions.clear();
        activityLifecycleCallbacks.clear();
        creators.clear();
    }

    public PatternMainEntryPointCreator(Set<SootClass> allComponents) {
        creators = new HashMap<>();
        this.allComponents = allComponents;
        entryPointUtils = new AndroidEntryPointUtils();

    }
    public Set<Pair<SootMethod, SootMethod>> create() {
        entrypoints = new HashSet<>();
        initializeApplicationClass();
        for (SootClass c : allComponents) {
            AndroidEntryPointUtils.ComponentType componentType = entryPointUtils.getComponentType(c);
            switch (componentType) {
                case Activity : {
                    PatternBaseEntryPointCreator subCreator = new PatternBaseEntryPointCreator(c, activityLifecycleCallbacks);
                    entrypoints.addAll(subCreator.createMainMethods());
                    break;
                }
                default:{
                    break;
                }
            }

        }
        return entrypoints;
    }

    public void initializeApplicationClass() {

        if (applicationName == null || applicationName.isEmpty())
            return;
        // Find the application class
        for (SootClass currentClass : allComponents) {
            // Is this the application class?
            if (entryPointUtils.isApplicationClass(currentClass) && currentClass.getName().equals(applicationName)) {
                if (applicationClass != null && currentClass != applicationClass)
                    throw new RuntimeException("Multiple application classes in app");
                applicationClass = currentClass;
                break;
            }
        }

        // We can only look for callbacks if we have an application class
        if (applicationClass == null)
            return;

        // Look into the application class' callbacks
        SootClass scActCallbacks = Scene.v()
                .getSootClassUnsafe(AndroidEntryPointConstants.ACTIVITYLIFECYCLECALLBACKSINTERFACE);
        Collection<SootMethod> callbacks = callbackFunctions.get(applicationClass);
        if (callbacks != null) {
            for (SootMethod smCallback : callbacks) {
                // Is this a special callback class? We have callbacks that model activity
                // lifecycle events and ones that model generic events (e.g., low memory)
                if (scActCallbacks != null && Scene.v().getOrMakeFastHierarchy()
                        .canStoreType(smCallback.getDeclaringClass().getType(), scActCallbacks.getType()))
                    activityLifecycleCallbacks.put(smCallback.getDeclaringClass(), smCallback.getSignature());
//                else
//                    applicationCallbackClasses.put(smCallback.getDeclaringClass(), smCallback.getSignature());
            }
        }
    }

    public void setCallbackFunctions(MultiMap<SootClass, SootMethod> callbackFunctions) {
        this.callbackFunctions = callbackFunctions;
    }

    public void removeIrrelevantComponents(Map<SootClass, PatternEntryData> patternComponents) {
        Set<SootClass> allPatternRelatedActivities = patternComponents.keySet();
        Set<Pair<SootMethod, SootMethod>> newEntrypoints = new HashSet<>();
        for (Pair<SootMethod, SootMethod> pair : entrypoints) {
            SootMethod lcmethod = pair.getO1();
            SootMethod lcmainmethod = pair.getO2();
            if (allPatternRelatedActivities.contains(lcmethod.getDeclaringClass())) {
                newEntrypoints.add(pair);
            } else {
                lcmainmethod.getDeclaringClass().removeMethod(lcmainmethod);
            }
        }
    }
}
