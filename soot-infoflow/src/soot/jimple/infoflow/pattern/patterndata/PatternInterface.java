package soot.jimple.infoflow.pattern.patterndata;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

import java.util.Map;
import java.util.Set;

public interface PatternInterface {
    void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, IInfoflowCFG nicfg);  //这是两个Analysis都要用的————查找它究竟涉及了哪些Activity,Fragment乃至Service

    Set<SootClass> getEntrypoints();
    void clear();

}
