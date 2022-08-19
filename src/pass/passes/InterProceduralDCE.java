package pass.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import ir.Argument;
import ir.Constant;
import ir.Function;
// import ir.GlobalVariable;
import ir.Module;
import ir.instructions.Instructions.CallInst;
import ir.instructions.Instructions.ReturnInst;
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
    private HashSet<Function> optedRetFuncs = new HashSet<Function>();
    private HashSet<Function> outputFuncs = new HashSet<Function>(); // 我们认为只有通过use关系向下搜索能够搜索到putch,putarray,putint,br,ret,call的全局变量是"有用"的全局变量

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
        }
        do {
            removeUselessArg(M);
            deadCodeEmit.runOnModule(M);
        } while (removeUselessRet(M) || removeUselessGlobalVariable(M));
    }

    /**
     * 对于Function Arguments
     * 默认它是活跃的，不能删去，经过算法判定为死参数才能删除
     * 
     * @trival 鉴于死代码删除时已经把过程内无用代码删去了，那么我们认为一个参数如果它的uselist为空，就说明该参数是无用参数
     * @advanced This pass deletes dead arguments from internal functions. Dead
     *           argument elimination removes arguments which are directly dead, as
     *           well as arguments only passed into function calls as dead arguments
     *           of other functions. This pass also deletes dead arguments in a
     *           similar way. -- llvm
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
     * @return 是否做了remove操作
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
                        isUseful = true;
                        break;
                    }
                }
                if (isUseful) {
                    continue;
                }
                doneRemove = true;
                optedRetFuncs.add(func);
                for (var bb : func.getBbList()) {
                    for (var inst : bb.getInstList()) {
                        if (inst instanceof ReturnInst) {
                            if (!inst.getOperandList().isEmpty()) {
                                Constant ret = Constant.getNullValue(((ReturnInst) inst).getOperand(0).getType());
                                inst.removeAllOperand();
                                if (ret != null) {
                                    inst.addOperand(ret);
                                }
                            }
                        }
                    }
                }
            } else {
                doneRemove = true;
                optedRetFuncs.add(func);
                for (var bb : func.getBbList()) {
                    for (var inst : bb.getInstList()) {
                        if (inst instanceof ReturnInst) {
                            if (!inst.getOperandList().isEmpty()) {
                                Constant ret = Constant.getNullValue(((ReturnInst) inst).getOperand(0).getType());
                                inst.removeAllOperand();
                                if (ret != null) {
                                    inst.addOperand(ret);
                                }
                            }
                        }
                    }
                }
            }
        }
        return doneRemove;
    }

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
        // for (GlobalVariable gv : M.getGlobalVariables()) {

        // }
        return doneRemove;
    }
}
