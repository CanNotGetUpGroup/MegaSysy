package pass.passes;

import analysis.DominatorTree;
import analysis.PointerInfo;
import ir.*;
import ir.Module;
import ir.instructions.Instructions;
import pass.ModulePass;
import util.MyIRBuilder;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 全局变量本地化，以及全局变量常数化
 * 该Pass在Function Inline后执行
 */
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
            for(GlobalVariable gv:new ArrayList<>(M.getGlobalVariables())){
                changed|=globalToLocal(gv);
            }
        }
    }

    /**
     * 全局变量本地化，顺便优化没有被再次赋值的全局变量，删除没有被读取的全局变量
     */
    private boolean globalToLocal(GlobalVariable GV){
        //获取GlobalVariable的基本信息
        PointerInfo PI=new PointerInfo(GV);
        PI.calculateInfo(GV);
        PointerInfo.GlobalStatus GS=PointerInfo.getGlobalStatus(GV);
        if(!GS.isHasMultipleAccessingFunctions()&&GS.getAccessingFunction()!=null){
            //经过函数内联后，除main外都是强连通分量，对于自递归函数，不做Global本地化
            if(GS.getAccessingFunction().getName().equals("main")){
                Type ty = GV.getElementType();
                if(!ty.isArrayTy()){//不对数组进行本地化
//                    System.out.println("localize");
                    builder.setInsertPoint(GS.getAccessingFunction().getEntryBB());
                    Instructions.AllocaInst AI= (Instructions.AllocaInst) builder.createAlloca(ty);
                    AI.setVarName("global_"+GV.getName().substring(1));
                    Iterator<Instruction> It=GS.getAccessingFunction().getEntryBB().getInstList().iterator();
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
                    return true;
                }
            }
        }
        //未被读取的全局变量
        if(!PI.isLoaded()){
            boolean changed=cleanupConstantGlobalUsers(GV,(Constant) GV.getOperand(0));
            if(GV.getUseList().isEmpty()){
//                System.out.println("isn't loaded");
                GV.remove();
                return true;
            }
            return changed;
        }
        if(!PI.isStored()){
            cleanupConstantGlobalUsers(GV, (Constant) GV.getOperand(0));
            if(GV.getUseList().isEmpty()){
//                System.out.println("isn't stored");
                GV.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * 确定了当前global初始化后可以看作一个常量
     */
    public static boolean cleanupConstantGlobalUsers(Value V, Constant Init) {
        if(Init==null) return false;
        boolean Changed = false;
        ArrayList<User> WorkList=V.getUsers();
        while (!WorkList.isEmpty()) {
            User UV = WorkList.remove(WorkList.size()-1);
            if (UV==null)
                continue;
            if (UV instanceof Instructions.LoadInst) {
                Instructions.LoadInst LI=(Instructions.LoadInst)UV;
                LI.replaceAllUsesWith(Init);
                LI.remove();
                Changed = true;
            }else if (UV instanceof Instructions.StoreInst) {
                // Store must be unreachable or storing Init into the global.
                UV.remove();
                Changed = true;
            }else if (UV instanceof Instructions.GetElementPtrInst) {
                Instructions.GetElementPtrInst gep=(Instructions.GetElementPtrInst)UV;
                Constant Init_tmp=gep.getConstantValue();
                Changed |= cleanupConstantGlobalUsers(gep, Init_tmp );

                if (UV.getUseList().isEmpty()) {
                    UV.remove();
                    Changed = true;
                }
            }
            //TODO: UV instance of gepInst
        }
        return Changed;
    }

    @Override
    public String getName() {
        return "Global Variable Optimization";
    }
}
