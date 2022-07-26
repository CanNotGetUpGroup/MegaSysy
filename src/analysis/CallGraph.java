package analysis;

import ir.*;
import ir.Module;
import ir.instructions.Instructions.*;
import org.antlr.v4.runtime.misc.Pair;
import util.MyIRBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class CallGraph {
    private Module M;
    private HashMap<Function,CallGraphNode> CallNodes;
    private CallGraphNode Main;

    public CallGraph(Module m) {
        M = m;
        for(Function F:m.getFuncList()){
            if(!F.isDefined()){
                continue;
            }
            addToCG(F);
        }
    }

    public void addToCG(Function F){
        if(CallNodes.containsKey(F)){
            return;
        }
        CallGraphNode CGN=getNode(F);
        if(F.getName().equals("main")){
            Main=CGN;
        }
        for(BasicBlock BB:F.getBbList()){
            for(Instruction I:BB.getInstList()){
                if(I instanceof CallInst){
                    CallInst CI=(CallInst)I;
                    Function Callee=CI.getCalledFunction();
                    if(Callee!=null){
                        CGN.addCalledFunction(CI,getNode(Callee));
                    }
                }
            }
        }
    }

    public CallGraphNode getNode(Function F){
        if(CallNodes.containsKey(F)){
            return CallNodes.get(F);
        }
        return new CallGraphNode(this,F);
    }

    public Module getM() {
        return M;
    }

    public void setM(Module m) {
        M = m;
    }

    public HashMap<Function, CallGraphNode> getCallNodes() {
        return CallNodes;
    }

    public void setCallNodes(HashMap<Function, CallGraphNode> callNodes) {
        CallNodes = callNodes;
    }

    public CallGraphNode getMain() {
        return Main;
    }

    public void setMain(CallGraphNode main) {
        Main = main;
    }

    public static void main(String[] args) {
        Module module=Module.getInstance();
        MyIRBuilder builder=MyIRBuilder.getInstance();
        CallGraphNode c1=new CallGraphNode(null,null);
        CallGraphNode c2=new CallGraphNode(null,null);
        Function f=Function.create(DerivedTypes.FunctionType.get(Type.getInt32Ty()),"f",Module.getInstance());
        BasicBlock entry_F=builder.createBasicBlock("entry",f);
        f.setEntryBB(entry_F);
        builder.setInsertPoint(entry_F);

        CallInst ci= (CallInst) builder.createCall(f,new ArrayList<>());
        c1.addCalledFunction(ci,c2);
        c1.removeCall(ci);
        System.out.println("");
    }

    public static class CallGraphNode{
        private CallGraph CG;
        private Function F;
        private ArrayList<Pair<CallInst,CallGraphNode>> CalledFunctions;//调用的函数
        private ArrayList<Pair<CallInst,CallGraphNode>> CallerFunctions;//在这些函数中被调用
        private int ReferTimes;//被调用次数

        public CallGraphNode(CallGraph CG, Function f) {
            this.CG = CG;
            F = f;
            CalledFunctions=new ArrayList<>();
            CallerFunctions=new ArrayList<>();
            ReferTimes=0;
        }

        public void removeCall(CallInst CI){
            for(int i=0,e=CalledFunctions.size();i!=e;i++){
                Pair<CallInst,CallGraphNode> I=CalledFunctions.get(i);
                if(I.a==CI){
                    I.b.dropRef();
                    CalledFunctions.set(i,CalledFunctions.get(e-1));
                    CalledFunctions.remove(e-1);
                    --i;--e;
                    I.b.CallerFunctions.remove(new Pair<>(CI,this));
                }
            }
        }

        /**
         * 添加CGN到CalledFunctions，并将本节点添加到CallerFunctions
         * @param CI
         * @param CGN
         */
        public void addCalledFunction(CallInst CI,CallGraphNode CGN){
            CalledFunctions.add(new Pair<>(CI,CGN));
            CGN.getCallerFunctions().add(new Pair<>(CI,this));
            CGN.ReferTimes++;
        }

        public void addCallerFunction(CallInst CI,CallGraphNode CGN){
            CallerFunctions.add(new Pair<>(CI,CGN));
            CGN.getCalledFunctions().add(new Pair<>(CI,this));
            ReferTimes++;
        }

        public CallGraph getCG() {
            return CG;
        }

        public void setCG(CallGraph CG) {
            this.CG = CG;
        }

        public Function getF() {
            return F;
        }

        public void setF(Function f) {
            F = f;
        }

        public ArrayList<Pair<CallInst,CallGraphNode>> getCalledFunctions() {
            return CalledFunctions;
        }

        public void setCalledFunctions(ArrayList<Pair<CallInst,CallGraphNode>> calledFunctions) {
            CalledFunctions = calledFunctions;
        }

        public int getReferTimes() {
            return ReferTimes;
        }

        public void setReferTimes(int referTimes) {
            ReferTimes = referTimes;
        }

        public ArrayList<Pair<CallInst,CallGraphNode>> getCallerFunctions() {
            return CallerFunctions;
        }

        public void setCallerFunctions(ArrayList<Pair<CallInst,CallGraphNode>> callerFunctions) {
            CallerFunctions = callerFunctions;
        }

        public void dropRef(){
            ReferTimes--;
        }

        public void addRef(){
            ReferTimes++;
        }

        public void dropAllRef(){
            ReferTimes=0;
        }
    }

}
