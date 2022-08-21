package pass.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import ir.*;
import ir.Module;
import ir.instructions.BinaryInstruction;
import ir.instructions.Instructions;
import ir.instructions.Instructions.BranchInst;
import ir.instructions.Instructions.CallInst;
import ir.instructions.Instructions.GetElementPtrInst;
import ir.instructions.Instructions.ReturnInst;
import ir.instructions.Instructions.StoreInst;
import pass.ModulePass;

/**
 * 过程间死代码删除
 * <p>
 * 过程间死代码删除(Interprocedural dead code elimination） 把关注点放在过程间的死代码中。
 * </p>
 * <p>
 * 
 * @apiNote 在死代码删除后使用该pass
 * 
 * @implNote
 *           目前本pass考虑了三种过程间死代码
 *           </p>
 *           <ul>
 *           <li>函数的参数</li>
 *           <li>函数的ret指令返回值</li>
 *           <li>全局变量</li>
 *           </ul>
 * 
 *
 *           <p>
 *           对于函数的参数argument, 死代码的典例如下：
 *           </p>
 * 
 *           <pre>
 *           void swap(int[] arr, int i, int j, int k) {
 *               int tmp = arr[i];
 *               arr[i] = arr[j];
 *               arr[j] = tmp;
 *           }
 * 
 *           int cal(int[] arr, int length) {
 *               for (int i = 0; i < length; i++) {
 *                   int meaninglessValue = meaninglessCal();
 *                   swap(arr, i, i + 1, meaninglessValue);
 *               }
 *           }
 * 
 *           int main() {
 *               a = getarray();
 *               cal(a);
 *               putarray(a);
 *           }
 *           </pre>
 *           <p>
 *           对于函数的ret指令返回值，死代码的典例如下：
 *           </p>
 *
 *           <pre>
 *           int swap(int[] arr, int i, int j) {
 *               int tmp = arr[i];
 *               arr[i] = arr[j];
 *               arr[j] = tmp;
 *               return useless();
 *           }
 *
 *           int main() {
 *               int a[] = { 1, 2, 3 };
 *               swap(a, 1, 2);
 *           }
 *           </pre>
 *           <p>
 *           对于全局变量，死代码的典例如下：
 *           </p>
 *
 *           <pre>
 *           int a = 1;
 *           int b = 2;
 *
 *           void uselessCal() {
 *               Cal(a, b);
 *           }
 *
 *           int main() {
 *               uselessCal();
 *               return 0;
 *           }
 *           </pre>
 */
public class InterProceduralDCE extends ModulePass {

    private DeadCodeEmit deadCodeEmit = new DeadCodeEmit();
    private HashSet<Function> optedRetFuncs = new HashSet<>();

    public InterProceduralDCE() {
        super();
    }

    @Override
    public String getName() {
        return "InterProceduralDCE";
    }

    @Override
    public void runOnModule(Module M) {
        for (var func : M.getFuncList()) {
            var name = func.getName();
            if (name.equals("putch") || name.equals("putarray") || name.equals("putint") || name.equals("putfloat")
                    || name.equals("putfarray")) {
                outputFuncs.add(func);
            }
            if (name.equals("getch") || name.equals("getarray") || name.equals("getint") || name.equals("getfloat")
                    || name.equals("getfarray")) {
                inputFuncs.add(func);
            }
        }
        do {
            removeUselessArg(M);
            deadCodeEmit.runOnModule(M);
        } while (removeUselessRet(M) || removeUselessGlobalVariable(M));
        for (var gv : uselessGvList) {
            gv.remove();
        }
    }

