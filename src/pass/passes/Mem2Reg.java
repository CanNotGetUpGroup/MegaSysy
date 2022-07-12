package pass.passes;

import ir.*;
import ir.instructions.Instructions;
import pass.FunctionPass;
import pass.Pass;
import ir.instructions.Instructions.*;

import java.util.ArrayList;

public class Mem2Reg extends FunctionPass {
    public Mem2Reg() {
        super();
    }

    @Override
    public String getName() {
        return "Mem2Reg";
    }

    @Override
    public void runOnFunction(Function F) {
        promoteMem2Reg(F);
    }

    public static FunctionPass createMem2Reg(){
        return new Mem2Reg();
    }

    public static void promoteMem2Reg(Function F){
        ArrayList<AllocaInst> allocaInsts=new ArrayList<>();
        BasicBlock BB=F.getEntryBB();

        while(true){
            allocaInsts.clear();
            for(Instruction I:BB.getInstList()){
                if(I instanceof AllocaInst){
                    AllocaInst AI=(AllocaInst)I;
                    if(isAllocaPromotable(AI)){
                        allocaInsts.add(AI);
                    }
                }
            }
            if(allocaInsts.isEmpty()){
                break;
            }
            promoteMemoryToRegister(allocaInsts);
        }
    }

    public static void promoteMemoryToRegister(ArrayList<AllocaInst> allocaInsts){

    }

    public static boolean isAllocaPromotable(AllocaInst AI){
        for(Use use:AI.getUseList()){
            User U=use.getU();
            if(U instanceof LoadInst){

            }else if(U instanceof StoreInst){
                StoreInst SI=(StoreInst)U;
                if(SI.getOperand(0).equals(AI)){
                    return false;
                }
            }else if(U instanceof GetElementPtrInst){
                GetElementPtrInst GEP=(GetElementPtrInst)U;
                if(!GEP.allIndicesZero()){
                    return false;
                }
            }else{
                return false;
            }
        }
        return true;
    }
}
