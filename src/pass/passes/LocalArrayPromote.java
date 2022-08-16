package pass.passes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.Collectors;

import ir.BasicBlock;
import ir.Constant;
import ir.Function;
import ir.GlobalVariable;
import ir.Instruction;
import ir.Module;
import ir.Type;
import ir.Value;
import ir.Constants.ConstantArray;
import ir.Constants.ConstantFP;
import ir.Constants.ConstantInt;
import ir.DerivedTypes.ArrayType;
import ir.instructions.Instructions.*;
import pass.ModulePass;

public class LocalArrayPromote extends ModulePass {

    private AllocaInst curArr; // 记录当前正在提升的alloc指令
    private Constant[] promoteResult;// 存储所有store对数组的操作结果，用于最终提升时创建GV
    private ArrayList<StoreInst> stores = new ArrayList<>(); // 存储alloc对应的store指令，在提升后需要删去这些指令
    private int cntStoresBeforeLoad;// 记录在bfs时，有多少store指令是在load之前的，用于判断是否alloc对应的所有的load指令都在store指令之后
    private HashSet<BasicBlock> visitmap = new HashSet<>();// 层序遍历使用，记录已经访问过的block
    private Queue<BasicBlock> bbqueue = new LinkedList<>();// 层序遍历使用，存储待访问的block
    private CallInst memset; // 记录memset函数，在提升后删去此指令
    private boolean otherCallUseAlloca; // 记录是否有memset以外的其他函数调用使用了alloc指令对应的数组，如果有，则不能提升
    private int promoteNum = 0; // 记录提升的局部数组的个数，用于命名
    private Module parent;

    public LocalArrayPromote() {
        super();
    }

    @Override
    public void runOnModule(Module M) {
        parent = M;
        for (Function F : M.getFuncList()) {
            // 非Builtin函数
            if (F.isDefined()) {
                runOnFunction(F);
            }
        }
    }

    @Override
    public String getName() {
        return "Local Array Promote";
    }

    public void runOnFunction(Function F) {
        GVNGCM gvngcm = new GVNGCM(true);
        GlobalVariableOpt gvo = new GlobalVariableOpt();
        boolean promoteArray;
        do {
            promoteArray = false;
            for (BasicBlock BB : F.getBbList()) {
                for (Instruction I : BB.getInstList()) {
                    if (I instanceof AllocaInst) {
                        AllocaInst alloca = (AllocaInst) I;
                        if (alloca.getAllocatedType().isArrayTy()) {
                            init(alloca);
                            if (travalStoresOfAlloca()) {
                                if (analyzeLoadAndCall()) {
                                    promote();
                                    promoteArray = true;
                                    // gvngcm.runOnModule(parent);
                                    // gvo.runOnModule(parent);
                                }
                            }
                        }
                    }
                }
            }
        } while (promoteArray);
    }

    private void init(AllocaInst alloca) {
        curArr = alloca;
        ArrayType arrayType = (ArrayType) curArr.getAllocatedType();
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
        stores.clear();
        cntStoresBeforeLoad = 0;
        visitmap.clear();
        bbqueue.clear();
        memset = null;
        otherCallUseAlloca = false;
    }

    /**
     * 遍历store指令
     *
     * @param
     * @return
     */
    private boolean travalStoresOfAlloca() {
        boolean canPromote = true;
        for (BasicBlock bb : curArr.getParent().getParent().getBbList()) {
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
                            if (gepAddr.equals(curArr)) {
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
                        }
                    }
                }
            }
        }
        return canPromote;
    }

    /**
     * 分析load指令和call指令
     * 如果存在load指令在store之前，则不能提升
     * 如果存在非memset的call指令，则不能提升
     * 
     * @return true:可以提升，false:不能提升
     */
    private boolean analyzeLoadAndCall() {
        visitmap.add(curArr.getParent());
        bbqueue.add(curArr.getParent());
        boolean metLoad = false; // 是否遇到load指令
        while (!bbqueue.isEmpty() && !metLoad) {
            BasicBlock bb = bbqueue.poll();
            metLoad = visitBB(bb);
            for (BasicBlock succ : bb.getSuccessors()) {
                if (!visitmap.contains(succ)) {
                    visitmap.add(succ);
                    bbqueue.add(succ);
                }
            }
        }
        return ((stores.size() == cntStoresBeforeLoad) && !otherCallUseAlloca);
    }

    /**
     * 分析每个基本块,如果遇到load指令，则停止分析
     * 如果遇到非memset的call指令，则认为不能提升
     * 
     * @param bb
     * @return
     */
    private boolean visitBB(BasicBlock bb) {
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
                    if (loadAddr.equals(curArr)) {
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
                    if (storeAddr.equals(curArr)) {
                        cntStoresBeforeLoad++;
                    }
                }
            } else if (inst instanceof CallInst) {
                CallInst call = (CallInst) inst;
                for (int i = 0; i < call.getArgs().size(); i++) {
                    Value arg = call.getArgs().get(i);
                    if (arg instanceof AllocaInst) {
                        if (arg.equals(curArr)) {
                            if (!call.getCalledFunction().getName().equals("memset")) {
                                otherCallUseAlloca = true;
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
                            if (gepAddr.equals(curArr)) {
                                if (!call.getCalledFunction().getName().equals("memset")) {
                                    otherCallUseAlloca = true;
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

    /**
     * 把promoteResult中的一维数组常量包装为alloca对应的数组类型
     *
     */
    private Constant packConstArr() {
        assert curArr.getAllocatedType() instanceof ArrayType;
        ArrayType targetArrayType = (ArrayType) curArr.getAllocatedType();
        Stack<ArrayType> typeStack = new Stack<>();
        Type curType = targetArrayType;
        while (curType instanceof ArrayType) {
            typeStack.push((ArrayType) curType);
            curType = ((ArrayType) curType).getKidType();
        }
        ArrayList<Constant> curElemArrayList = new ArrayList<>(Arrays.asList(promoteResult));
        while (!typeStack.isEmpty()) {
            ArrayType curArrayType = typeStack.pop();
            int curNumElem = curArrayType.getNumElements();
            ArrayList<Constant> newElemArrayList = new ArrayList<>();
            for (int i = 0; i < curElemArrayList.size(); i += curNumElem) {
                ArrayList<Value> tmpElemArrayList = new ArrayList<>();
                for (int j = 0; j < curNumElem; j++) {
                    tmpElemArrayList.add(curElemArrayList.get(i + j));
                }
                newElemArrayList.add(ConstantArray.get(curArrayType, tmpElemArrayList));
            }
            curElemArrayList = newElemArrayList;
        }
        return curElemArrayList.get(0);
    }

    private void promote() {
        Constant arr = packConstArr();
        GlobalVariable gv = GlobalVariable.create("@promote_" + promoteNum, arr.getType(), parent, arr, true);
        promoteNum++;
        for (var store : stores) {
            store.remove();
        }
        if (memset != null) {
            memset.remove();
        }
        curArr.replaceAllUsesWith(gv);
        curArr.remove();
    }
}
