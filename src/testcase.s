	.data
	.text
	.align	2
	.arch armv7ve
	.syntax unified
	.arm
	.fpu vfpv4
	.global	f
	.type	f, %function
f:
.f1:
	Push { LR }
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #76
	vmov.32	s18, s0
	vSTR.32 s18, [ r11 , #-44 ]
	vmov.32	s18, s1
	vSTR.32 s18, [ r11 , #-48 ]
	vmov.32	s18, s2
	vSTR.32 s18, [ r11 , #-52 ]
	vmov.32	s18, s3
	vSTR.32 s18, [ r11 , #-56 ]
	vmov.32	s18, s4
	vSTR.32 s18, [ r11 , #-60 ]
	vmov.32	s18, s5
	vSTR.32 s18, [ r11 , #-64 ]
	vmov.32	s18, s6
	vSTR.32 s18, [ r11 , #-68 ]
	vmov.32	s18, s7
	vSTR.32 s18, [ r11 , #-72 ]
	@ %8 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #8
	STR r4, [ r11 , #-76 ]
	@ %9 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #12
	STR r4, [ r11 , #-80 ]
	@ %10 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #16
	STR r4, [ r11 , #-84 ]
	@ %11 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #20
	STR r4, [ r11 , #-88 ]
	@ %12 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #24
	STR r4, [ r11 , #-92 ]
	@ %13 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #28
	STR r4, [ r11 , #-96 ]
	@ %14 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #32
	STR r4, [ r11 , #-100 ]
	@ %15 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #36
	STR r4, [ r11 , #-104 ]
	@ ret = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #40
	STR r4, [ r11 , #-108 ]
	@ store float %0, float* %15
	vLDR.32 s16, [ r11 , #-44 ]
	LDR r7, [ r11 , #-104 ]
	vSTR.32 s16, [ r7 ]
	@ store float %1, float* %14
	vLDR.32 s16, [ r11 , #-48 ]
	LDR r7, [ r11 , #-100 ]
	vSTR.32 s16, [ r7 ]
	@ store float %2, float* %13
	vLDR.32 s16, [ r11 , #-52 ]
	LDR r7, [ r11 , #-96 ]
	vSTR.32 s16, [ r7 ]
	@ store float %3, float* %12
	vLDR.32 s16, [ r11 , #-56 ]
	LDR r7, [ r11 , #-92 ]
	vSTR.32 s16, [ r7 ]
	@ store float %4, float* %11
	vLDR.32 s16, [ r11 , #-60 ]
	LDR r7, [ r11 , #-88 ]
	vSTR.32 s16, [ r7 ]
	@ store float %5, float* %10
	vLDR.32 s16, [ r11 , #-64 ]
	LDR r7, [ r11 , #-84 ]
	vSTR.32 s16, [ r7 ]
	@ store float %6, float* %9
	vLDR.32 s16, [ r11 , #-68 ]
	LDR r7, [ r11 , #-80 ]
	vSTR.32 s16, [ r7 ]
	@ store float %7, float* %8
	vLDR.32 s16, [ r11 , #-72 ]
	LDR r7, [ r11 , #-76 ]
	vSTR.32 s16, [ r7 ]
	@ br label %ret
	B	.f2

.f2:
	@ %16 = load float, float* ret
	LDR r7, [ r11 , #-108 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-112 ]
	@ ret float %16
	LDR r6, [ r11 , #-112 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-116 ]
	vLDR.32 s17, [ r11 , #-116 ]
	vmov.32	s0, s17
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

	.size	f, .-f
	.align	2
	.arch armv7ve
	.syntax unified
	.arm
	.fpu vfpv4
	.global	main
	.type	main, %function
main:
.main3:
	Push { LR }
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #148
	@ %0 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #8
	STR r4, [ r11 , #-44 ]
	@ %1 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #12
	STR r4, [ r11 , #-48 ]
	@ %2 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #16
	STR r4, [ r11 , #-52 ]
	@ %3 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #20
	STR r4, [ r11 , #-56 ]
	@ %4 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #24
	STR r4, [ r11 , #-60 ]
	@ %5 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #28
	STR r4, [ r11 , #-64 ]
	@ %6 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #32
	STR r4, [ r11 , #-68 ]
	@ %7 = alloca float
	SUB	SP, SP, #4
	SUB	r4, r11, #36
	STR r4, [ r11 , #-72 ]
	@ ret = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #40
	STR r4, [ r11 , #-76 ]
	@ store float 0x40091eb860000000, float* %7
	movw	r4, 62915
	movt	r4, 16456
	STR r4, [ r11 , #-80 ]
	LDR r5, [ r11 , #-80 ]
	LDR r7, [ r11 , #-72 ]
	STR r5, [ r7 ]
	@ store float 0x4009333340000000, float* %6
	movw	r4, 39322
	movt	r4, 16457
	STR r4, [ r11 , #-84 ]
	LDR r5, [ r11 , #-84 ]
	LDR r7, [ r11 , #-68 ]
	STR r5, [ r7 ]
	@ store float 0x400947ae20000000, float* %5
	movw	r4, 15729
	movt	r4, 16458
	STR r4, [ r11 , #-88 ]
	LDR r5, [ r11 , #-88 ]
	LDR r7, [ r11 , #-64 ]
	STR r5, [ r7 ]
	@ store float 0x40095c2900000000, float* %4
	movw	r4, 57672
	movt	r4, 16458
	STR r4, [ r11 , #-92 ]
	LDR r5, [ r11 , #-92 ]
	LDR r7, [ r11 , #-60 ]
	STR r5, [ r7 ]
	@ store float 0x40090d8440000000, float* %3
	movw	r4, 27682
	movt	r4, 16456
	STR r4, [ r11 , #-96 ]
	LDR r5, [ r11 , #-96 ]
	LDR r7, [ r11 , #-56 ]
	STR r5, [ r7 ]
	@ store float 0x400947ae20000000, float* %2
	movw	r4, 15729
	movt	r4, 16458
	STR r4, [ r11 , #-100 ]
	LDR r5, [ r11 , #-100 ]
	LDR r7, [ r11 , #-52 ]
	STR r5, [ r7 ]
	@ store float 0x4009333340000000, float* %1
	movw	r4, 39322
	movt	r4, 16457
	STR r4, [ r11 , #-104 ]
	LDR r5, [ r11 , #-104 ]
	LDR r7, [ r11 , #-48 ]
	STR r5, [ r7 ]
	@ store float 0x40091eb860000000, float* %0
	movw	r4, 62915
	movt	r4, 16456
	STR r4, [ r11 , #-108 ]
	LDR r5, [ r11 , #-108 ]
	LDR r7, [ r11 , #-44 ]
	STR r5, [ r7 ]
	@ %8 = load float, float* %7
	LDR r7, [ r11 , #-72 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-112 ]
	@ %9 = load float, float* %6
	LDR r7, [ r11 , #-68 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-116 ]
	@ %10 = load float, float* %5
	LDR r7, [ r11 , #-64 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-120 ]
	@ %11 = load float, float* %4
	LDR r7, [ r11 , #-60 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-124 ]
	@ %12 = load float, float* %3
	LDR r7, [ r11 , #-56 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-128 ]
	@ %13 = load float, float* %2
	LDR r7, [ r11 , #-52 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-132 ]
	@ %14 = load float, float* %1
	LDR r7, [ r11 , #-48 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-136 ]
	@ %15 = load float, float* %0
	LDR r7, [ r11 , #-44 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-140 ]
	@ %16 = call float @f(float %8,float %9,float %10,float %11,float %12,float %13,float %14,float %15)
	LDR r6, [ r11 , #-140 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-144 ]
	vLDR.32 s17, [ r11 , #-144 ]
	vmov.32	s7, s17
	LDR r6, [ r11 , #-136 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-148 ]
	vLDR.32 s17, [ r11 , #-148 ]
	vmov.32	s6, s17
	LDR r6, [ r11 , #-132 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-152 ]
	vLDR.32 s17, [ r11 , #-152 ]
	vmov.32	s5, s17
	LDR r6, [ r11 , #-128 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-156 ]
	vLDR.32 s17, [ r11 , #-156 ]
	vmov.32	s4, s17
	LDR r6, [ r11 , #-124 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-160 ]
	vLDR.32 s17, [ r11 , #-160 ]
	vmov.32	s3, s17
	LDR r6, [ r11 , #-120 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-164 ]
	vLDR.32 s17, [ r11 , #-164 ]
	vmov.32	s2, s17
	LDR r6, [ r11 , #-116 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-168 ]
	vLDR.32 s17, [ r11 , #-168 ]
	vmov.32	s1, s17
	LDR r6, [ r11 , #-112 ]
	vmov.32	s18, r6
	vSTR.32 s18, [ r11 , #-172 ]
	vLDR.32 s17, [ r11 , #-172 ]
	vmov.32	s0, s17
	BLX	f
	vmov.32	s18, s0
	vSTR.32 s18, [ r11 , #-176 ]
	@ call void @putfloat(float %16)
	vLDR.32 s17, [ r11 , #-176 ]
	vmov.32	s0, s17
	BLX	putfloat
	@ store i32 0, i32* ret
	mov	r4, #0
	STR r4, [ r11 , #-180 ]
	LDR r5, [ r11 , #-180 ]
	LDR r7, [ r11 , #-76 ]
	STR r5, [ r7 ]
	@ br label %ret
	B	.main4

.main4:
	@ %17 = load i32, i32* ret
	LDR r7, [ r11 , #-76 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-184 ]
	@ ret i32 %17
	LDR r6, [ r11 , #-184 ]
	mov	r0, r6
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

	.size	main, .-main

