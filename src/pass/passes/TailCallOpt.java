package pass.passes;

import ir.Function;
import ir.Type;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;
import pass.FunctionPass;
import util.MyIRBuilder;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * a trivial tail call optimization
 */
public class TailCallOpt extends FunctionPass {

    @Override
    public String getName() {
        return "TailCallOpt";
    }

    @Override
    public void runOnFunction(Function F) {
        MyIRBuilder builder= MyIRBuilder.getInstance();
        if(F.getName().equals("main")) {
            return;
        }
//         暂时只处理返回值为Int的递归函数
        if(F.getType().getReturnType() != Type.getInt32Ty()){
            return;
        }
        boolean isTailCallFunc = true;
        int outer = 0;
        int tailcall = 0;
        for(var preBB : F.getReturnBlock().getPredecessors()){
            if(preBB.getTerminator().getInstNode().getPrev()!=null){
                var inst = preBB.getTerminator().getInstNode().getPrev().getVal();
                // retBB前驱的teminator前的指令必须是storeInst
                if(!(inst instanceof Instructions.StoreInst)){
                    isTailCallFunc = false;
                    break;
                }
                if(inst.getInstNode().getPrev()==null){
                    isTailCallFunc = false;
                    break;
                }
                var instpre = inst.getInstNode().getPrev().getVal();
                if(!(instpre instanceof Instructions.CallInst)){
                    if(instpre instanceof Instructions.LoadInst){
                        outer++;
                    }
                    if(outer>1){
                        isTailCallFunc = false;
                        break;
                    }
                }else{
                    tailcall++;
                }
            } else {
                isTailCallFunc = false;
                break;
            }
        }
        int innerCall = 0;
        for(var use : F.getUseList()){
            var user = use.getU();
            if(user instanceof Instructions.CallInst){
                var call = (Instructions.CallInst)user;
                if(call.getParent().getParent()==F){
                    innerCall++;
                }
            }
        }
        if(innerCall!=tailcall){
            isTailCallFunc = false;
        }
        if(innerCall == 0){
            isTailCallFunc = false;
        }
        if(isTailCallFunc) {
            ArrayList<Instructions.AllocaInst> allocList = new ArrayList<>();
            for(var arg : F.getArguments()){
                var user = arg.getUseList().getFirst().getU();
                assert(user instanceof Instructions.StoreInst);
                var alloca = ((Instructions.StoreInst)user).getOperand(1);
                allocList.add((Instructions.AllocaInst)alloca);
            }
            ArrayList<Instructions.CallInst> del = new ArrayList<>();
            for(var use : F.getUseList()){
                var user = use.getU();
                if(user instanceof Instructions.CallInst){
                    var call = (Instructions.CallInst)user;
                    if(call.getParent().getParent()==F){
                        // 修改alloc
                        var argList = call.getArgs();
                        builder.setInsertPoint(call);
                        for(int i = 0; i< argList.size(); i++){
                            builder.createStore(argList.get(i),allocList.get(i));
                        }
                        builder.setInsertPoint(call.getParent().getTerminator());
                        assert F.getEntryBB().getSuccessorsNum() == 1;
                        builder.createBr(F.getEntryBB().getSuccessor(0));
                        call.getParent().getTerminator().remove();
                        call.getInstNode().getNext().getVal().remove();
                        del.add(call);
                    }
                }
            }
            for(var delcall: del){
                delcall.remove();
            }
            System.out.println("[DEBUG] " + F);
        }
    }
}
