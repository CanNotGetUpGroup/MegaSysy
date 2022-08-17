package pass.passes;

import ir.Module;
import pass.ModulePass;

/**
 * 过程间死代码删除
 * <p>
 * 过程间死代码删除(Interprocedural dead code elimination） 把关注点放在过程间的死代码中。
 * </p>
 * <p>
 * 目前本pass考虑了两种过程间死代码
 * </p>
 * <ul>
 * <li>函数的ret指令返回值</li>
 * <li>全局变量</li>
 * </ul>
 * <p>
 * 对于函数的ret指令返回值，死代码的典例如下：
 * </p>
 * 
 * <pre>
 * int swap(int[] arr, int i, int j) {
 *     int tmp = arr[i];
 *     arr[i] = arr[j];
 *     arr[j] = tmp;
 *     return useless();
 * }
 * 
 * int main() {
 *     int a[] = { 1, 2, 3 };
 *     swap(a, 1, 2);
 * }
 * </pre>
 * <p>
 * 对于全局变量，死代码的典例如下：
 * </p>
 * 
 * <pre>
 * int a = 1;
 * int b = 2;
 * 
 * void uselessCal() {
 *     Cal(a, b);
 * }
 * 
 * int main() {
 *     uselessCal();
 *     return 0;
 * }
 * </pre>
 */
public class InterproceduralDCE extends ModulePass {

    public InterproceduralDCE() {
        super();
    }

    @Override
    public String getName() {
        return "InterproceduralDCE";
    }

    @Override
    public void runOnModule(Module M) {
        for (var func : M.getFuncList()) {
            if (!func.isDefined()) {
                continue;
            }

        }
    }

}
