package pass.passes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import ir.BasicBlock;
import ir.Constant;
import ir.Function;
import ir.Instruction;
import ir.Module;
import ir.Value;
import ir.Constants.ConstantFP;
import ir.Constants.ConstantInt;
import ir.DerivedTypes.ArrayType;
import ir.instructions.Instructions.*;
import pass.FunctionPass;

public class LocalArrayPromote extends FunctionPass {

    private Constant[] promoteResult; // 存储所有store对数组的操作结果，用于最终提升时创建GV
    private ArrayList<StoreInst> stores = new ArrayList<>();// 存储alloc对应的store指令，在提升后需要删去这些指令
    private int NumOfStoresBeforeLoad;// 记录在bfs时，有多少store指令是在load之前的，用于判断是否alloc对应的所有的load指令都在store指令之后
    private HashSet<BasicBlock> visitmap = new HashSet<>();// 层序遍历使用，记录已经访问过的block
    private Queue<BasicBlock> bbqueue = new LinkedList<>();// 层序遍历使用，存储待访问的block

    private boolean canPromote; // alloc对应的局部数组是否可以提升的标志

    public LocalArrayPromote() {
        super();
    }

    @Override
    public void runOnModule(Module M) {
        super.runOnModule(M);
    }

    @Override
    public String getName() {
        return "Local Array Promote";
    }

