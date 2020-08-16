package soot.jimple.infoflow.pattern.solver;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.patternresource.LCResourceOPList;
import soot.jimple.infoflow.pattern.sourceandsink.PatternFieldSourceSinkDefinition;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.MyOwnUtils;
import soot.tagkit.Tag;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.util.*;

public class NormalState implements Cloneable{
    protected Set<AccessPath> aps;
//    protected Stmt currentStmt = null;
//    protected LinkedList<Stmt> traces = null; //这里换成block可能会更好一些？  还是直接不存了？
    //那么后向的alias要如何计算？？？
    //TODO 先全都不存，只生成ops试试看
    protected PatternFieldSourceSinkDefinition def = null;
    protected LCResourceOPList ops = null;
    //  每个State都有个起始点，即entrypoints中的一个，而finishStmt则是该entrypoint的结尾，到达该Stmt时进行kill
    protected SootMethod entrymethod = null; //这个和下面的finishStmt是一致的，所以存一个就行了
    protected Collection<Unit> finishStmts = null;
    protected SootClass entryClass = null; //这个和上面的entrymethod是一致的
    protected List<Tag> lcmethodTags = null;  //注意，这里的tag的list总数是有限的，且不会生成新的list，因此直接比较就行了

    protected int hashCode = 0;

    //下面是不需要计入hash和equals的
    protected NormalState zeroState = null; //注意，zeroState可能会生成新的了
    protected int propagationPathLength = 0;
    protected boolean exceptionThrown = false;

    protected Stack<Stmt> callStack = new Stack<>();
    //可以存个之前的stmt来debug看看-----先全去了
    protected NormalState preState = null;
    protected List<Stmt> preStmts = null;

    //TODO 记录跳转的深度，用来减少错误传播的
    protected Stack<Stmt> invocationStack = null;

