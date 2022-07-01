package backend.pass;

import backend.machineCode.Instruction.Arithmetic;
import backend.machineCode.Instruction.LoadOrStore;
import backend.machineCode.Instruction.Push;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.Operand.Adress;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCRegister;
import backend.machineCode.Operand.VirtualRegister;
import ir.DerivedTypes;
import ir.Function;
import ir.Instruction;
import util.SymbolTable;


import java.util.ArrayList;
import java.util.HashMap;

import static backend.machineCode.Instruction.Arithmetic.Type.SUB;

public class RegAllocator {
    private ArrayList<MachineFunction> funcList;

    public RegAllocator(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    public void run() {

        for (var func : funcList) {
            if (!func.isDefined()) continue;
            HashMap<VirtualRegister, Integer> vRegHash = new HashMap<>();
            int numOnStack = 0;
            System.out.println(func.getName());
            for (var bb : func.getBbList()) {
                for (var inst : bb.getInstList()) {
                    var dest = inst.getDest();
                    var op1 = inst.getOp1();
                    var op2 = inst.getOp2();

                    if (dest instanceof VirtualRegister && !vRegHash.containsKey(dest)) {
                        vRegHash.put((VirtualRegister) dest, numOnStack);
                        numOnStack++;
                    }
                    if (op1 instanceof VirtualRegister && !vRegHash.containsKey(op1)) {
                        vRegHash.put((VirtualRegister) op1, numOnStack);
                        numOnStack++;
                    }
                    if (op2 instanceof VirtualRegister && !vRegHash.containsKey(op2)) {
                        vRegHash.put((VirtualRegister) op2, numOnStack);
                        numOnStack++;
                    }
                }
            }
            MachineBasicBlock firstBb = func.getBbList().getFirst().getVal();
            firstBb.getInstList().insertAtHead(new Arithmetic(firstBb, SUB, new MCRegister(MCRegister.RegName.SP),
                    new ImmediateNumber(4 * numOnStack)));

            for (var bb : func.getBbList()) {
                for (var inst : bb.getInstList()) {
                    System.out.println(inst.toString());

                    var dest = inst.getDest();
                    var op1 = inst.getOp1();
                    var op2 = inst.getOp2();

                    if (dest instanceof VirtualRegister) {
                        new LoadOrStore(bb, LoadOrStore.Type.STORE, new MCRegister(MCRegister.RegName.r0),
                                new Adress(new MCRegister(MCRegister.RegName.SP), 4 * vRegHash.get((VirtualRegister) dest)))
                                .getInstNode().insertAfter(inst.getInstNode());
                        inst.setDest(new MCRegister(MCRegister.RegName.r0)) ;
                    }
                    if(op1 instanceof VirtualRegister){
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, new MCRegister(MCRegister.RegName.r1),
                                new Adress(new MCRegister(MCRegister.RegName.SP), 4 * vRegHash.get((VirtualRegister) op1)))
                                .getInstNode().insertBefore(inst.getInstNode());
                        inst.setOp1(new MCRegister(MCRegister.RegName.r1)) ;
                    }
                    if(op2 instanceof VirtualRegister){
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, new MCRegister(MCRegister.RegName.r2),
                                new Adress(new MCRegister(MCRegister.RegName.SP), 4 * vRegHash.get((VirtualRegister) op2)))
                                .getInstNode().insertBefore(inst.getInstNode());
                        inst.setOp2(new MCRegister(MCRegister.RegName.r2)) ;
                    }
                }
            }
        }
    }
}