    /**
     * 对于Function Arguments
     * 默认它是活跃的，不能删去，经过算法判定为死参数才能删除
     * 
     * @ trival 鉴于死代码删除时已经把过程内无用代码删去了，那么我们认为一个参数如果它的uselist为空，就说明该参数是无用参数
     * @ advanced This pass deletes dead arguments from internal functions. Dead
     * argument elimination removes arguments which are directly dead, as
     * well as arguments only passed into function calls as dead arguments
     * of other functions. This pass also deletes dead arguments in a
     * similar way. -- llvm
     * 
     * @implNote 这个递归分析给我整麻了，死参数分析还不一定有测试点...
     *           - 遍历每一个func的argument
     *           - 建立一个分析栈,维护在分析此argument时递归分析的其他参数
     *           - 如果已经被判定过生死,跳过,否则进入第三步
     *           - 判断argument的user是否为空,若为空,则加入dead,否则进入第四步
     *           - 判断argumen的user是否存在非call指令,若为是,则加入not dead,否则进入第五步
     *           - 此时argument的user全为call指令,对user遍历
     *           - 找到argument在call指令中args的idx,凭借这个idx找到argment在call_func中的作为什么参数传入
     *           - 递归分析新参数
     *           - 如果这个参数是死参数或者已经在分析栈中,则跳过
     *           - 如果这个参数是活参数,则argument为活参数
     *           - 遍历结束,此时说明argument的call指令全为死参数或者存在循环、递归的参数调用
     *           - 那么此时我们判定该argument为死参数
     * 
     * @param M module
     */
    private void removeUselessArg(Module M) {
        HashSet<Argument> analysisSet = new HashSet<>();
        HashMap<Argument, Boolean> isDeadArg = new HashMap<>();
        for (var func : M.getFuncList()) {
            if (!func.isDefined()) {
                continue;
            }
            for (var arg : func.getArguments()) {
                // arg已经被判定完成
                if (isDeadArg.containsKey(arg)) {
                    continue;
                }
                analysisSet.clear();
                analysisArgUse(arg, analysisSet, isDeadArg);
            }
        }
        for (var func : M.getFuncList()) {
            if (!func.isDefined()) {
                continue;
            }
            for (var arg : func.getArguments()) {
                if (isDeadArg.get(arg)) {
                    removeDeadArg(arg);
                }
            }
        }
    }

    private void analysisArgUse(Argument arg, HashSet<Argument> analysisSet, HashMap<Argument, Boolean> isDeadArg) {
        analysisSet.add(arg);
        if (arg.getUseList().isEmpty()) {
            isDeadArg.put(arg, true);
            return;
        }
        for (var use : arg.getUseList()) {
            var user = use.getU();
            if (user instanceof CallInst) {
                CallInst call = (CallInst) user;
                var callFunc = call.getCalledFunction();
                var name = callFunc.getName();
                if (name.equals("putch") || name.equals("putarray") || name.equals("putint") || name.equals("putfloat")
                        || name.equals("putfarray")) {
                    isDeadArg.put(arg, false);
                    return;
                }
            } else {
                isDeadArg.put(arg, false);
                return;
            }
        }
        // 此时arg的所有use都是call指令，假定此时是Dead的
        // 如果所有的use都是dead arg或者分析到最后没有新arg被分析（参数调用形成了循环依赖或者递归）那就说明它是dead的
        // 如果存在一个use不是dead，那么它就不是dead的
        for (var use : arg.getUseList()) {
            var user = use.getU();
            assert user instanceof CallInst;
            // 找到arg在call指令arglist的哪个位置
            int idx = 0;
            for (var userArg : ((CallInst) user).getArgs()) {
                if (userArg == arg) {
                    break;
                }
                idx++;
            }
            // 分析call对应的func在idx位置的arg是不是dead的
            var func = ((CallInst) user).getCalledFunction();
            var newArg = func.getArguments().get(idx);
            if (!analysisSet.contains(newArg)) {
                if (isDeadArg.containsKey(newArg)) {
                    if (!isDeadArg.get(newArg)) {
                        isDeadArg.put(arg, false);
                        return;
                    }
                } else {
                    analysisArgUse(newArg, analysisSet, isDeadArg);
                    if (!isDeadArg.get(newArg)) {
                        isDeadArg.put(arg, false);
                    }
                }
            }
        }
        isDeadArg.put(arg, true);
    }

    private void removeDeadArg(Argument arg) {
        var func = arg.getParent();
        int idx = arg.getArgNo();
        for (var use : func.getUseList()) {
            var user = use.getU();
            if (user instanceof CallInst) {
                var call = (CallInst) user;
                Constant nullArg = Constant.getNullValue(call.getArgs().get(idx).getType());
                call.CoReplaceOperandByIndex(idx + 1, nullArg);
//                System.out.println("remove dead arg " + arg.getName() + " in func " + func.getName());
            }
        }
    }

