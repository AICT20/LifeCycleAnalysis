package soot.jimple.infoflow.pattern.alias;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.solver.NormalSolver;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.solver.IInfoflowSolver;

import java.util.Set;

public interface IPatternAliasingStrategy {

    void computeAliasTaints(final NormalState source, final Stmt src, SootMethod method);


//    /**
//     * Notifies the aliasing strategy that a method has been called in the
//     * taint analysis. This may be helpful for interprocedural alias analyses.
//     * @param abs The abstraction on the callee's start unit
//     * @param fSolver The forward solver propagating the taints
//     * @param callee The callee
//     * @param callSite The call site
//     * @param source The abstraction at the call site
//     * @param d1 The abstraction at the caller method's entry point
//     */
//    void injectCallingContext(Abstraction abs, IInfoflowSolver fSolver,
//                              SootMethod callee, Unit callSite, Abstraction source, Abstraction d1);


//    /**
//     * Gets whether this algorithm requires the analysis to be triggered again
//     * when returning from a callee.
//     * @return True if the alias analysis must be triggered again when returning
//     * from a method, otherwise false
//     */
//    boolean requiresAnalysisOnReturn();

//这个交互式的说不定日后会有用
//    public boolean isInteractive()
//    boolean mayAlias(AccessPath ap1, AccessPath ap2);

    NormalSolver getSolver();

    void cleanup();
}
