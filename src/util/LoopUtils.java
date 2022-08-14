package util;

import ir.BasicBlock;
import ir.Instruction;
import ir.Loop;
import ir.instructions.BinaryInstruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;

public class LoopUtils {

    private static final MyIRBuilder myIRBuilder = MyIRBuilder.getInstance();

    // return a new instruction but uses the old inst operands
    public static Instruction copyInstruction(Instruction inst) {
        Instruction copy = null;
        var ops = inst.getOperandList();
        if (inst instanceof BinaryInstruction) {
            copy = (Instruction) myIRBuilder.createBinary(inst.getOp(), ops.get(0), ops.get(1));
        }
        return copy;
    }

    public static void addLoopToWorkList(Loop loop, Queue<Loop> WorkList){
        for(Loop sub:loop.getSubLoops()){
            if (sub != null) {
                addLoopToWorkList(sub,WorkList);
            }
        }
        WorkList.add(loop);
    }

    public static void rearrangeBBOrder(Loop loop, HashSet<BasicBlock> Visited) {
        var header = loop.getLoopHeader();
        ArrayList<BasicBlock> tmp = new ArrayList<>();
        Visited.add(header);
        rearrangeDFS(header, loop, tmp, Visited);
        loop.getBbList().clear();
        loop.getBbList().addAll(tmp);
    }

    public static void rearrangeDFS(BasicBlock curr, Loop loop, ArrayList<BasicBlock> tmp, HashSet<BasicBlock> Visited) {
        tmp.add(curr);
        for (var bb : curr.getSuccessors()) {
            if (loop.getBbList().contains(bb)) {
                if (Visited.add(bb)) {
                    rearrangeDFS(bb, loop, tmp, Visited);
                }
            }
        }
    }

}