package backend.pass;

import backend.machineCode.Instruction.*;
import backend.CodeGenManager;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;
import backend.machineCode.Operand.Shifter;
import ir.Module;
import pass.MCPass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import org.antlr.v4.runtime.misc.Pair;

public class PeepHole extends MCPass{
    private static final boolean PEEPHOLE_DEBUG = false;
    private boolean DEL_ENDING_BR = false;

    public PeepHole() {
        super();
    }

    public PeepHole(boolean deleteEndingBr) {
        super();
        this.DEL_ENDING_BR = deleteEndingBr;
    }

    private boolean peepHoleWithoutDataflow(CodeGenManager CGM) {
        boolean done = true;
        for(var f : CGM.getFuncList()) {
            if (!f.isDefined()) continue;
            for(var bb : f.getBbList()) {
                if(bb.getInstList().isEmpty()) continue; // 怎么会有空的bb啊喂

                // 消除bb尾无效跳转
                var lastInst = bb.getInstList().getLast().getVal();
                if (DEL_ENDING_BR && lastInst instanceof Branch && lastInst.getCond() == null) {
                    if ( bb.getBbNode().getNext() != null && ((Branch) lastInst).getDestBB() == bb.getBbNode().getNext().getVal()) {
                        lastInst.delete();
                        done = false;
                        if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE0: del bb ending branch");
                        continue;
                    }
                }

                for(var i : bb.getInstList()) {
                    if(i instanceof Arithmetic) {
                        if((((Arithmetic) i).getType() == Arithmetic.Type.ADD || ((Arithmetic) i).getType() == Arithmetic.Type.SUB)
                                && i.getOp2() instanceof ImmediateNumber
                                && ((ImmediateNumber)i.getOp2()).getValue() == 0) {
                            if(i.getDest().equals(i.getOp1()) || (i.getDest().toString().equals("SP") && i.getOp1().toString().equals("SP"))) {
                                // add/sub a a 0 -> del
                                i.delete();
                                done = false;
                                if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE0: add/sub a a 0 -> del");
                                continue;
                            }else {
                                // add/sub a b 0 -> mov a b
                                new Move(bb, i.getDest(), i.getOp1()).insertBefore(i);
                                i.delete();
                                done = false;
                                if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE0: add/sub a b 0 -> mov a b");
                                continue;
                            }
                        }
                    }
                    // move a a -> del
                    if(i instanceof Move && i.getDest().toString().equals(i.getOp2().toString())) {
                        i.delete();
                        done = false;
                        if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE0: move a a -> del");
                        continue;
                    }

                    // pushpopSimplify
                    if(i instanceof PushOrPop) {                     
                        var type = ((PushOrPop)i).getType();
                        var forFloat = i.isForFloat();
                        var next = i.getNext();
                        if(next instanceof PushOrPop && ((PushOrPop) next).getType().equals(type) && next.isForFloat() == forFloat) continue;

                        ArrayList<PushOrPop> iList =  new ArrayList<PushOrPop>();
                        iList.add((PushOrPop)i);
                        for(var cur=i.getPrev();
                            cur instanceof PushOrPop && ((PushOrPop) cur).getType().equals(type) && cur.isForFloat() == forFloat;
                            cur = cur.getPrev()
                        ) {
                            iList.add((PushOrPop)cur);
                        }
                        if(iList.size() > 1) {
                            var n = new PushOrPopList(bb, type);
                            n.setForFloat(forFloat);
                            if(type.equals(PushOrPop.Type.Push)) iList.forEach(ii -> n.AddReg((Register)ii.getOp2()));
                            else iList.forEach(ii -> n.AddReg((Register)ii.getDest()));
                            n.insertAfter(i);
                            iList.forEach(MachineInstruction::delete);
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE0: pushpopSimplify");
                            continue;
                        }
                    }
                }


                for (var next : bb.getInstList()) {
                    var iNode = next.getInstNode().getPrev();
                    if (iNode == null) continue;
                    var i = iNode.getVal();

                    if(i instanceof LoadOrStore) {
                        // ldr2mov
                        // str a [b x]    str a [b x]
                        // ldr c [b x] -> mov c a
                        if(next instanceof LoadOrStore
                            && ((LoadOrStore)i).getType() == LoadOrStore.Type.STORE
                            && ((LoadOrStore)next).getType() == LoadOrStore.Type.LOAD
                            && ((LoadOrStore)i).getOp2().equals(((LoadOrStore)next).getOp2())
                        ) {
                            new Move(bb, next.getDest(), i.getOp1()).insertBefore(next);
                            next.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE0: ldr2mov");
                        }
                    }

                    if(i instanceof Move) {   
                        if(!i.getDest().toString().equals(i.getOp2().toString())) {
                            // delredmov
                            // move a b
                            // move a c -> move a c    !! move a a 不能删除
                            if(next instanceof Move 
                                    && !i.hasShift() && !next.hasShift()
                                    && i.getCond() == null && next.getCond() == null
                                    && i.getDest().equals(next.getDest())
                                    && !next.getDest().equals(next.getOp2())) {
                                i.delete();
                                done = false;
                                if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE0: delredmov");
                                }
                            // delloopmov
                            // move a b
                            // move b a
                            if(next instanceof Move
                                    && !i.hasShift() && !next.hasShift()
                                    && i.getCond() == null && next.getCond() == null
                                    && i.getDest().equals(next.getOp2())
                                    && i.getOp2().equals(next.getDest())
                                ) {
                                    next.delete();
                                    done = false;
                                    if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE0: delloopmov");
                                }
                        }
                    }

                    // TODO： 这段有bug 过不了tc71
                    // // 	mov	a, 4
                    // //	MUL	a, b, a -> LSL
                    // if ((i instanceof Move || i instanceof LoadImm) && i.getOp2() instanceof ImmediateNumber
                    //         && isPowerOfTwo(((ImmediateNumber) i.getOp2()).getValue())
                    //         && next instanceof Arithmetic && ((Arithmetic) next).getType() == Arithmetic.Type.MUL) {
                    //     var r4 = i.getDest();
                    //     var num = ((ImmediateNumber) i.getOp2()).getValue();
                    //     var other = (Register) next.getOp1();
                    //     if (other.equals(r4))
                    //         other = (Register) next.getOp2();
                    //     if (next.getOp2().equals(r4) || next.getOp1().equals(r4)) {
                    //         new Arithmetic(bb, Arithmetic.Type.LSL, next.getDest(), other, new ImmediateNumber(log2(num)))
                    //                 .insertBefore(i);
                    //         i.delete();
                    //         next.delete();
                    //     }
                    // }
                }

            }
        }

        return done;
    }