    @Override
    public void runOnFunction(Function F) {
        for (BasicBlock BB : F.getBbList()) {
            for (Instruction I : BB.getInstList()) {
                if (I instanceof AllocaInst) {
                    AllocaInst alloca = (AllocaInst) I;
                    if (alloca.getAllocatedType().isArrayTy()) {
                        ArrayType arrayType = (ArrayType) alloca.getAllocatedType();
                        // 初始化数组,存储将要被提升的局部数组的value
                        promoteResult = new Constant[arrayType.size()];
                        if (arrayType.isIntArray()) {
                            for (int i = 0; i < arrayType.size(); i++) {
                                promoteResult[i] = ConstantInt.const_0();
                            }
                        } else if (arrayType.isFloatArray()) {
                            for (int i = 0; i < arrayType.size(); i++) {
                                promoteResult[i] = ConstantFP.const_0();
                            }
                        }
                        travalStoresOfAlloca(alloca);
                        if (canPromote) {
                            IsAllStoreBeforeLoad(alloca);
                            if (canPromote) {
                                promote(alloca);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 遍历store指令
     * 
     * @param alloca
     * @return
     */
    private boolean travalStoresOfAlloca(AllocaInst alloca) {
        canPromote = true;
        stores.clear();
        for (BasicBlock bb : alloca.getParent().getParent().getBbList()) {
            for (Instruction inst : bb.getInstList()) {
                if (inst instanceof StoreInst) {
                    // 如果是store指令，则检测它是否是存到了alloca对应的地址
                    StoreInst storeInst = (StoreInst) inst;
                    // 获取store存储的地址，这里因为是做局部数组提升，所以只检测GetElementPtrInst
                    Value storeAddr = storeInst.getOperand(1);
                    if (storeAddr instanceof GetElementPtrInst) {
                        GetElementPtrInst gep = (GetElementPtrInst) storeAddr;
                        // 获取gep取元素的地址
                        // 如果基地址的类型是alloca的类型，则直接判断是否是同一个alloca
                        // 如果基地址的类型是getelementptr的类型，则需要递归判断是否是同一个alloca
                        // 例子如下
                        // %3 = alloca [31 x [2 x i32]] ; i32 x[bitcount+1][2]={}
                        // %5= getelementptr [31 x i32],[31 x i32]* %3 , i32 0, i32 0
                        // %6= getelementptr i32,i32* %5 , i32 1
                        Value gepAddr = gep.getOperand(0);
                        while (gepAddr instanceof GetElementPtrInst) {
                            GetElementPtrInst preGep = (GetElementPtrInst) gepAddr;
                            gepAddr = preGep.getOperand(0);
                        }
                        if (gepAddr instanceof AllocaInst) {
                            if (gepAddr.equals(alloca)) {
                                // 如果store的位置和值任意一个不是常量，则不能提升
                                if (!(storeInst.getOperand(0) instanceof Constant)) { // 如果存储的值不是常量
                                    canPromote = false;
                                }
                                for (int i = 1; i < gep.getNumOperands(); i++) { // 如果存储的位置不是常量
                                    if (!(gep.getOperand(i) instanceof Constant)) {
                                        canPromote = false;
                                    }
                                }
                                if (canPromote) {
                                    if (gep.getNumOperands() == 3) {
                                        int idx = ((ConstantInt) gep.getOperand(2)).getVal();
                                        promoteResult[idx] = (Constant) storeInst.getOperand(0);
                                        stores.add(storeInst);
                                    } else if (gep.getNumOperands() == 2) {
                                        int idx = ((ConstantInt) gep.getOperand(1)).getVal();
                                        promoteResult[idx] = (Constant) storeInst.getOperand(0);
                                        stores.add(storeInst);
                                    } else {
                                        canPromote = false;
                                    }
                                }
                            }
                        } else {
                            canPromote = false;
                        }
                    }
                }
            }
        }
        return canPromote;
    }

    private boolean IsAllStoreBeforeLoad(AllocaInst alloca) {
        visitmap.clear();
        bbqueue.clear();
        visitmap.add(alloca.getParent());
        bbqueue.add(alloca.getParent());
        boolean metLoad = false; // 是否遇到load指令
        while (!bbqueue.isEmpty() && !metLoad) {
            BasicBlock bb = bbqueue.poll();
            metLoad = visitBB(bb, alloca);
            for (BasicBlock succ : bb.getSuccessors()) {
                if (!visitmap.contains(succ)) {
                    visitmap.add(succ);
                    bbqueue.add(succ);
                }
            }
        }
        return true;
    }

    private boolean visitBB(BasicBlock bb, AllocaInst alloca) {
        boolean metLoad = false;
        for (var inst : bb.getInstList()) {
            if (inst instanceof LoadInst) {
                LoadInst load = (LoadInst) inst;
                Value loadAddr = load.getOperand(0);
                while (loadAddr instanceof GetElementPtrInst) {
                    GetElementPtrInst gep = (GetElementPtrInst) loadAddr;
                    loadAddr = gep.getOperand(0);
                }
                if (loadAddr instanceof AllocaInst) {
                    if (loadAddr.equals(alloca)) {
                        metLoad = true;
                    }
                }
            } else if (inst instanceof StoreInst) {
                StoreInst store = (StoreInst) inst;
                Value storeAddr = store.getOperand(1);
                while (storeAddr instanceof GetElementPtrInst) {
                    GetElementPtrInst gep = (GetElementPtrInst) storeAddr;
                    storeAddr = gep.getOperand(0);
                }
                if (storeAddr instanceof AllocaInst) {
                    if (storeAddr.equals(alloca)) {
                        count++;
                    }
                }
            } else if (inst instanceof CallInst) {
                CallInst call = (CallInst) inst;
                for (int i = 0; i < call.getArgs().size(); i++) {
                    Value arg = call.getArgs().get(i);
                    if (arg instanceof AllocaInst) {
                        if (arg.equals(alloca)) {
                            if (!call.getName().equals("memset")) {
                                canPromote = false;
                            } else {
                                memset = call;
                            }

                        }
                    } else if (arg instanceof GetElementPtrInst) {
                        GetElementPtrInst gep = (GetElementPtrInst) arg;
                        Value gepAddr = gep.getOperand(0);
                        while (gepAddr instanceof GetElementPtrInst) {
                            GetElementPtrInst preGep = (GetElementPtrInst) gepAddr;
                            gepAddr = preGep.getOperand(0);
                        }
                        if (gepAddr instanceof AllocaInst) {
                            if (gepAddr.equals(alloca)) {
                                if (!call.getName().equals("memset")) {
                                    canPromote = false;
                                } else {
                                    memset = call;
                                }
                            }
                        }
                    }

                }
            }

        }
        return metLoad;
    }

    private Constant packConstArr() {

    }

    private void promote(AllocaInst alloca) {

    }
}