    public static NormalState getInitialState() {
        NormalState zeroValue = new NormalState(Collections.EMPTY_SET, null);
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
        if (this.entrymethod != other.entrymethod)
            return false;
        if (this.lcmethodTags != other.lcmethodTags)
            return false;
        if (this.OPposition != other.OPposition)
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
        Set<AccessPath> inactiveaps = this.getInactiveAps();
        Set<AccessPath> otherinactiveaps = other.getInactiveAps();
        if (null == inactiveaps || inactiveaps.isEmpty()) {
            if (otherinactiveaps != null && !otherinactiveaps.isEmpty()) {
                return false;
            }
        } else {
            if (!MyOwnUtils.setCompare(inactiveaps,otherinactiveaps)) {
                return false;
            }
        }
        if (this.callStack.isEmpty()) {
            if (!other.callStack.isEmpty()) {
                return false;
            }
        } else {
            if (other.callStack.isEmpty()) {
                return false;
            }
            //contextsensitive-只采用一层
            if (this.callStack.peek() != other.callStack.peek()) {
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
        result = prime * result + ((entrymethod == null) ? 0 : entrymethod.hashCode());
        result = prime * result + ((lcmethodTags == null) ? 0 : lcmethodTags.hashCode());
        result = prime * result + OPposition;
        result = prime * result + (exceptionThrown ? 1231 : 1237);
        if (null != aps && !aps.isEmpty()) {
            int aphash = 0;
            for (AccessPath ap : aps) {
                aphash += ap.hashCode();
            }
            result = prime * result + aphash;
        }
        Set<AccessPath> inactiveApSet = getInactiveAps();
        if (null != inactiveApSet && !inactiveApSet.isEmpty()) {
            int aphash = 0;
            for (AccessPath ap : inactiveApSet) {
                aphash += ap.hashCode();
            }
            result = prime * result + aphash;
        }
        int callStmtHash = 0;
        if (!this.callStack.isEmpty()) {
            callStmtHash = this.callStack.peek().hashCode();
        }
        result = prime * result + callStmtHash;

//        result = prime * result + (null==aliasAp ? 0 : aliasAp.hashCode());

        this.hashCode = result;
        return this.hashCode;
    }
    public int getPathLength() {
        return propagationPathLength;
    }
    public NormalState getZeroState() { return this.zeroState;}
    public boolean isZeroState() { return this.zeroState == this;}
    public Set<AccessPath> getAps() {return aps;}
    public PatternFieldSourceSinkDefinition getDef() {return def;}
    public boolean isStatic() {if(null != def){return def.isStatic();}return false;}
    public SootField getField() {if(null != def){return def.getField();}return null;}
    public LCResourceOPList getOps() {return ops;}
    public SootMethod getEntryMethod() {return this.entrymethod;}
    public List<Tag> getLcmethodTags() {return this.lcmethodTags;}

    public SootClass getEntryClass() {return this.entryClass;}
    public boolean shouldFinish(Unit stmt) {if (finishStmts.contains(stmt)){return true;}else{return false;}}


    protected NormalState(Set<AccessPath> newaps, NormalState original) {
        if (original == null) {
            def = null;
            exceptionThrown = false;
            OPposition = -1;
            preStmts = Collections.EMPTY_LIST;
            finishStmts = Collections.EMPTY_SET;
            inActivationMap = new HashMultiMap<>();
            ops = LCResourceOPList.getInitialList();
        } else {
            def = original.def;
            exceptionThrown = original.exceptionThrown;
            OPposition = original.OPposition;
            inActivationMap = original.inActivationMap;
            aliasAp = original.aliasAp;
            ops = original.ops;
            preStmts = original.preStmts;
            zeroState = original.zeroState;
            entrymethod = original.entrymethod;
            finishStmts = original.finishStmts;
            lcmethodTags = original.lcmethodTags;
            callStack = original.callStack;
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


    //这个只在分析初始化时用到
    public NormalState deriveFinishStmtCopy(Collection<Unit> finishStmts, SootMethod entryMethod, List<Tag> tags) {
        if (this.entrymethod == entryMethod) {
            return this;
        }
        NormalState a = clone();
        a.finishStmts = finishStmts;
        a.entrymethod = entryMethod;
        a.lcmethodTags = tags;
        a.zeroState = a;
        return a;
    }


    public NormalState deriveNewState(Set<AccessPath> newaps, PatternFieldSourceSinkDefinition def, ByReferenceBoolean hasgeneratedNewState, Stmt stmt) {
        if (this.aps == newaps && this.def == def) {
            return this;
        }
        NormalState newState = deriveNewState(newaps, hasgeneratedNewState, stmt);
        newState.def = def;
        return newState;
    }

    public NormalState deriveNewState(Set<AccessPath> newaps, ByReferenceBoolean hasgeneratedNewState, Stmt stmt) {
        if (this.aps == newaps) {
            return this;
        }
        NormalState a = null;
        if (hasgeneratedNewState.value) { a = this;} else {a = clone();hasgeneratedNewState.value = true;}
        a.aps = newaps;
        a.preStmts = new LinkedList<>(a.preStmts);
        a.preStmts.add(stmt);
        return a;
    }

    //这是BW传播专用的，更新aps和inactiveapmap，并且如果是null的情况就说明保持原样
    public NormalState deriveNewAliasState(AccessPath newAliasAp, MultiMap<Stmt, AccessPath> newInactiveApMap, ByReferenceBoolean hasgeneratedNewState, Stmt stmt) {
        NormalState a = null;
        if (hasgeneratedNewState.value) { a = this;} else {a = clone();hasgeneratedNewState.value = true;}
        a.aliasAp = newAliasAp;
        if (newInactiveApMap != null) {
            a.inActivationMap = newInactiveApMap;
        }
        a.preStmts = new LinkedList<>(a.preStmts);
        a.preStmts.add(stmt);
        return a;
    }


    public NormalState deriveNewOPListState(ByReferenceBoolean hasgeneratedNewState, LCResourceOPList newops, Stmt stmt) {
        if (this.ops == newops || this.ops.equals(newops)) {
            return this;
        }
        NormalState a = null;
        if (hasgeneratedNewState.value) { a = this;} else {a = clone();hasgeneratedNewState.value = true;}
        a.ops = newops;
        a.preStmts = new LinkedList<>(a.preStmts);
        a.preStmts.add(stmt);
        return a;
    }

    public NormalState deriveNewTagListState(ByReferenceBoolean hasgeneratedNewState, List<Tag> newttags, Stmt stmt) {
        if (this.lcmethodTags == newttags) {
            return this;
        }
        NormalState a = null;
        if (hasgeneratedNewState.value) { a = this;} else {a = clone();hasgeneratedNewState.value = true;}
        a.lcmethodTags = newttags;
        a.preStmts = new LinkedList<>(a.preStmts);
        a.preStmts.add(stmt);
        return a;
    }

    //生成新的zerostate的时候，只可能是遇到notcheck的op操作，比如finish()
    public NormalState deriveNewZeroState(ByReferenceBoolean hasgeneratedNewState, LCResourceOPList newops, Stmt stmt) {
        if (this.ops == newops) {
            return this;
        }
        NormalState a = clone();
        //注意，这里的hasgeneratedNewState不要改，并且肯定为false
        assert  !hasgeneratedNewState.value;
//        if (hasgeneratedNewState.value) { a = this;} else {a = clone();hasgeneratedNewState.value = true;}
        a.ops = newops;
        a.preStmts = new LinkedList<>(a.preStmts);
        a.preStmts.add(stmt);
        a.zeroState = a;
        return a;
    }


    protected MultiMap<Stmt, AccessPath> inActivationMap = null; //activationMap记录所有inactive的Ap和active的stmt
    protected int OPposition = -1;//这个或许不需要加进equals里和hash里边
    public  MultiMap<Stmt, AccessPath> getInActivationMap() {return this.inActivationMap;}
    //下面是临时存的，不用管
    protected Stmt nextOpStmt = null;
    protected Set<AccessPath> inactiveAps = null;//这里用inactiveAps来代替inActivationMap进行equals和hashcode，方便一些
    public boolean isActive() {return getInactiveAps().isEmpty();}
    public Set<AccessPath> getInactiveAps() {
        if (inactiveAps == null) {
            inactiveAps = new HashSet<>();
            if (null != inActivationMap) {
                inactiveAps.addAll(inActivationMap.values());
            }
        }
        return inactiveAps;
    }

    protected AccessPath aliasAp = null;//专门用来计算alias的ap，当它为空时就解除alias，然后开始正向传播
    protected Stmt activationStmt = null;
    public AccessPath getAliasAp() {return aliasAp;}
    public Stmt getActivationStmt() {return activationStmt;}
    public NormalState deriveAliasState(AccessPath apToBW, Stmt stmt, boolean isToSlice) {
        NormalState a = clone();
        a.aps = new HashSet<>();
        a.aliasAp = apToBW;
        a.activationStmt = isToSlice?null:stmt;
        a.preStmts = new LinkedList<>(a.preStmts);
        a.preStmts.add(stmt);
        a.inActivationMap = new HashMultiMap<>();
        a.inActivationMap.putAll(stmt, aps);


        //同时也要更新op位置
        if (a.ops.isEmpty()) {
            a.OPposition = 0;
        } else {
            a.OPposition = a.ops.getSize();
            a.nextOpStmt = a.ops.getStmt(a.OPposition-1);
        }
        return a;
    }

    //前向传播时需要更新op的position以及inactive的stmt
    //TODO   !!!!!!!!注意，这要把active的ap先拿出来，跳过rules里的strongupdate和apupdate修改，并最后再更新到aps里边去
    protected Set<AccessPath> tempAps = null;
    public NormalState FWCheckCurrentStmt(Stmt stmt, ByReferenceBoolean hasGeneratedNewState) {
        NormalState a = null;
        boolean shouldActive = this.inActivationMap.containsKey(stmt);
        boolean shouldMoveOpCursor = this.OPposition != -1 && nextOpStmt == stmt;
        if (!shouldActive && !shouldMoveOpCursor) {
            return this;
        }
        if (hasGeneratedNewState.value) {
            a = this;
        } else {
            a = clone();
            hasGeneratedNewState.value = true;
        }
        if (shouldActive) {
            a.inActivationMap = new HashMultiMap<>(this.inActivationMap);
            a.tempAps = this.inActivationMap.get(stmt);
            a.inActivationMap.remove(stmt);
            a.aps = new HashSet<>(this.aps);
        }
        if (shouldMoveOpCursor) {
            a.OPposition++;
            if (a.OPposition >= a.ops.getSize()) {
                a.OPposition = -1;
            } else {
                a.nextOpStmt = a.ops.getStmt(a.OPposition);
            }
        }
        return a;
    }
    //这个和上面的必须配套使用
    public void updateActiveAps() {
        if (this.tempAps != null) {
            this.aps.addAll(tempAps);
            this.tempAps = null;
        }
    }
    public Set<AccessPath> getTempAps() {//这个只用在op的rule里边
        return this.tempAps;
    }

    //后向传播计算Alias以及Slice时，只需要计算op位置
    public NormalState BWCheckCurrentStmt(Stmt stmt, ByReferenceBoolean hasGeneratedNewState) {
        NormalState a = null;
        boolean shouldMoveOpCursor = this.OPposition != 0 && nextOpStmt == stmt;
        if (!shouldMoveOpCursor) {
            return this;
        }
        if (hasGeneratedNewState.value) {
            a = this;
        } else {
            a = clone();
            hasGeneratedNewState.value = true;
        }
        a.OPposition--;
        if (a.OPposition != 0) {
            a.nextOpStmt = a.ops.getStmt(a.OPposition-1);
        }
        return a;
    }

    //目前是从最开始的起点进行重新开始的
    public NormalState deriveNormalFromAliasState(Stmt stmt, ByReferenceBoolean hasGeneratedNewState) {
        if (this.OPposition != 0) {
            return null;//说明BW时，回退顺序不对，这条路径不能要
        }

        NormalState a;
        if (hasGeneratedNewState.value) {
            a = this;
        } else {
            a = clone();
            hasGeneratedNewState.value = true;
        }
        a.aliasAp = null;
        a.preStmts = new LinkedList<>(a.preStmts);
        a.preStmts.add(stmt);
        return a;
    }

    //注意！！！这里如果是zeroState的话也要更新zeroState的状态
    public NormalState deriveCallState(Stmt callStmt, ByReferenceBoolean hasGeneratedNewState) {
        NormalState a;
        if (hasGeneratedNewState.value) {
            a = this;
        } else {
            a = clone();
            hasGeneratedNewState.value = true;
        }
        Stack<Stmt> originalStack = this.callStack;
        a.callStack = new Stack<>();
        a.callStack.addAll(originalStack);
        a.callStack.push(callStmt);
        if (this.isZeroState()) {
            a.zeroState = a;
        }
        return a;
    }


    public NormalState deriveReturnState(Stmt callStmt, ByReferenceBoolean hasGeneratedNewState) {
        NormalState a;
        if (hasGeneratedNewState.value) {
            a = this;
        } else {
            a = clone();
            hasGeneratedNewState.value = true;
        }
        Stack<Stmt> originalStack = this.callStack;
        if (originalStack.isEmpty() || originalStack.peek() != callStmt) {return null;}

        a.callStack = new Stack<>();
        a.callStack.addAll(originalStack);
        a.callStack.pop();
        if (this.isZeroState()) {
            a.zeroState = a;
        }
        return a;
    }

    }
