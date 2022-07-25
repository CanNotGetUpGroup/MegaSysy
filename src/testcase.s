	.data
	.text
	.align	2
	.arch armv7ve
	.syntax unified
	.arm
	.fpu vfpv4
	.global	main
	.type	main, %function
main:
.main1:
	Push { LR }
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #32
	@ %0 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #8
	STR r4, [ r11 , #-20 ]
	@ %1 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #12
	STR r4, [ r11 , #-24 ]
	@ %2 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #16
	STR r4, [ r11 , #-28 ]
	@ store i32 5, i32* %2
	mov	r4, #5
	STR r4, [ r11 , #-32 ]
	LDR r5, [ r11 , #-32 ]
	LDR r7, [ r11 , #-28 ]
	STR r5, [ r7 ]
	@ store i32 4, i32* %1
	mov	r4, #4
	STR r4, [ r11 , #-36 ]
	LDR r5, [ r11 , #-36 ]
	LDR r7, [ r11 , #-24 ]
	STR r5, [ r7 ]
	@ %3 = load i32, i32* %2
	LDR r7, [ r11 , #-28 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-40 ]
	@ %4 = load i32, i32* %1
	LDR r7, [ r11 , #-24 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-44 ]
	@ %5 = srem i32 %3, %4
	LDR r6, [ r11 , #-40 ]
	mov	r0, r6
	LDR r6, [ r11 , #-44 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-48 ]
	@ store i32 %5, i32* %0
	LDR r5, [ r11 , #-48 ]
	LDR r7, [ r11 , #-20 ]
	STR r5, [ r7 ]
	@ ret i32 0
	mov	r0, #0
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

	.size	main, .-main

