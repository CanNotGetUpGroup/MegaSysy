package ir;

import ir.DerivedTypes.FunctionType;
import ir.instructions.Instructions;
import util.CloneMap;
import util.IList;
import util.IListNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import analysis.DominatorTree;
import analysis.LoopInfo;
import util.MyIRBuilder;

import javax.print.attribute.standard.NumberUp;

public class Function extends User {
    private ArrayList<Argument> Arguments;
    private Module Parent;
    private IListNode<Function, Module> funcNode;
    private IList<BasicBlock, Function> bbList;
    private BasicBlock entryBB;
    private boolean isDefined = true;
    private LoopInfo loopInfo = new LoopInfo(); // function内的循环信息
    private ArrayList<Function> callerList;
    private ArrayList<Function> calleeList;
    private boolean sideEffect = false;
    private boolean useGlobalVars = false;
    private DominatorTree dominatorTree = null;
    private HashSet<GlobalVariable> storeGlobalVars;
    private HashSet<GlobalVariable> loadGlobalVars;

    /**
     * 生成一个Function对象
     * 
     * @param type
     * @param name
     * @param module
     * @return
     */
    public static Function create(FunctionType type, String name, Module module) {
        return new Function(type, name, module);
    }

    public Function(FunctionType type, String name, Module module) {
        super(type, name);
        Parent = module;
        funcNode = new IListNode<>(this, module.getFuncList());
        bbList = new IList<>(this);
        // 添加到module
        funcNode.insertIntoListEnd(Parent.getFuncList());
        Arguments = new ArrayList<>();
        calleeList = new ArrayList<>();
        callerList = new ArrayList<>();
        storeGlobalVars = new HashSet<>();
        loadGlobalVars = new HashSet<>();
    }

    public Function(FunctionType type, String name) {
        super(type, name);
        bbList = new IList<>(this);
        Arguments = new ArrayList<>();
    }

    @Override
    public FunctionType getType() {
        return (FunctionType) super.getType();
    }