    /**
     * 对于ret, 我们分析call指令的useList即可，需要分为递归和非递归两种情况判断
     * 
     * @param M module
     * @return 是否做了remove操作
     */
    private boolean removeUselessRet(Module M) {
        boolean doneRemove = false;
        for (var func : M.getFuncList()) {
            if (!func.isDefined()) {
                continue;
            }
            // main函数的返回值一定有意义，不能替换为0
            if (func.getName().equals("main")) {
                continue;
            }
            if (optedRetFuncs.contains(func)) {
                continue;
            }
            var isUseful = false; // 是否存在有用的ret指令，即ret指令有user
            var recursion = false; // 是否有递归调用
            ArrayList<CallInst> innerCall = new ArrayList<>(); // func内部递归调用自身的call指令列表
            // analyse all call
            for (var use : func.getUseList()) {
                var user = use.getU();
                if (user instanceof CallInst) {
                    var callInst = (CallInst) user;
                    // 如果user在func内，说明是递归调用，暂时不判断isUseful,把这个call指令先记录下来
                    // 如果user不在func内，那么这个call指令一定是在func外部调用的，这也就意味着call指令的UseList不为空的话，func的返回值就是有意义的
                    if (callInst.getParent().getParent() == func) {
                        recursion = true;
                        innerCall.add(callInst);
                    } else if (!user.getUseList().isEmpty()) {
                        isUseful = true;
                        break;
                    }
                }
            }
            if (isUseful) {
                continue;
            }
            if (recursion) {
                for (var call : innerCall) {
                    if (call.getUseList().size() > 1) {
                        isUseful = true;
                        break;
                    } else if (call.getUseList().size() == 1
                            && !(call.getUseList().get(0).getU() instanceof ReturnInst)) {
                        // 再向下搜索一层，应该能处理掉大部分的无效return了，不想写递归了
                        // System.out.println("[DEBUG]: " + call.getCalledFunction().getName());
                        var user = call.getUseList().get(0).getU();
                        if (user instanceof Instructions.PHIInst || user instanceof BinaryInstruction) {
                            for (var use : call.getUseList().get(0).getU().getUseList()) {
                                if (!(use.getU() instanceof ReturnInst)) {
                                    isUseful = true;
                                    break;
                                }
                            }
                        } else {
                            isUseful = true;
                            break;
                        }
                    }
                }
                if (isUseful) {
                    continue;
                }
                doneRemove = true;
                optedRetFuncs.add(func);
                ArrayList<Type> newParamTy = new ArrayList<>(
                        func.getType().getContainedTys().subList(1, func.getType().getParamNum() + 1));
                DerivedTypes.FunctionType newFty = DerivedTypes.FunctionType.get(Type.getVoidTy(), newParamTy);
                func.setType(newFty);
                for (var bb : func.getBbList()) {
                    for (var inst : bb.getInstList()) {
                        if (inst instanceof ReturnInst) {
                            if (!inst.getOperandList().isEmpty()) {
                                inst.removeAllOperand();
                            }
                        }
                    }
                }
                for (var use : func.getUseList()) {
                    var user = use.getU();
                    if (user instanceof CallInst) {
                        var call = (CallInst) user;
                        call.setType(Type.getVoidTy());
                    }
                }
            } else {
                doneRemove = true;
                optedRetFuncs.add(func);
                ArrayList<Type> newParamTy = new ArrayList<>(
                        func.getType().getContainedTys().subList(1, func.getType().getParamNum() + 1));
                DerivedTypes.FunctionType newFty = DerivedTypes.FunctionType.get(Type.getVoidTy(), newParamTy);
                func.setType(newFty);
                for (var bb : func.getBbList()) {
                    for (var inst : bb.getInstList()) {
                        if (inst instanceof ReturnInst) {
                            if (!inst.getOperandList().isEmpty()) {
                                inst.removeAllOperand();
                            }
                        }
                    }
                }
                for (var use : func.getUseList()) {
                    var user = use.getU();
                    if (user instanceof CallInst) {
                        var call = (CallInst) user;
                        call.setType(Type.getVoidTy());
                    }
                }
            }
        }
        return doneRemove;
    }

    private HashSet<Function> outputFuncs = new HashSet<>(); // 我们认为只有通过use关系向下搜索能够搜索到putch,putarray,putint,br,ret,call的全局变量是"有用"的全局变量
    private HashSet<Function> inputFuncs = new HashSet<>();
    private HashSet<Value> relatedValues = new HashSet<>();
    private HashSet<Function> relatedFunc = new HashSet<>();
    private HashSet<Value> inputRelated = new HashSet<>();
    private HashSet<Instruction> relatedInst = new HashSet<>();
    private ArrayList<CallInst> inputFuncCall = new ArrayList<>();
    private ArrayList<GlobalVariable> uselessGvList = new ArrayList<>();

