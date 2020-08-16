package soot.jimple.infoflow.pattern.patterndata;

import heros.solver.Pair;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.util.MultiMap;

import java.util.Map;
import java.util.Set;

public interface PatternInterface {
    void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, IInfoflowCFG nicfg);  //这是两个Analysis都要用的————查找它究竟涉及了哪些Activity,Fragment乃至Service
    Set<SootMethod> getEntryMethods();
    MultiMap<SootClass, SootField> getEntryFields();
    Map<SootClass, PatternEntryData> getInvolvedEntrypoints();
    Set<SootMethod> getCannotSkipMethods();
    void clear();

}