    public Type getRetType() {
        return getType().getReturnType();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isDefined) {
            sb.append("define dso_local ")
                    .append(this.getType().getReturnType())
                    .append(" @")
                    .append(this.getName())
                    .append("(");
            for (Argument arg : Arguments) {
                sb.append(arg).append(",");
            }
            if (Arguments.size() != 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
        } else {
            String funcName=this.getName();
//            if(funcName.equals("_sysy_stoptime")){
//                funcName="stoptime";
//            }else if(funcName.equals("_sysy_starttime")){
//                funcName="starttime";
//            }
            sb.append("declare ")
                    .append(this.getType().getReturnType())
                    .append(" @")
                    .append(funcName)
                    .append("(");
            for (int i = 0; i < getType().getParamNum(); i++) {
                sb.append(getType().getParamType(i)).append(",");
            }
            if (getType().getParamNum() != 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public ArrayList<Argument> getArguments() {
        return Arguments;
    }

    public void setArguments(ArrayList<Argument> arguments) {
        Arguments = arguments;
    }

    public void setParent(Module parent) {
        Parent = parent;
    }

    public Module getParent() {
        return Parent;
    }

    public IList<BasicBlock, Function> getBbList() {
        return bbList;
    }

    public void setBbList(IList<BasicBlock, Function> bbList) {
        this.bbList = bbList;
    }

    public IListNode<Function, Module> getFuncNode() {
        return funcNode;
    }

    public void setFuncNode(IListNode<Function, Module> funcNode) {
        this.funcNode = funcNode;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    public BasicBlock getEntryBB() {
        if (entryBB == null || entryBB != getBbList().getFirst().getVal()) {
            if (entryBB != null)
                entryBB.setEntryBlock(false);
            entryBB = getBbList().getFirst().getVal();
            entryBB.setEntryBlock(true);
        }
        return entryBB;
    }

    public void setEntryBB(BasicBlock entryBB) {
        this.entryBB = entryBB;
        entryBB.setEntryBlock(true);
    }

    public void remove() {
        for(BasicBlock BB:getBbList()){
            for(Instruction I:BB.getInstList()){
                if(I instanceof Instructions.PHIInst){
                    continue;
                }
                I.remove();
            }
        }
        funcNode.remove();
        dropUsesAsValue();
        dropUsesAsUser();
    }

    /**
     * @return 首个基本块
     */
    public BasicBlock front() {
        return getBbList().getFirst().getVal();
    }

    /**
     *
     * @return 最后一个基本块
     */
    public BasicBlock back() {
        return getBbList().getLast().getVal();
    }

    /**
     * 
     * @return function内的循环信息
     */
    public LoopInfo getLoopInfo() {
        return loopInfo;
    }

    /**
     * 深拷贝该Function
     */
    @Override
    public Function copy(CloneMap cloneMap) {
        if(cloneMap.get(this)!=null){
            return (Function) cloneMap.get(this);
        }
        cloneMap.setInFunctionCopy(true);
        Function ret=new Function(getType(),getName()+"_"+cloneMap.hashCode());
        cloneMap.put(this,ret);
        for(Argument argument:getArguments()){
            Argument copy=argument.copy(cloneMap);
            ret.getArguments().add(copy);
            copy.setParent(ret);
            copy.setArgNo(argument.getArgNo());
        }
        MyIRBuilder builder=MyIRBuilder.getInstance();
        for(BasicBlock BB:getBbList()){
            cloneMap.put(BB,builder.createBasicBlock(ret));
            (BB.copy(cloneMap)).setComment(this.getName()+" "+BB.getComment());
        }
        for(BasicBlock BB:getBbList()){
            for(Instruction I:BB.getInstList()){
                ((Instruction)I.copy(cloneMap)).getInstNode().insertIntoListEnd(((BasicBlock)cloneMap.get(BB)).getInstList());
                if(I.getComment()!= null) I.copy(cloneMap).setComment(I.getComment());
            }
        }
        //添加phi
        for(BasicBlock BB:getBbList()){
            for(Instruction I:BB.getInstList()){
                if(!(I instanceof Instructions.PHIInst)){
                    break;
                }
                Instructions.PHIInst phi=(Instructions.PHIInst)I;
                for (int i = 0; i < phi.getNumOperands(); i++) {
                    phi.copy(cloneMap).addIncoming(phi.getOperand(i).copy(cloneMap), phi.getBlocks().get(i).copy(cloneMap));
                }
            }
        }
        ir.Module.getInstance().rename(ret);
        return ret;
    }

    /**
     *
     * @return 调用该函数的函数列表
     */
    public ArrayList<Function> getCallerList() {
        return callerList;
    }

    /**
     *
     * @return 该函数调用的函数列表
     */
    public ArrayList<Function> getCalleeList() {
        return calleeList;
    }

    /**
     *
     * @return 函数是否有附加影响
     */
    public boolean hasSideEffect(){
        return sideEffect;
    }

    public void setSideEffect(boolean sideEffect) {
        this.sideEffect = sideEffect;
    }

    public boolean useGlobalVars(){
        return useGlobalVars;
    }

    public void setUseGlobalVars(boolean useGlobalVars) {
        this.useGlobalVars = useGlobalVars;
    }

    public DominatorTree getDominatorTree() {
        if(dominatorTree == null){
            dominatorTree = new DominatorTree(this);
        }
        return dominatorTree;
    }

    /**
     * Auto Update DominatorTree
     * @return
     */
    public DominatorTree getAndUpdateDominatorTree() {
        if(dominatorTree == null){
            dominatorTree = new DominatorTree(this);
        }else {
            dominatorTree.update(this);
        }
        return dominatorTree;
    }

    public void setStoreGlobalVars(HashSet<GlobalVariable> storeGlobalVars) {
        this.storeGlobalVars = storeGlobalVars;
    }

    public HashSet<GlobalVariable> getStoreGlobalVars() { 
        return storeGlobalVars;
    }

    public void setLoadGlobalVars(HashSet<GlobalVariable> loadGlobalVars) {
        this.loadGlobalVars = loadGlobalVars;
    }

    public HashSet<GlobalVariable> getLoadGlobalVars() { 
        return loadGlobalVars;
    }
}
