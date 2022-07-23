.data
.text
	.align	2
	.arch armv7ve
	.syntax unified
	.arm
	.fpu vfp
	.global	ifElseIf
	.type	ifElseIf	%function
ifElseIf:
.ifElseIf1:
Push { LR }
Push { LR }
Push { r11 }
ADD	r11, SP, #4
SUB	SP, SP, #72
@ %0 = alloca i32
SUB	SP, SP, #4
SUB	r4, r11, #8
STR r4, [ r11 , #-16 ]
@ %1 = alloca i32
SUB	SP, SP, #4
SUB	r4, r11, #12
STR r4, [ r11 , #-20 ]
@ store i32 5, i32* %1
mov	r4, #5
STR r4, [ r11 , #-24 ]
LDR r5, [ r11 , #-24 ]
LDR r7, [ r11 , #-20 ]
STR r5, [ r7 ]
@ store i32 10, i32* %0
mov	r4, #10
STR r4, [ r11 , #-28 ]
LDR r5, [ r11 , #-28 ]
LDR r7, [ r11 , #-16 ]
STR r5, [ r7 ]
@ %2 = load i32, i32* %1
LDR r7, [ r11 , #-20 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-32 ]
@ %3= icmp eq i32 %2, 6
@ br i1 %3, label %7, label %4
LDR r5, [ r11 , #-32 ]
cmp r5 , #6
BEQ	.ifElseIf3
B	.ifElseIf2

.ifElseIf2:
@ %5 = load i32, i32* %0
LDR r7, [ r11 , #-16 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-36 ]
@ %6= icmp eq i32 %5, 11
@ br i1 %6, label %7, label %9
LDR r5, [ r11 , #-36 ]
cmp r5 , #11
BEQ	.ifElseIf3
B	.ifElseIf4

.ifElseIf3:
@ %8 = load i32, i32* %1
LDR r7, [ r11 , #-20 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-40 ]
@ ret i32 %8
LDR r6, [ r11 , #-40 ]
mov	r0, r6
SUB	SP, r11, #4
Pop { r11 }
BX	LR

.ifElseIf4:
@ %10 = load i32, i32* %0
LDR r7, [ r11 , #-16 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-44 ]
@ %11= icmp eq i32 %10, 10
@ br i1 %11, label %12, label %16
LDR r5, [ r11 , #-44 ]
cmp r5 , #10
BEQ	.ifElseIf5
B	.ifElseIf7

.ifElseIf5:
@ %13 = load i32, i32* %1
LDR r7, [ r11 , #-20 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-48 ]
@ %14= icmp eq i32 %13, 1
@ br i1 %14, label %15, label %16
LDR r5, [ r11 , #-48 ]
cmp r5 , #1
BEQ	.ifElseIf6
B	.ifElseIf7

.ifElseIf6:
@ store i32 25, i32* %1
mov	r4, #25
STR r4, [ r11 , #-52 ]
LDR r5, [ r11 , #-52 ]
LDR r7, [ r11 , #-20 ]
STR r5, [ r7 ]
@ br label %29
B	.ifElseIf12

.ifElseIf7:
@ %17 = load i32, i32* %0
LDR r7, [ r11 , #-16 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-56 ]
@ %18= icmp eq i32 %17, 10
@ br i1 %18, label %19, label %25
LDR r5, [ r11 , #-56 ]
cmp r5 , #10
BEQ	.ifElseIf8
B	.ifElseIf10

.ifElseIf8:
@ %20 = load i32, i32* %1
LDR r7, [ r11 , #-20 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-60 ]
@ %21= icmp eq i32 %20, -5
@ br i1 %21, label %22, label %25
LDR r5, [ r11 , #-60 ]
cmp r5 , #-5
BEQ	.ifElseIf9
B	.ifElseIf10

.ifElseIf9:
@ %23 = load i32, i32* %1
LDR r7, [ r11 , #-20 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-64 ]
@ %24 = add i32 %23, 15
LDR r5, [ r11 , #-64 ]
ADD	r4, r5, #15
STR r4, [ r11 , #-68 ]
@ store i32 %24, i32* %1
LDR r5, [ r11 , #-68 ]
LDR r7, [ r11 , #-20 ]
STR r5, [ r7 ]
@ br label %28
B	.ifElseIf11

.ifElseIf10:
@ %26 = load i32, i32* %1
LDR r7, [ r11 , #-20 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-72 ]
@ %27 = sub i32 0, %26
mov	r4, #0
STR r4, [ r11 , #-76 ]
LDR r5, [ r11 , #-76 ]
LDR r6, [ r11 , #-72 ]
SUB	r4, r5, r6
STR r4, [ r11 , #-80 ]
@ store i32 %27, i32* %1
LDR r5, [ r11 , #-80 ]
LDR r7, [ r11 , #-20 ]
STR r5, [ r7 ]
@ br label %28
B	.ifElseIf11

.ifElseIf11:
@ br label %29
B	.ifElseIf12

.ifElseIf12:
@ br label %30
B	.ifElseIf13

.ifElseIf13:
@ %31 = load i32, i32* %1
LDR r7, [ r11 , #-20 ]
LDR r4, [ r7 ]
STR r4, [ r11 , #-84 ]
@ ret i32 %31
LDR r6, [ r11 , #-84 ]
mov	r0, r6
SUB	SP, r11, #4
Pop { r11 }
BX	LR

.size	ifElseIf, .-ifElseIf
	.align	2
	.arch armv7ve
	.syntax unified
	.arm
	.fpu vfp
	.global	main
	.type	main	%function
main:
.main14:
Push { LR }
Push { r11 }
ADD	r11, SP, #4
SUB	SP, SP, #8
@ %0 = call i32 @ifElseIf()
BLX	ifElseIf
mov	r4, r0
STR r4, [ r11 , #-8 ]
@ call void @putint(i32 %0)
LDR r6, [ r11 , #-8 ]
mov	r0, r6
BLX	putint
mov	r4, r0
STR r4, [ r11 , #-12 ]
@ ret i32 0
mov	r0, #0
SUB	SP, r11, #4
Pop { r11 }
Pop { PC }

.size	main, .-main

