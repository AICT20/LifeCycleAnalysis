package soot.jimple.infoflow.pattern.patterndata;

import soot.SootClass;
import soot.SootMethod;

import java.util.Map;
import java.util.Set;

public interface PatternInterface {
    abstract public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, Map<SootMethod, Set<SootMethod>> totalInvocationMap);  //这是两个Analysis都要用的————查找它究竟涉及了哪些Activity,Fragment乃至Service

    abstract public void updateDummyMainMethod(SootMethod dummyMainMethod);//根据上面找到的涉及到的InvolvedEntrypoints，修改jimple代码，从而修改ICFG的构建

    abstract  public void clear();
}
