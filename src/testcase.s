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
	SUB	SP, SP, #8
	@ %0 = add i32 2, 3
	mov	r4, #3
	STR r4, [ r11 , #-8 ]
	LDR r5, [ r11 , #-8 ]
	ADD	r4, r5, #2
	STR r4, [ r11 , #-12 ]
	@ ret i32 %0
	LDR r6, [ r11 , #-12 ]
	mov	r0, r6
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

	.size	main, .-main

