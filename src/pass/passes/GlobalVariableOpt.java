package pass.passes;

import analysis.DominatorTree;
import ir.*;
import ir.Module;
import ir.instructions.Instructions;
import pass.ModulePass;
import util.MyIRBuilder;

import java.util.ArrayList;
import java.util.Iterator;

public class GlobalVariableOpt extends ModulePass {
    static MyIRBuilder builder=MyIRBuilder.getInstance();

    public GlobalVariableOpt() {
        super();
    }

    @Override
    public void runOnModule(Module M) {
        boolean changed=true;
        while (changed){
            changed=false;
            for(GlobalVariable gv:M.getGlobalVariables()){

            }
        }
    }

    private boolean OptimizeGlobalVars(Module M,DominatorTree DT) {
        boolean Changed = false;

        for (GlobalVariable GV:M.getGlobalVariables()) {
            Changed |= globalToLocal(GV);
        }
        return Changed;
    }

    /**
     * 全局变量本地化
     */
    private boolean globalToLocal(GlobalVariable GV){
        //获取GlobalVariable的基本信息
        GV.calculateInfo();
        if(!GV.isHasMultipleAccessingFunctions()&&GV.getAccessingFunction()!=null){
            //经过函数内联后，除main外都是强连通分量，对于自递归函数，不做Global本地化
            if(GV.getAccessingFunction().getName().equals("main")){
                Type ty = GV.getElementType();
                if(!ty.isArrayTy()){
                    builder.setInsertPoint(GV.getAccessingFunction().getEntryBB());
                    Instructions.AllocaInst AI= (Instructions.AllocaInst) builder.createAlloca(ty);
                    Iterator<Instruction> It=GV.getAccessingFunction().getEntryBB().getInstList().iterator();
                    Instruction I = It.next();
                    while(It.hasNext()){
                        if(!(I instanceof Instructions.AllocaInst)){
                            break;
                        }
                        I=It.next();
                    }
                    builder.setInsertPoint(I);
                    builder.createStore(GV.getOperand(0),AI);
                    GV.replaceAllUsesWith(AI);
                    GV.remove();
                }
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "Global Variable Optimization";
    }
}
