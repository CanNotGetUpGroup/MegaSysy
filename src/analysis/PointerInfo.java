package analysis;

import ir.*;
import ir.instructions.Instructions;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 计算global和alloca的基本信息
 */
public class PointerInfo {
    private boolean isLoaded;
    private boolean isStored;
    private boolean asParam;
    private Value pointer;
    public ArrayList<Instruction> gepList;
    public static HashMap<GlobalVariable,GlobalStatus> globalStatuses=new HashMap<>();

    public PointerInfo(Value pointer) {
        this.pointer = pointer;
        gepList=new ArrayList<>();
        if(pointer instanceof GlobalVariable&&!globalStatuses.containsKey(pointer)){
            globalStatuses.put((GlobalVariable) pointer,new GlobalStatus());
        }
    }

    public static GlobalStatus getGlobalStatus(GlobalVariable gv){
        return globalStatuses.get(gv);
    }

    public void calculateInfo(Value V){
        if(V instanceof GlobalVariable){
            GlobalVariable GV=(GlobalVariable)V;
            GlobalStatus GS=getGlobalStatus(GV);
            for(Use use:GV.getUseList()){
                User U=use.getU();
                if(U instanceof Instruction){
                    Instruction I=(Instruction)U;
                    Function F=I.getFunction();
                    if(!GS.isHasMultipleAccessingFunctions()){
                        if(GS.getAccessingFunction()==null){
                            GS.setAccessingFunction(F);
                        }else if(GS.getAccessingFunction()!=F){
                            GS.setHasMultipleAccessingFunctions(true);
                        }
                    }
                    if(I instanceof Instructions.LoadInst){
                        isLoaded=true;
                    }else if(I instanceof Instructions.StoreInst){
                        isStored=true;
                    }else if(I instanceof Instructions.CallInst){
                        asParam=true;
                        isLoaded=true;
                        isStored=true;
                    }else if(I instanceof Instructions.GetElementPtrInst){
                        //当前V为数组，向下查找gep的load和store
                        gepList.add(I);
                        calculateInfo(I);
                    }
                }
            }
        }else{
            for(Use use:V.getUseList()){
                User U=use.getU();
                if(U instanceof Instruction){
                    Instruction I=(Instruction)U;
                    if(I instanceof Instructions.LoadInst){
                        isLoaded=true;
                    }else if(I instanceof Instructions.StoreInst){
                        isStored=true;
                    }else if(I instanceof Instructions.CallInst){
                        asParam=true;
                        isLoaded=true;
                        isStored=true;
                    }else if(I instanceof Instructions.GetElementPtrInst){
                        //当前V为数组，向下查找gep的load和store
                        gepList.add(I);
                        calculateInfo(I);
                    }
                }
            }
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public boolean isStored() {
        return isStored;
    }

    public void setStored(boolean stored) {
        isStored = stored;
    }

    public boolean isAsParam() {
        return asParam;
    }

    public void setAsParam(boolean asParam) {
        this.asParam = asParam;
    }

    public Value getPointer() {
        return pointer;
    }

    public void setPointer(Value pointer) {
        this.pointer = pointer;
    }

    public static class GlobalStatus{
        private boolean HasMultipleAccessingFunctions;//是否只在一个函数中使用，用于判断是否可以本地化
        private Function AccessingFunction;

        public boolean isHasMultipleAccessingFunctions() {
            return HasMultipleAccessingFunctions;
        }

        public void setHasMultipleAccessingFunctions(boolean hasMultipleAccessingFunctions) {
            HasMultipleAccessingFunctions = hasMultipleAccessingFunctions;
        }

        public Function getAccessingFunction() {
            return AccessingFunction;
        }

        public void setAccessingFunction(Function accessingFunction) {
            AccessingFunction = accessingFunction;
        }
    }
}
