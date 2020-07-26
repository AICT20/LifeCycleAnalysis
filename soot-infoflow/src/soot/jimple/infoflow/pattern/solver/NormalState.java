package soot.jimple.infoflow.pattern.solver;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.patternresource.LCResourceOPList;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.util.MyOwnUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

public class NormalState implements Cloneable{
    protected Set<AccessPath> aps;
//    protected Stmt currentStmt = null;
//    protected LinkedList<Stmt> traces = null; //这里换成block可能会更好一些？  还是直接不存了？
    //那么后向的alias要如何计算？？？
    //TODO 先全都不存，只生成ops试试看
    protected Stmt activationUnit = null;
    protected SourceSinkDefinition def = null;
    protected LCResourceOPList ops = null;
    //  每个State都有个起始点，即entrypoints中的一个，而finishStmt则是该entrypoint的结尾，到达该Stmt时进行kill
    protected Stmt finishStmt = null;
    protected int hashCode = 0;

    //下面是不需要计入hash和equals的
    protected NormalState zeroState = null;
    protected int propagationPathLength = 0;
    protected boolean exceptionThrown = false;
    //可以存个之前的stmt来debug看看
    protected NormalState preState = null;
    protected LinkedList<Stmt> preStmts = null;

    public static NormalState getInitialState() {
        NormalState zeroValue = new NormalState(Collections.singleton(AccessPath.getZeroAccessPath()), null);
        zeroValue.zeroState = zeroValue;
        return zeroValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        NormalState other = (NormalState) obj;

        // If we have already computed hash codes, we can use them for
        // comparison
        if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
            return false;
        if (this.def != other.def)
            return false;
        if (this.finishStmt != other.finishStmt)
            return false;
        if (activationUnit == null) {
            if (other.activationUnit != null)
                return false;
        } else if (!activationUnit.equals(other.activationUnit))
            return false;
        if (null == this.ops) {
            if (other.ops != null) {
                return false;
            }
        } else {
            if (!this.ops.equals(other.ops))
                return false;
        }
        if (null == this.aps || this.aps.isEmpty()) {
            if (other.aps != null && !other.aps.isEmpty()) {
                return false;
            }
        } else {
            if (!MyOwnUtils.setCompare(this.aps, other.aps)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public int hashCode() {
        if (this.hashCode != 0)
            return hashCode;

        final int prime = 31;
        int result = 1;

        // deliberately ignore prevAbs
        result = prime * result + ((def == null) ? 0 : def.hashCode());
        result = prime * result + ((finishStmt == null) ? 0 : finishStmt.hashCode());
        result = prime * result + ((activationUnit == null) ? 0 : activationUnit.hashCode());
        result = prime * result + (exceptionThrown ? 1231 : 1237);
        if (null != aps && !aps.isEmpty()) {
            int aphash = 0;
            for (AccessPath ap : aps) {
                aphash += ap.hashCode();
            }
            result = prime * result + aphash;
        }
        this.hashCode = result;
        return this.hashCode;
    }
    public int getPathLength() {
        return propagationPathLength;
    }

    protected NormalState(Set<AccessPath> newaps, NormalState original) {
        if (original == null) {
            def = null;
            exceptionThrown = false;
            activationUnit = null;
            ops = null;
        } else {
            def = original.def;
            exceptionThrown = original.exceptionThrown;
            activationUnit = original.activationUnit;
            ops = original.ops;
            preStmts = original.preStmts;
            zeroState = original.zeroState;
        }
        this.aps = newaps;
    }

    @Override
    public NormalState clone() {
        NormalState abs = new NormalState(this.aps, this);
        abs.propagationPathLength = propagationPathLength + 1;
        abs.preState = this;
        assert abs.equals(this);
        return abs;
    }

    public boolean isAbstractionActive() {
        return activationUnit == null;
    }
    public Stmt getActivationUnit() {return activationUnit;}
    public Set<AccessPath> getAps() {return aps;}

    public NormalState getActiveCopy() {
        if (this.isAbstractionActive())
            return this;

        NormalState a = clone();
        a.activationUnit = null;
        return a;
    }


    //这个只在分析初始化时用到
    public NormalState deriveFinishStmtCopy(Stmt finishStmt) {
        if (this.finishStmt == finishStmt) {
            return this;
        }
        NormalState a = clone();
        a.finishStmt = finishStmt;
        a.zeroState = a;
        return a;
    }
    public NormalState getZeroState() { return this.zeroState;}
    public boolean isZeroState() { return this.zeroState == this;}

}