    /**
     * 对于Global variable
     * 我们认为只要它能通过use关系向下搜索能够搜索到putch,putarray,putint,br,ret,call的全局变量是"有用"的全局变量
     * 但上面这种情况会有误判，比如gloabl和return相关联了，但是return值没被用过。类似的还有call指令，global作为参数传入函数，但是这个参数没有用处
     * 因此需要配合前面的removeUselessRet和removeUselessArg来使用
     * 同时我们保留gv的getint、getarray、getfloat、getch
     * 
     * @param M module
     * @return 是否做了remove操作
     */
    private boolean removeUselessGlobalVariable(Module M) {
        boolean doneRemove = false;
        for (GlobalVariable gv : M.getGlobalVariables()) {
            initRelated();
            findRelatedUsers(gv);
            for (var call : inputFuncCall) {
                findInputCallRelated(call);
            }
            if (isUselessGlobalVariable()) {
                doneRemove = removeGV();
            }
        }
        return doneRemove;
    }

    private void initRelated() {
        relatedValues.clear();
        relatedFunc.clear();
        relatedInst.clear();
        inputRelated.clear();
    }

    private void findRelatedUsers(Value value) {
        if (relatedValues.contains(value)) {
            return;
        }
        /*
         * store @a pointer
         * a和pointer有关联
         */
        if (value instanceof StoreInst) {
            if (((StoreInst) value).getOperand(1) instanceof GetElementPtrInst) {
                findRelatedUsers(((StoreInst) value).getOperand(0));
                Value storeAddr = ((StoreInst) value).getOperand(1);
                var gep = (GetElementPtrInst) storeAddr;
                var gepAddr = gep.getOperand(0);
                while (gepAddr instanceof GetElementPtrInst) {
                    GetElementPtrInst preGep = (GetElementPtrInst) gepAddr;
                    gepAddr = preGep.getOperand(0);
                }
                findRelatedUsers(gepAddr);
            } else {
                findRelatedUsers(((StoreInst) value).getOperand(1));
                findRelatedUsers(((StoreInst) value).getOperand(0));
            }
        }
        if (value instanceof ReturnInst || value instanceof CallInst || value instanceof BranchInst) {
            if (value instanceof CallInst) {
                if (!inputFuncs.contains(((CallInst) value).getCalledFunction())) {
                    relatedInst.add((Instruction) value);
                } else {
                    inputFuncCall.add((CallInst) value);
                }
            } else {
                relatedInst.add((Instruction) value);
            }
        }
        if (value instanceof CallInst) {
            relatedFunc.add(((CallInst) value).getCalledFunction());
        }
        relatedValues.add(value);
        value.getUseList().forEach(use -> {
            findRelatedUsers(use.getU());
        });
    }

    private void findInputCallRelated(User user) {
        if (inputRelated.contains(user)) {
            return;
        }
        inputRelated.add(user);
        for (var op : user.getOperandList()) {
            if (op instanceof User) {
                findInputCallRelated((User) op);
            }
        }
    }

    private boolean isUselessGlobalVariable() {
        for (Function f : outputFuncs) {
            if (relatedFunc.contains(f)) {
                return false;
            }
        }
        for (Function f : relatedFunc) {
            if (!inputFuncs.contains(f)) {
                return false;
            }
        }
        if (!relatedInst.isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean removeGV() {
        boolean doneRemove = false;
        for (var value : relatedValues) {
            if (!inputRelated.contains(value)) {
                if (value instanceof Instruction) {
                    if (!(value instanceof CallInst)) {
                        doneRemove = true;
                        Instruction inst = (Instruction) value;
                        inst.remove();
//                        System.out.println("remove gv related: " + value.getName());
                    }
                }
                if (relatedFunc.isEmpty()) {
                    if (value instanceof GlobalVariable) {
                        GlobalVariable gv = (GlobalVariable) value;
                        if (!uselessGvList.contains(gv)) {
                            doneRemove = true;
                            uselessGvList.add(gv);
                        }
                    }
                }
            }
        }
        return doneRemove;
    }
}
