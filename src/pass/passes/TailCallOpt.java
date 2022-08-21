package pass.passes;

import ir.Function;
import ir.Type;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;
import pass.FunctionPass;
import util.MyIRBuilder;

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
        // 暂时只处理无返回值的递归函数
        if(F.getType().getReturnType() != Type.getVoidTy()){
            return;
        }
        boolean isTailCallFunc = true;
        int outer = 0;
        int tailcall = 0;
        for(var preBB : F.getReturnBlock().getPredecessors()){
            if(preBB.getTerminator().getInstNode().getPrev()!=null){
                var inst = preBB.getTerminator().getInstNode().getPrev().getVal();
                if(!(inst instanceof Instructions.CallInst)){
                    if(inst instanceof CmpInst){
                        outer++;
                    }
                    if(outer>1){
                        isTailCallFunc = false;
                    }
                }else{
                    tailcall++;
                }
            }else {
                isTailCallFunc = false;
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
        if(isTailCallFunc) {
            for(var use : F.getUseList()){
                var user = use.getU();
                if(user instanceof Instructions.CallInst){
                    var call = (Instructions.CallInst)user;
                    var argList = call.getArgs();

                    builder.setInsertPoint(call);
                    builder.createBr(F.getEntryBB());
                    call.remove();
                    call.getParent().getTerminator().remove();
                }
            }
            System.out.println("[DEBUG] " + F);
        }
    }
}
