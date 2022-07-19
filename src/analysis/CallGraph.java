package analysis;

import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import ir.Module;
import ir.instructions.Instructions.*;

import java.util.ArrayList;
import java.util.HashMap;

public class CallGraph {
    Module M;
    HashMap<Function,CallGraphNode> CallNodes;

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

    public static class CallGraphNode{
        private CallGraph CG;
        private Function F;
        private ArrayList<CallGraphNode> CalledFunctions;//引用的函数
        private int ReferTimes;//被引用次数

        public CallGraphNode(CallGraph CG, Function f) {
            this.CG = CG;
            F = f;
            CalledFunctions=new ArrayList<>();
            ReferTimes=0;
        }

        public void addCalledFunction(CallInst CI,CallGraphNode CGN){
            CalledFunctions.add(CGN);
            CGN.ReferTimes++;
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

        public ArrayList<CallGraphNode> getCalledFunctions() {
            return CalledFunctions;
        }

        public void setCalledFunctions(ArrayList<CallGraphNode> calledFunctions) {
            CalledFunctions = calledFunctions;
        }

        public int getReferTimes() {
            return ReferTimes;
        }

        public void setReferTimes(int referTimes) {
            ReferTimes = referTimes;
        }
    }

}
