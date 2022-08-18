package pass.passes;

import java.util.ArrayList;
import java.util.HashSet;

import ir.Constant;
import ir.Function;
import ir.GlobalVariable;
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
            if (name.equals("putch") || name.equals("putarray") || name.equals("putint") || name.equals("putfloat")) {
                outputFuncs.add(func);
            }
        }
        while (removeUselessRet(M) || removeUselessArg(M) || removeUselessGlobalVariable(M)) {
            deadCodeEmit.runOnModule(M);
        }
    }

    /**
     * 对于Function Arguments
     * 鉴于死代码删除时已经把过程内无用代码删去了，那么我们认为一个参数如果它的uselist为空，就说明该参数是无用参数
     * 对于这种无用参数，我们将全局对该func的call指令对应的arg位置换为NullValue
     * 
     * @param M module
     * @return 是否做了remove操作
     */
    private boolean removeUselessArg(Module M) {
        boolean doneRemove = false;

        return doneRemove;
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
        for (GlobalVariable gv : M.getGlobalVariables()) {

        }
        return doneRemove;
    }
}