    private boolean peepHoleWithDataflow(CodeGenManager CGM) {
        boolean done = true;
        for (var f : CGM.getFuncList()) {
            if (!f.isDefined()) continue;
            var funcLivenessMap = funcLivenessAnalysis(f);
            
            for (var bb : f.getBbList()) {
                if(bb.getInstList().isEmpty()) continue;
                var lastInst = bb.getInstList().getLast().getVal();

                var lastDef = new HashMap<MCOperand,MachineInstruction>();
                var lastUse = new HashMap<MachineInstruction, MachineInstruction>();
                var bbout = funcLivenessMap.get(bb).out;
                blockLiveRange(bb, lastDef, lastUse);
                
                for (var next : bb.getInstList()) {
                    var iNode = next.getInstNode().getPrev();
                    if (iNode == null) continue;
                    var i = iNode.getVal();

                    var iLastUse = lastUse.get(i);
                    var isIDefbbOut = i.getDef().stream().anyMatch(bbout::contains);
                    var isLastDef = i.getDef().stream().anyMatch(def -> (lastDef.get(def) != null && lastDef.get(def).equals(i)));
                    var isNotDefSP = i.getDef().stream().noneMatch(def -> def.toString().equals("SP"));
                    if(isIDefbbOut) iLastUse = i;

                    if(!isIDefbbOut && !isLastDef && isNotDefSP && i.getCond() == null && !i.hasShift() && iLastUse == null && !i.isForFloat()) { // float不杀
                        i.delete();
                        done = false;
                        if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: remove useless instr");
                    }
                }

                for (var next : bb.getInstList()) {
                    var iNode = next.getInstNode().getPrev();
                    if (iNode == null) continue;
                    var i = iNode.getVal();

                    var iLastUse = lastUse.get(i);
                    var isIDefbbOut = i.getDef().stream().anyMatch(bbout::contains);
                    var isLastDef = i.getDef().stream().anyMatch(def -> (lastDef.get(def) != null && lastDef.get(def).equals(i)));
                    if(isIDefbbOut && isLastDef) iLastUse = i;

                    // *********************************************************************************************************************************************
                    // add/sub ldr/str/move
                    // ADD r1, r2, 4
                    // LDR r0, [ r1, 8 ] -> LDR r0, [ r2, 4+8 ]
                    if( !i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null
                        && i instanceof Arithmetic
                        && (((Arithmetic) i).getType() == Arithmetic.Type.ADD || ((Arithmetic) i).getType() == Arithmetic.Type.SUB)
                        && i.getOp2() instanceof ImmediateNumber
                            ) {
                        boolean isAdd = ((Arithmetic) i).getType() == Arithmetic.Type.ADD;
                        int iImm = ((ImmediateNumber)i.getOp2()).getValue();

                        if(next instanceof LoadOrStore && Objects.equals(iLastUse, next)) {
                            boolean isSameDest = ((Address)next.getOp2()).getReg().equals(i.getDest());
                            boolean isImmOffset = ((Address)next.getOp2()).getOffset() instanceof ImmediateNumber;
                            if(isSameDest && isImmOffset) {
                                int nextImm = ((ImmediateNumber)((Address)next.getOp2()).getOffset()).getValue();
                                if((isAdd && nextImm+iImm < 4096) || (!isAdd && nextImm-iImm >= 0)) {
                                    var nImm = isAdd ? new ImmediateNumber(nextImm+iImm) : new ImmediateNumber(nextImm-iImm);
                                    ((Address)next.getOp2()).setReg((Register)i.getOp1());
                                    ((Address)next.getOp2()).setOffset(nImm);
                                    i.delete();
                                    done = false;
                                    if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: add/sub ldr/str");
                                    continue;
                                }
                            }
                        } else if (next instanceof Move) {
                        // add/sub a b 2
                        // move c y          move c y
                        // str c [a, 4] ->   str c [b, 2+4]    !! b != c
                            var nextnextNode = next.getInstNode().getNext();
                            if(nextnextNode != null) {
                                var nextnext = nextnextNode.getVal();
                                if( !nextnext.hasShift()
                                    && nextnext instanceof LoadOrStore
                                    && ((LoadOrStore)nextnext).getType() == LoadOrStore.Type.STORE
                                    && Objects.equals(iLastUse, nextnext)) {
                                    var moveStoreData = next.getDest().equals(nextnext.getOp1());
                                    var isSameDest = ((Address)nextnext.getOp2()).getReg().equals(i.getDest());
                                    var notEditStoreReg = !((Register)i.getOp1()).equals(((Address)nextnext.getOp2()).getReg());
                                    var isImmOffset = ((Address)nextnext.getOp2()).getOffset() instanceof ImmediateNumber;

                                    if(moveStoreData && isSameDest && notEditStoreReg && isImmOffset) {
                                        int nextImm = ((ImmediateNumber)((Address)nextnext.getOp2()).getOffset()).getValue();
                                        if((isAdd && nextImm+iImm < 4096) || (!isAdd && nextImm-iImm >= 0)) {
                                            var nImm = isAdd ? new ImmediateNumber(nextImm+iImm) : new ImmediateNumber(nextImm-iImm);
                                            ((Address)nextnext.getOp2()).setReg((Register)i.getOp1());
                                            ((Address)nextnext.getOp2()).setOffset(nImm);
                                            i.delete();
                                            done = false;
                                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: add/sub move");
                                            continue;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // *********************************************************************************************************************************************
                    // mov a 2
                    // cmp b a  -> cmp b 2
                    if(!i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null &&
                        (i instanceof Move || i instanceof LoadImm) &&
                        i.getOp2() instanceof ImmediateNumber &&
                        Objects.equals(iLastUse, next) &&
                        next instanceof Cmp &&
                        i.getDest().equals(next.getOp2()) &&
                        !next.getOp1().equals(next.getOp2())) { // to avoid cmp a a
                            var imm = ((ImmediateNumber)i.getOp2()).getValue();
                            boolean success = false;
                            if(ImmediateNumber.isLegalImm(imm)) {
                                next.setOp2(new ImmediateNumber(imm));
                                success = true;
                            } else if(ImmediateNumber.isLegalImm(-imm)) {
                                next.setOp2(new ImmediateNumber(-imm));
                                ((Cmp)next).setCmn(true);
                                success = true;
                            }
                            if(success) {
                                i.delete();
                                done = false;
                                if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: mov cmp");
                                continue;
                            }
                    }
                    // *********************************************************************************************************************************************
                    // move replace
                    // mov a b
                    // .......
                    if(!i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null &&
                        i instanceof Move &&
                        !i.isForFloat() &&
                        !(i.getOp2() instanceof ImmediateNumber) &&
                        Objects.equals(iLastUse, next) &&
                        !(next instanceof Branch)) {
                        var src = i.getOp2();
                        var dst = i.getDest();
                        boolean success = false;
                        if(next.getOp1() != null && next.getOp1().equals(dst)) {next.setOp1(src); success = true;}
                        else if(next.getOp2().equals(dst)) {next.setOp2(src); success = true;}
                        else if(next.getOp2() != null && next.getOp2() instanceof Address) {
                            Address addr = (Address)next.getOp2();
                            if(addr.getReg().equals(dst)) {addr.setReg((Register)src); success = true;}
                        }
                        if(success) {
                            i.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: move replace");
                            continue;
                        }
                    }
                    // *********************************************************************************************************************************************
                    // move replace 2
                    // ....... dst ia a            ... dst is b
                    // mov b a             ->
                    if(i instanceof Arithmetic || i instanceof Move || i instanceof LoadImm ||
                        (i instanceof LoadOrStore && ((LoadOrStore) i).getType().equals(LoadOrStore.Type.LOAD))) {
                        if(!i.hasShift() && !next.hasShift() &&
                            i.getCond() == null && next.getCond() == null &&
                            next instanceof Move &&
                            !next.isForFloat() &&
                            Objects.equals(iLastUse, next)) {
                            if(i.getDest() != null && i.getDest().equals(next.getOp2())) {
                                i.setDest(next.getDest());
                                next.delete();
                                done = false;
                                if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: move replace 2");
                                continue;
                            }
                        }
                    }
                     // *********************************************************************************************************************************************
                     // mvnadd2sub
                     // mvn a imm
                     // add/sub z b a  -> sub/add z b imm+1
                    if(!i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null &&
                        i instanceof LoadImm &&
                        i.getOp2() instanceof ImmediateNumber &&
                        next instanceof Arithmetic &&
                        (((Arithmetic)next).getType().equals(Arithmetic.Type.ADD) || ((Arithmetic)next).getType().equals(Arithmetic.Type.SUB)) &&
                        Objects.equals(iLastUse, next)
                    ) {
                        var val = ~(((ImmediateNumber)i.getOp2()).getValue());
                        boolean isMvn = ImmediateNumber.isLegalImm(val);
                        boolean isDestOp2 = i.getDest().equals(next.getOp2());
                        boolean isNewImmLegal = ImmediateNumber.isLegalImm(val+1);
                        if(isMvn && isDestOp2 && isNewImmLegal) {
                            MachineInstruction n;
                            if(((Arithmetic)next).getType().equals(Arithmetic.Type.ADD)){
                                n = new Arithmetic(bb, Arithmetic.Type.SUB, next.getDest(), (Register)next.getOp1(), new ImmediateNumber(val+1));
                                n.insertAfter(next);
                            } else {
                                n = new Arithmetic(bb, Arithmetic.Type.ADD, next.getDest(), (Register)next.getOp1(), new ImmediateNumber(val+1));
                                n.insertAfter(next);
                            }
                            lastUse.put(n, lastUse.get(next)); // Maintenance lastUse
                            if(lastDef.get(n.getDest()) != null && lastDef.get(n.getDest()).equals(next)) lastDef.put(n.getDest(), n);
                            i.delete();
                            next.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: mvnadd2sub");
                            continue;
                        }
                    }

                     // *********************************************************************************************************************************************
                     // addldrstrshift
                     // add a b c shift
                     // ldr z a           -> ldr z b c shift
                     if( i.hasShift() && !next.hasShift() &&
                         i.getCond() == null && next.getCond() == null &&
                         !i.isForFloat() && !next.isForFloat() &&
                         i instanceof Arithmetic && ((Arithmetic)i).getType().equals(Arithmetic.Type.ADD) &&
                         next instanceof LoadOrStore && ((LoadOrStore)next).getType().equals(LoadOrStore.Type.LOAD) &&
                         Objects.equals(iLastUse, next)
                     ) {
                        var addr = (Address)next.getOp2();
                        boolean isDestAddr = i.getDest().equals(addr.getReg()) && addr.hasConstOffset() && ((ImmediateNumber)addr.getOffset()).getValue() == 0;
                        if(isDestAddr) {
                            addr.setReg((Register)i.getOp1());
                            addr.setOffset((Register)i.getOp2());
                            next.setShifter(i.getShifter());
                            i.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: addldrstrshift1");
                            continue;
                        }
                     }
                     // add a b c shift
                     // mov d e                   mov d e
                     // str d [a]             ->  str d [b c shift]       !! b != d
                     var nextnextNode = next.getInstNode().getNext();
                     if(nextnextNode != null) {
                        var nextnext = nextnextNode.getVal();
                        if(i.hasShift() && !next.hasShift() && !nextnext.hasShift() &&
                        i.getCond() == null && next.getCond() == null && nextnext.getCond() == null &&
                        !i.isForFloat() && !next.isForFloat() && !nextnext.isForFloat() &&
                        i instanceof Arithmetic && ((Arithmetic)i).getType().equals(Arithmetic.Type.ADD) &&
                        (next instanceof Move || next instanceof LoadImm) &&
                        nextnext instanceof LoadOrStore && ((LoadOrStore)nextnext).getType().equals(LoadOrStore.Type.STORE) &&
                        Objects.equals(iLastUse, nextnext)
                        ) {
                            var addr = (Address)nextnext.getOp2();
                            boolean isSameData = next.getDest().equals(nextnext.getOp1());
                            boolean isSameDst = i.getDest().equals(addr.getReg()) && addr.hasConstOffset() && ((ImmediateNumber)addr.getOffset()).getValue() == 0;
                            boolean notEditData = !i.getOp1().equals(next.getDest()) && !i.getOp2().equals(next.getDest());
                            if(isSameData && isSameDst && notEditData) {
                                addr.setReg((Register)i.getOp1());
                                addr.setOffset((Register)i.getOp2());
                                nextnext.setShifter(i.getShifter());
                                i.delete();
                                done = false;
                                if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: addldrstrshift2");
                                continue;
                            }
                        }
                     }

                     // *********************************************************************************************************************************************
                     // loadImm replace
                     // mov a imm   @LoadImm
                     // add z b a               -> add z b imm
                    if(!i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null &&
                        i instanceof LoadImm &&
                        i.getOp2() instanceof ImmediateNumber &&
                        next instanceof Arithmetic &&
                        Objects.equals(iLastUse, next)
                    ) {
                        var type = ((Arithmetic)next).getType();
                        var typeOk = type.equals(Arithmetic.Type.ADD) || type.equals(Arithmetic.Type.SUB) || type.equals(Arithmetic.Type.AND) || type.equals(Arithmetic.Type.RSB);
                        var val = ((ImmediateNumber)i.getOp2()).getValue();
                        boolean isMov = ImmediateNumber.isLegalImm(val);
                        boolean isDestOp2 = i.getDest().equals(next.getOp2());
                        if(isMov && isDestOp2 && typeOk) {
                            next.setOp2(new ImmediateNumber(val));
                            i.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: loadImm replace");
                            continue;
                        }
                    }
                    // *********************************************************************************************************************************************
                    // mla/mls
                    // mul a b c 不考虑muls
                    // add/sub d a e -> mla/mls d b c e
                    if(!i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null &&
                        !i.isForFloat() && !next.isForFloat() &&
                        i instanceof Arithmetic && ((Arithmetic)i).getType().equals(Arithmetic.Type.MUL) &&
                        next instanceof Arithmetic && (((Arithmetic)next).getType().equals(Arithmetic.Type.ADD) || ((Arithmetic)next).getType().equals(Arithmetic.Type.SUB)) &&
                        !(next.getOp2() instanceof ImmediateNumber) &&
                        Objects.equals(iLastUse, next)
                    ) {
                        var type = ((Arithmetic)next).getType();
                        var mulRes = i.getDest();
                        boolean success = false;
                        if(type.equals(Arithmetic.Type.ADD)) {
                            // add d a e -> mla d b c e
                            if(next.getOp1().equals(mulRes)) {
                                var mla = new MLAMLS(bb, next.getDest(), i.getOp1(), i.getOp2(), next.getOp2());
                                mla.setCond(next.getCond());
                                lastUse.put(mla, lastUse.get(next)); // Maintenance lastUse
                                if(lastDef.get(mla.getDest()) != null && lastDef.get(mla.getDest()).equals(next)) lastDef.put(mla.getDest(), mla);
                                mla.insertBefore(next);
                                success = true;
                            }
                            // add d e a -> mla d b c e
                            else if (next.getOp2().equals(mulRes)) {
                                var mla = new MLAMLS(bb, next.getDest(), i.getOp1(), i.getOp2(), next.getOp1());
                                mla.setCond(next.getCond());
                                lastUse.put(mla, lastUse.get(next)); // Maintenance lastUse
                                if(lastDef.get(mla.getDest()) != null && lastDef.get(mla.getDest()).equals(next)) lastDef.put(mla.getDest(), mla);
                                mla.insertAfter(next);
                                success = true;
                            }
                        }else if(type.equals(Arithmetic.Type.SUB)) {
                            // sub d e a -> mls d b c e
                            if(next.getOp2().equals(mulRes)) {
                                var mls = new MLAMLS(bb, next.getDest(), i.getOp1(), i.getOp2(), next.getOp1());
                                mls.setCond(next.getCond());
                                mls.setMls(true);
                                lastUse.put(mls, lastUse.get(next)); // Maintenance lastUse
                                if(lastDef.get(mls.getDest()) != null && lastDef.get(mls.getDest()).equals(next)) lastDef.put(mls.getDest(), mls);
                                mls.insertAfter(next);
                                success = true;
                            }
                        }

                        if(success) {
                            i.delete();
                            next.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: mla/mls");
                            continue;
                        }
                    }

                    // *********************************************************************************************************************************************
                    // subsub
                    // sub a b a
                    // sub b b a    ->     mov b a
                    if(!i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null &&
                        !i.isForFloat() && !next.isForFloat() &&
                        i instanceof Arithmetic && ((Arithmetic)i).getType().equals(Arithmetic.Type.SUB) &&
                        next instanceof Arithmetic && ((Arithmetic)next).getType().equals(Arithmetic.Type.SUB) &&
                        Objects.equals(iLastUse, next)
                    ) {
                        boolean isSameA = i.getDest().equals(i.getOp2()) && i.getDest().equals(next.getOp2());
                        boolean isSameB = next.getDest().equals(i.getOp1()) && next.getDest().equals(next.getOp1());
                        if(isSameA && isSameB) {
                            var n = new Move(bb, next.getDest(), i.getDest());
                            lastUse.put(n, lastUse.get(next)); // Maintenance lastUse
                            if(lastDef.get(n.getDest()) != null && lastDef.get(n.getDest()).equals(next)) lastDef.put(n.getDest(), n);
                            n.insertAfter(next);
                            i.delete();
                            next.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: subsub");
                            continue;
                        }
                    }

                    // *********************************************************************************************************************************************
                    // shiftAdd
                    // ASR,LSL,LSR a b #x
                    // add d a c   ->      add d c b [shift] #x
                    if(!i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null &&
                        !i.isForFloat() && !next.isForFloat() &&
                        i instanceof Arithmetic &&
                        (((Arithmetic)i).getType().equals(Arithmetic.Type.ASR) || ((Arithmetic)i).getType().equals(Arithmetic.Type.LSL) || ((Arithmetic)i).getType().equals(Arithmetic.Type.LSR)) &&
                        i.getOp2() instanceof ImmediateNumber &&
                        next instanceof Arithmetic && ((Arithmetic)next).getType().equals(Arithmetic.Type.ADD) &&
                        Objects.equals(iLastUse, next)
                    ) {
                        boolean success = false;
                        var type = switch(((Arithmetic)i).getType()) {
                            case ASR -> Shift.Type.ASR;
                            case LSL -> Shift.Type.LSL;
                            case LSR -> Shift.Type.LSR;
                            default -> null;
                        };
                        if (type == null) throw new RuntimeException("Dead shift type!");
                        if(i.getDest().equals(next.getOp1()) && !(next.getOp2() instanceof ImmediateNumber)) {
                            next.setOp1(next.getOp2());
                            next.setOp2(i.getOp1());
                            next.setShifter(type, ((ImmediateNumber)i.getOp2()).getValue());
                            success = true;
                        } else if(i.getDest().equals(next.getOp2())) {
                            next.setOp2(i.getOp1());
                            next.setShifter(type, ((ImmediateNumber)i.getOp2()).getValue());
                            success = true;
                        }
                        if(success) {
                            i.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: shiftAdd");
                            continue;
                        }
                    }

                    // *********************************************************************************************************************************************
                    // shiftSub
                    // ASR,LSL,LSR a b #x
                    // sub d c a   ->      dub d c b [shift] #x
                    if(!i.hasShift() && !next.hasShift() &&
                        i.getCond() == null && next.getCond() == null &&
                        !i.isForFloat() && !next.isForFloat() &&
                        i instanceof Arithmetic &&
                        (((Arithmetic)i).getType().equals(Arithmetic.Type.ASR) || ((Arithmetic)i).getType().equals(Arithmetic.Type.LSL) || ((Arithmetic)i).getType().equals(Arithmetic.Type.LSR)) &&
                        i.getOp2() instanceof ImmediateNumber &&
                        next instanceof Arithmetic && ((Arithmetic)next).getType().equals(Arithmetic.Type.SUB) &&
                        Objects.equals(iLastUse, next)
                    ) {
                        var type = switch(((Arithmetic)i).getType()) {
                            case ASR -> Shift.Type.ASR;
                            case LSL -> Shift.Type.LSL;
                            case LSR -> Shift.Type.LSR;
                            default -> null;
                        };
                        if (type == null) throw new RuntimeException("Dead shift type!");
                        if(i.getDest().equals(next.getOp2())) {
                            next.setOp2(i.getOp1());
                            next.setShifter(type, ((ImmediateNumber)i.getOp2()).getValue());
                            i.delete();
                            done = false;
                            if(PEEPHOLE_DEBUG) System.out.println("PEEPHOLE1: shiftSub");
                            continue;
                        }
                    }
                    // *********************************************************************************************************************************************
                }
            }
        }

        return done;
    }

    private static class BlockLiveness {
        private final HashSet<MCOperand> use = new HashSet<>();
        private final HashSet<MCOperand> def = new HashSet<>();
        private HashSet<MCOperand> in = new HashSet<>();
        private HashSet<MCOperand> out = new HashSet<>();

        BlockLiveness(MachineBasicBlock block) {
        }
    }

    private HashMap<MachineBasicBlock, BlockLiveness> funcLivenessAnalysis(MachineFunction func) {
        var funcLivenessMap = new HashMap<MachineBasicBlock, BlockLiveness>();
        for(var bb : func.getBbList()) {
            var bbLiveness = new BlockLiveness(bb);
            funcLivenessMap.put(bb, bbLiveness);

            for(var i : bb.getInstList()) {
                for(var use : i.getUse()) {
                    if(!bbLiveness.def.contains(use)) bbLiveness.use.add(use);
                }
                for(var def : i.getDef()) {
                    if(!bbLiveness.use.contains(def)) bbLiveness.def.add(def);
                }
            }
            bbLiveness.in.addAll(bbLiveness.use);
        }

        boolean done = false;
        while(!done) {
            done = true;
            for(var bb : func.getBbList()) {
                var bbLiveness = funcLivenessMap.get(bb);
                var newOut = new HashSet<MCOperand>();

                for(var s : bb.getSuccessors()) {
                    newOut.addAll(funcLivenessMap.get(s).in);
                }

                if(!newOut.equals(bbLiveness.out)) {
                    done = false;
                    bbLiveness.out = newOut;
                    bbLiveness.in = new HashSet<>(bbLiveness.use);
                    for(var out : bbLiveness.out) {
                        if(!bbLiveness.def.contains(out)) bbLiveness.in.add(out);
                    }
                }
            }
        }

        return funcLivenessMap;
    }

    private void blockLiveRange(MachineBasicBlock bb, HashMap<MCOperand,MachineInstruction> lastDef, HashMap<MachineInstruction, MachineInstruction> lastUse) {
        lastDef.clear();
        lastUse.clear();

        for(var i : bb.getInstList()) {
            for(var use : i.getUse()) {
                if(lastDef.containsKey(use)) {
                    lastUse.put(lastDef.get(use), i);
                }
                    
            }
            if(i.getCond() == null) {
                for(var def : i.getDef()) {
                    lastDef.put(def, i);
                }
            }
            
            if(i instanceof Branch ||
                    (i instanceof LoadOrStore && ((LoadOrStore)i).getType() == LoadOrStore.Type.STORE) ||
                    i instanceof PushOrPop ||
                    i instanceof PushOrPopList ||
                    i instanceof Comment ||
                    i instanceof Cmp ||
                    i.getCond() != null
                    ) {  // 这里的SideEffect比较激进 可能有问题？ 818 加了个cmp
                lastUse.put(i, i);
            } else {
                lastUse.put(i, null);
            }
        }
    }

    @Override
    public void runOnCodeGen(CodeGenManager CGM) {
        boolean done = false;
        while(!done) {
            // System.out.println("PeepHole");
            // done = peepHoleWithDataflow(CGM);
            done = peepHoleWithoutDataflow(CGM) & peepHoleWithDataflow(CGM);
        }
    }

    @Override
    public void runOnModule(Module M) {
        throw new RuntimeException("runOnModule for a MCPass");
    }

    public static boolean isPowerOfTwo(int n) {
        if (n < 0) n = -n;
        return n > 0 && (n & (n - 1)) == 0;
    }

    public static int log2(int N) {

        // calculate log2 N indirectly
        // using log() method
        int result = (int) (Math.log(N) / Math.log(2));

        return result;
    }

    @Override
    public String getName() {
        return "PeepHole";
    }

}
