	.data
	.global	seed
	.align	2
	.type	seed, %object
	.size	seed, 12
seed:
	.word	19971231
	.word	19981013
	.word	1000000007
	.global	staticvalue
	.align	2
	.type	staticvalue, %object
	.size	staticvalue, 4
staticvalue:
	.word	0
	.global	a
	.align	2
	.type	a, %object
	.size	a, 40000
a:
	.space	40000
	.text
	.align	2
	.arch armv7ve
	.syntax unified
	.arm
	.fpu vfpv4
	.global	set
	.type	set, %function
set:
.set1:
	Push { LR }
	Push { LR }
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #684
	mov	r4, r0
	STR r4, [ r11 , #-152 ]
	mov	r4, r1
	STR r4, [ r11 , #-156 ]
	mov	r4, r2
	STR r4, [ r11 , #-160 ]
	@ %4 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #12
	STR r4, [ r11 , #-164 ]
	@ %5 = alloca [31 x i32]
	SUB	SP, SP, #124
	SUB	r4, r11, #136
	STR r4, [ r11 , #-168 ]
	@ %6 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #140
	STR r4, [ r11 , #-172 ]
	@ %7 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #144
	STR r4, [ r11 , #-176 ]
	@ %8 = alloca i32*
	SUB	SP, SP, #4
	SUB	r4, r11, #148
	STR r4, [ r11 , #-180 ]
	@ store i32* %0, i32** %8
	LDR r5, [ r11 , #-152 ]
	LDR r7, [ r11 , #-180 ]
	STR r5, [ r7 ]
	@ store i32 %1, i32* %7
	LDR r5, [ r11 , #-156 ]
	LDR r7, [ r11 , #-176 ]
	STR r5, [ r7 ]
	@ store i32 %2, i32* %6
	LDR r5, [ r11 , #-160 ]
	LDR r7, [ r11 , #-172 ]
	STR r5, [ r7 ]
	@ %9= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 0
	@ call void @memset(i32* %9,i32 0,i32 124)
	mov	r2, #124
	mov	r1, #0
	LDR r6, [ r11 , #-168 ]
	mov	r0, r6
	BLX	memset
	mov	r4, r0
	STR r4, [ r11 , #-184 ]
	@ %10= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 0
	@ store i32 1, i32* %10
	mov	r4, #1
	STR r4, [ r11 , #-188 ]
	LDR r5, [ r11 , #-188 ]
	LDR r7, [ r11 , #-168 ]
	STR r5, [ r7 ]
	@ %11= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 1
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #4
	STR r4, [ r11 , #-192 ]
	@ %12= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 0
	@ %13 = load i32, i32* %12
	LDR r7, [ r11 , #-168 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-196 ]
	@ %14 = mul i32 %13, 2
	mov	r4, #2
	STR r4, [ r11 , #-200 ]
	LDR r5, [ r11 , #-196 ]
	LDR r6, [ r11 , #-200 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-204 ]
	@ store i32 %14, i32* %11
	LDR r5, [ r11 , #-204 ]
	LDR r7, [ r11 , #-192 ]
	STR r5, [ r7 ]
	@ %15= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 2
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #8
	STR r4, [ r11 , #-208 ]
	@ %16= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 1
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #4
	STR r4, [ r11 , #-212 ]
	@ %17 = load i32, i32* %16
	LDR r7, [ r11 , #-212 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-216 ]
	@ %18 = mul i32 %17, 2
	mov	r4, #2
	STR r4, [ r11 , #-220 ]
	LDR r5, [ r11 , #-216 ]
	LDR r6, [ r11 , #-220 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-224 ]
	@ store i32 %18, i32* %15
	LDR r5, [ r11 , #-224 ]
	LDR r7, [ r11 , #-208 ]
	STR r5, [ r7 ]
	@ %19= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 3
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #12
	STR r4, [ r11 , #-228 ]
	@ %20= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 2
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #8
	STR r4, [ r11 , #-232 ]
	@ %21 = load i32, i32* %20
	LDR r7, [ r11 , #-232 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-236 ]
	@ %22 = mul i32 %21, 2
	mov	r4, #2
	STR r4, [ r11 , #-240 ]
	LDR r5, [ r11 , #-236 ]
	LDR r6, [ r11 , #-240 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-244 ]
	@ store i32 %22, i32* %19
	LDR r5, [ r11 , #-244 ]
	LDR r7, [ r11 , #-228 ]
	STR r5, [ r7 ]
	@ %23= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 4
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #16
	STR r4, [ r11 , #-248 ]
	@ %24= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 3
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #12
	STR r4, [ r11 , #-252 ]
	@ %25 = load i32, i32* %24
	LDR r7, [ r11 , #-252 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-256 ]
	@ %26 = mul i32 %25, 2
	mov	r4, #2
	STR r4, [ r11 , #-260 ]
	LDR r5, [ r11 , #-256 ]
	LDR r6, [ r11 , #-260 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-264 ]
	@ store i32 %26, i32* %23
	LDR r5, [ r11 , #-264 ]
	LDR r7, [ r11 , #-248 ]
	STR r5, [ r7 ]
	@ %27= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 5
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #20
	STR r4, [ r11 , #-268 ]
	@ %28= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 4
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #16
	STR r4, [ r11 , #-272 ]
	@ %29 = load i32, i32* %28
	LDR r7, [ r11 , #-272 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-276 ]
	@ %30 = mul i32 %29, 2
	mov	r4, #2
	STR r4, [ r11 , #-280 ]
	LDR r5, [ r11 , #-276 ]
	LDR r6, [ r11 , #-280 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-284 ]
	@ store i32 %30, i32* %27
	LDR r5, [ r11 , #-284 ]
	LDR r7, [ r11 , #-268 ]
	STR r5, [ r7 ]
	@ %31= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 6
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #24
	STR r4, [ r11 , #-288 ]
	@ %32= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 5
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #20
	STR r4, [ r11 , #-292 ]
	@ %33 = load i32, i32* %32
	LDR r7, [ r11 , #-292 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-296 ]
	@ %34 = mul i32 %33, 2
	mov	r4, #2
	STR r4, [ r11 , #-300 ]
	LDR r5, [ r11 , #-296 ]
	LDR r6, [ r11 , #-300 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-304 ]
	@ store i32 %34, i32* %31
	LDR r5, [ r11 , #-304 ]
	LDR r7, [ r11 , #-288 ]
	STR r5, [ r7 ]
	@ %35= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 7
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #28
	STR r4, [ r11 , #-308 ]
	@ %36= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 6
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #24
	STR r4, [ r11 , #-312 ]
	@ %37 = load i32, i32* %36
	LDR r7, [ r11 , #-312 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-316 ]
	@ %38 = mul i32 %37, 2
	mov	r4, #2
	STR r4, [ r11 , #-320 ]
	LDR r5, [ r11 , #-316 ]
	LDR r6, [ r11 , #-320 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-324 ]
	@ store i32 %38, i32* %35
	LDR r5, [ r11 , #-324 ]
	LDR r7, [ r11 , #-308 ]
	STR r5, [ r7 ]
	@ %39= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 8
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #32
	STR r4, [ r11 , #-328 ]
	@ %40= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 7
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #28
	STR r4, [ r11 , #-332 ]
	@ %41 = load i32, i32* %40
	LDR r7, [ r11 , #-332 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-336 ]
	@ %42 = mul i32 %41, 2
	mov	r4, #2
	STR r4, [ r11 , #-340 ]
	LDR r5, [ r11 , #-336 ]
	LDR r6, [ r11 , #-340 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-344 ]
	@ store i32 %42, i32* %39
	LDR r5, [ r11 , #-344 ]
	LDR r7, [ r11 , #-328 ]
	STR r5, [ r7 ]
	@ %43= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 9
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #36
	STR r4, [ r11 , #-348 ]
	@ %44= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 8
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #32
	STR r4, [ r11 , #-352 ]
	@ %45 = load i32, i32* %44
	LDR r7, [ r11 , #-352 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-356 ]
	@ %46 = mul i32 %45, 2
	mov	r4, #2
	STR r4, [ r11 , #-360 ]
	LDR r5, [ r11 , #-356 ]
	LDR r6, [ r11 , #-360 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-364 ]
	@ store i32 %46, i32* %43
	LDR r5, [ r11 , #-364 ]
	LDR r7, [ r11 , #-348 ]
	STR r5, [ r7 ]
	@ %47= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 10
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #40
	STR r4, [ r11 , #-368 ]
	@ %48= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 9
	LDR r5, [ r11 , #-168 ]
	ADD	r4, r5, #36
	STR r4, [ r11 , #-372 ]
	@ %49 = load i32, i32* %48
	LDR r7, [ r11 , #-372 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-376 ]
	@ %50 = mul i32 %49, 2
	mov	r4, #2
	STR r4, [ r11 , #-380 ]
	LDR r5, [ r11 , #-376 ]
	LDR r6, [ r11 , #-380 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-384 ]
	@ store i32 %50, i32* %47
	LDR r5, [ r11 , #-384 ]
	LDR r7, [ r11 , #-368 ]
	STR r5, [ r7 ]
	@ store i32 10, i32* %4
	mov	r4, #10
	STR r4, [ r11 , #-388 ]
	LDR r5, [ r11 , #-388 ]
	LDR r7, [ r11 , #-164 ]
	STR r5, [ r7 ]
	@ br label %51
	B	.set2

.set2:
	@ %52 = load i32, i32* %4
	LDR r7, [ r11 , #-164 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-392 ]
	@ %53= icmp slt i32 %52, 30
	@ br i1 %53, label %54, label %64
	LDR r5, [ r11 , #-392 ]
	cmp r5 , #30
	BLT	.set3
	B	.set4

.set3:
	@ %55 = load i32, i32* %4
	LDR r7, [ r11 , #-164 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-396 ]
	@ %56 = add i32 %55, 1
	LDR r5, [ r11 , #-396 ]
	ADD	r4, r5, #1
	STR r4, [ r11 , #-400 ]
	@ store i32 %56, i32* %4
	LDR r5, [ r11 , #-400 ]
	LDR r7, [ r11 , #-164 ]
	STR r5, [ r7 ]
	@ %57 = load i32, i32* %4
	LDR r7, [ r11 , #-164 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-404 ]
	@ %58= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %57
	mov	r4, 4
	STR r4, [ r11 , #-408 ]
	LDR r5, [ r11 , #-404 ]
	LDR r6, [ r11 , #-408 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-412 ]
	LDR r5, [ r11 , #-168 ]
	LDR r6, [ r11 , #-412 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-416 ]
	@ %59 = load i32, i32* %4
	LDR r7, [ r11 , #-164 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-420 ]
	@ %60 = sub i32 %59, 1
	LDR r5, [ r11 , #-420 ]
	SUB	r4, r5, #1
	STR r4, [ r11 , #-424 ]
	@ %61= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %60
	mov	r4, 4
	STR r4, [ r11 , #-428 ]
	LDR r5, [ r11 , #-424 ]
	LDR r6, [ r11 , #-428 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-432 ]
	LDR r5, [ r11 , #-168 ]
	LDR r6, [ r11 , #-432 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-436 ]
	@ %62 = load i32, i32* %61
	LDR r7, [ r11 , #-436 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-440 ]
	@ %63 = mul i32 %62, 2
	mov	r4, #2
	STR r4, [ r11 , #-444 ]
	LDR r5, [ r11 , #-440 ]
	LDR r6, [ r11 , #-444 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-448 ]
	@ store i32 %63, i32* %58
	LDR r5, [ r11 , #-448 ]
	LDR r7, [ r11 , #-416 ]
	STR r5, [ r7 ]
	@ br label %51
	B	.set2

.set4:
	@ %3 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #8
	STR r4, [ r11 , #-452 ]
	@ store i32 0, i32* %3
	mov	r4, #0
	STR r4, [ r11 , #-456 ]
	LDR r5, [ r11 , #-456 ]
	LDR r7, [ r11 , #-452 ]
	STR r5, [ r7 ]
	@ %65 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-460 ]
	@ %66 = sdiv i32 %65, 30
	mov	r4, #30
	STR r4, [ r11 , #-464 ]
	LDR r5, [ r11 , #-460 ]
	LDR r6, [ r11 , #-464 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-468 ]
	@ %67= icmp sge i32 %66, 10000
	@ br i1 %67, label %68, label %69
	movw	r4, 10000
	movt	r4, 0

	STR r4, [ r11 , #-472 ]
	LDR r5, [ r11 , #-468 ]
	LDR r6, [ r11 , #-472 ]
	cmp r5 , r6
	BGE	.set5
	B	.set6

.set5:
	@ ret i32 0
	mov	r0, #0
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

.set6:
	@ %70 = load i32*, i32** %8
	LDR r7, [ r11 , #-180 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-476 ]
	@ %71 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-480 ]
	@ %72 = sdiv i32 %71, 30
	mov	r4, #30
	STR r4, [ r11 , #-484 ]
	LDR r5, [ r11 , #-480 ]
	LDR r6, [ r11 , #-484 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-488 ]
	@ %73= getelementptr i32,i32* %70 , i32 %72
	mov	r4, 4
	STR r4, [ r11 , #-492 ]
	LDR r5, [ r11 , #-488 ]
	LDR r6, [ r11 , #-492 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-496 ]
	LDR r5, [ r11 , #-476 ]
	LDR r6, [ r11 , #-496 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-500 ]
	@ %74 = load i32, i32* %73
	LDR r7, [ r11 , #-500 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-504 ]
	@ %75 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-508 ]
	@ %76 = srem i32 %75, 30
	mov	r4, #30
	STR r4, [ r11 , #-512 ]
	LDR r6, [ r11 , #-508 ]
	mov	r0, r6
	LDR r6, [ r11 , #-512 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-516 ]
	@ %77= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %76
	mov	r4, 4
	STR r4, [ r11 , #-520 ]
	LDR r5, [ r11 , #-516 ]
	LDR r6, [ r11 , #-520 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-524 ]
	LDR r5, [ r11 , #-168 ]
	LDR r6, [ r11 , #-524 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-528 ]
	@ %78 = load i32, i32* %77
	LDR r7, [ r11 , #-528 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-532 ]
	@ %79 = sdiv i32 %74, %78
	LDR r5, [ r11 , #-504 ]
	LDR r6, [ r11 , #-532 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-536 ]
	@ %80 = srem i32 %79, 2
	mov	r4, #2
	STR r4, [ r11 , #-540 ]
	LDR r6, [ r11 , #-536 ]
	mov	r0, r6
	LDR r6, [ r11 , #-540 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-544 ]
	@ %81 = load i32, i32* %6
	LDR r7, [ r11 , #-172 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-548 ]
	@ %82= icmp ne i32 %80, %81
	@ br i1 %82, label %83, label %130
	LDR r5, [ r11 , #-544 ]
	LDR r6, [ r11 , #-548 ]
	cmp r5 , r6
	BNE	.set7
	B	.set16

.set7:
	@ %84 = load i32*, i32** %8
	LDR r7, [ r11 , #-180 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-552 ]
	@ %85 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-556 ]
	@ %86 = sdiv i32 %85, 30
	mov	r4, #30
	STR r4, [ r11 , #-560 ]
	LDR r5, [ r11 , #-556 ]
	LDR r6, [ r11 , #-560 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-564 ]
	@ %87= getelementptr i32,i32* %84 , i32 %86
	mov	r4, 4
	STR r4, [ r11 , #-568 ]
	LDR r5, [ r11 , #-564 ]
	LDR r6, [ r11 , #-568 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-572 ]
	LDR r5, [ r11 , #-552 ]
	LDR r6, [ r11 , #-572 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-576 ]
	@ %88 = load i32, i32* %87
	LDR r7, [ r11 , #-576 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-580 ]
	@ %89 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-584 ]
	@ %90 = srem i32 %89, 30
	mov	r4, #30
	STR r4, [ r11 , #-588 ]
	LDR r6, [ r11 , #-584 ]
	mov	r0, r6
	LDR r6, [ r11 , #-588 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-592 ]
	@ %91= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %90
	mov	r4, 4
	STR r4, [ r11 , #-596 ]
	LDR r5, [ r11 , #-592 ]
	LDR r6, [ r11 , #-596 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-600 ]
	LDR r5, [ r11 , #-168 ]
	LDR r6, [ r11 , #-600 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-604 ]
	@ %92 = load i32, i32* %91
	LDR r7, [ r11 , #-604 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-608 ]
	@ %93 = sdiv i32 %88, %92
	LDR r5, [ r11 , #-580 ]
	LDR r6, [ r11 , #-608 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-612 ]
	@ %94 = srem i32 %93, 2
	mov	r4, #2
	STR r4, [ r11 , #-616 ]
	LDR r6, [ r11 , #-612 ]
	mov	r0, r6
	LDR r6, [ r11 , #-616 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-620 ]
	@ %95= icmp eq i32 %94, 0
	@ br i1 %95, label %96, label %105
	LDR r5, [ r11 , #-620 ]
	cmp r5 , #0
	BEQ	.set8
	B	.set11

.set8:
	@ %97 = load i32, i32* %6
	LDR r7, [ r11 , #-172 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-624 ]
	@ %98= icmp eq i32 %97, 1
	@ br i1 %98, label %99, label %104
	LDR r5, [ r11 , #-624 ]
	cmp r5 , #1
	BEQ	.set9
	B	.set10

.set9:
	@ %100 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-628 ]
	@ %101 = srem i32 %100, 30
	mov	r4, #30
	STR r4, [ r11 , #-632 ]
	LDR r6, [ r11 , #-628 ]
	mov	r0, r6
	LDR r6, [ r11 , #-632 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-636 ]
	@ %102= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %101
	mov	r4, 4
	STR r4, [ r11 , #-640 ]
	LDR r5, [ r11 , #-636 ]
	LDR r6, [ r11 , #-640 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-644 ]
	LDR r5, [ r11 , #-168 ]
	LDR r6, [ r11 , #-644 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-648 ]
	@ %103 = load i32, i32* %102
	LDR r7, [ r11 , #-648 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-652 ]
	@ store i32 %103, i32* %3
	LDR r5, [ r11 , #-652 ]
	LDR r7, [ r11 , #-452 ]
	STR r5, [ r7 ]
	@ br label %104
	B	.set10

.set10:
	@ br label %105
	B	.set11

.set11:
	@ %106 = load i32*, i32** %8
	LDR r7, [ r11 , #-180 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-656 ]
	@ %107 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-660 ]
	@ %108 = sdiv i32 %107, 30
	mov	r4, #30
	STR r4, [ r11 , #-664 ]
	LDR r5, [ r11 , #-660 ]
	LDR r6, [ r11 , #-664 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-668 ]
	@ %109= getelementptr i32,i32* %106 , i32 %108
	mov	r4, 4
	STR r4, [ r11 , #-672 ]
	LDR r5, [ r11 , #-668 ]
	LDR r6, [ r11 , #-672 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-676 ]
	LDR r5, [ r11 , #-656 ]
	LDR r6, [ r11 , #-676 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-680 ]
	@ %110 = load i32, i32* %109
	LDR r7, [ r11 , #-680 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-684 ]
	@ %111 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-688 ]
	@ %112 = srem i32 %111, 30
	mov	r4, #30
	STR r4, [ r11 , #-692 ]
	LDR r6, [ r11 , #-688 ]
	mov	r0, r6
	LDR r6, [ r11 , #-692 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-696 ]
	@ %113= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %112
	mov	r4, 4
	STR r4, [ r11 , #-700 ]
	LDR r5, [ r11 , #-696 ]
	LDR r6, [ r11 , #-700 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-704 ]
	LDR r5, [ r11 , #-168 ]
	LDR r6, [ r11 , #-704 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-708 ]
	@ %114 = load i32, i32* %113
	LDR r7, [ r11 , #-708 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-712 ]
	@ %115 = sdiv i32 %110, %114
	LDR r5, [ r11 , #-684 ]
	LDR r6, [ r11 , #-712 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-716 ]
	@ %116 = srem i32 %115, 2
	mov	r4, #2
	STR r4, [ r11 , #-720 ]
	LDR r6, [ r11 , #-716 ]
	mov	r0, r6
	LDR r6, [ r11 , #-720 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-724 ]
	@ %117= icmp eq i32 %116, 1
	@ br i1 %117, label %118, label %129
	LDR r5, [ r11 , #-724 ]
	cmp r5 , #1
	BEQ	.set12
	B	.set15

.set12:
	@ %119 = load i32, i32* %6
	LDR r7, [ r11 , #-172 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-728 ]
	@ %120= icmp eq i32 %119, 0
	@ br i1 %120, label %121, label %128
	LDR r5, [ r11 , #-728 ]
	cmp r5 , #0
	BEQ	.set13
	B	.set14

.set13:
	@ %122 = load i32, i32* %3
	LDR r7, [ r11 , #-452 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-732 ]
	@ %123 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-736 ]
	@ %124 = srem i32 %123, 30
	mov	r4, #30
	STR r4, [ r11 , #-740 ]
	LDR r6, [ r11 , #-736 ]
	mov	r0, r6
	LDR r6, [ r11 , #-740 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-744 ]
	@ %125= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %124
	mov	r4, 4
	STR r4, [ r11 , #-748 ]
	LDR r5, [ r11 , #-744 ]
	LDR r6, [ r11 , #-748 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-752 ]
	LDR r5, [ r11 , #-168 ]
	LDR r6, [ r11 , #-752 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-756 ]
	@ %126 = load i32, i32* %125
	LDR r7, [ r11 , #-756 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-760 ]
	@ %127 = sub i32 %122, %126
	LDR r5, [ r11 , #-732 ]
	LDR r6, [ r11 , #-760 ]
	SUB	r4, r5, r6
	STR r4, [ r11 , #-764 ]
	@ store i32 %127, i32* %3
	LDR r5, [ r11 , #-764 ]
	LDR r7, [ r11 , #-452 ]
	STR r5, [ r7 ]
	@ br label %128
	B	.set14

.set14:
	@ br label %129
	B	.set15

.set15:
	@ br label %130
	B	.set16

.set16:
	@ %131 = load i32*, i32** %8
	LDR r7, [ r11 , #-180 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-768 ]
	@ %132 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-772 ]
	@ %133 = sdiv i32 %132, 30
	mov	r4, #30
	STR r4, [ r11 , #-776 ]
	LDR r5, [ r11 , #-772 ]
	LDR r6, [ r11 , #-776 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-780 ]
	@ %134= getelementptr i32,i32* %131 , i32 %133
	mov	r4, 4
	STR r4, [ r11 , #-784 ]
	LDR r5, [ r11 , #-780 ]
	LDR r6, [ r11 , #-784 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-788 ]
	LDR r5, [ r11 , #-768 ]
	LDR r6, [ r11 , #-788 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-792 ]
	@ %135 = load i32*, i32** %8
	LDR r7, [ r11 , #-180 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-796 ]
	@ %136 = load i32, i32* %7
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-800 ]
	@ %137 = sdiv i32 %136, 30
	mov	r4, #30
	STR r4, [ r11 , #-804 ]
	LDR r5, [ r11 , #-800 ]
	LDR r6, [ r11 , #-804 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-808 ]
	@ %138= getelementptr i32,i32* %135 , i32 %137
	mov	r4, 4
	STR r4, [ r11 , #-812 ]
	LDR r5, [ r11 , #-808 ]
	LDR r6, [ r11 , #-812 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-816 ]
	LDR r5, [ r11 , #-796 ]
	LDR r6, [ r11 , #-816 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-820 ]
	@ %139 = load i32, i32* %138
	LDR r7, [ r11 , #-820 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-824 ]
	@ %140 = load i32, i32* %3
	LDR r7, [ r11 , #-452 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-828 ]
	@ %141 = add i32 %139, %140
	LDR r5, [ r11 , #-824 ]
	LDR r6, [ r11 , #-828 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-832 ]
	@ store i32 %141, i32* %134
	LDR r5, [ r11 , #-832 ]
	LDR r7, [ r11 , #-792 ]
	STR r5, [ r7 ]
	@ ret i32 0
	mov	r0, #0
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

	.size	set, .-set
	.align	2
	.arch armv7ve
	.syntax unified
	.arm
	.fpu vfpv4
	.global	rand
	.type	rand, %function
rand:
.rand17:
	Push { LR }
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #112
	@ %0 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-8 ]
	LDR r7, [ r11 , #-8 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-12 ]
	@ %1= getelementptr [3 x i32],[3 x i32]* @seed , i32 0, i32 0
	movw	r4, #:lower16:seed
	movt	r4, #:upper16:seed

	STR r4, [ r11 , #-16 ]
	@ %2 = load i32, i32* %1
	LDR r7, [ r11 , #-16 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-20 ]
	@ %3 = mul i32 %0, %2
	LDR r5, [ r11 , #-12 ]
	LDR r6, [ r11 , #-20 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-24 ]
	@ %4= getelementptr [3 x i32],[3 x i32]* @seed , i32 0, i32 1
	movw	r4, #:lower16:seed
	movt	r4, #:upper16:seed

	STR r4, [ r11 , #-28 ]
	LDR r5, [ r11 , #-28 ]
	ADD	r4, r5, #4
	STR r4, [ r11 , #-32 ]
	@ %5 = load i32, i32* %4
	LDR r7, [ r11 , #-32 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-36 ]
	@ %6 = add i32 %3, %5
	LDR r5, [ r11 , #-24 ]
	LDR r6, [ r11 , #-36 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-40 ]
	@ store i32 %6, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-44 ]
	LDR r5, [ r11 , #-40 ]
	LDR r7, [ r11 , #-44 ]
	STR r5, [ r7 ]
	@ %7 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-48 ]
	LDR r7, [ r11 , #-48 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-52 ]
	@ %8= getelementptr [3 x i32],[3 x i32]* @seed , i32 0, i32 2
	movw	r4, #:lower16:seed
	movt	r4, #:upper16:seed

	STR r4, [ r11 , #-56 ]
	LDR r5, [ r11 , #-56 ]
	ADD	r4, r5, #8
	STR r4, [ r11 , #-60 ]
	@ %9 = load i32, i32* %8
	LDR r7, [ r11 , #-60 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-64 ]
	@ %10 = srem i32 %7, %9
	LDR r6, [ r11 , #-52 ]
	mov	r0, r6
	LDR r6, [ r11 , #-64 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-68 ]
	@ store i32 %10, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-72 ]
	LDR r5, [ r11 , #-68 ]
	LDR r7, [ r11 , #-72 ]
	STR r5, [ r7 ]
	@ %11 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-76 ]
	LDR r7, [ r11 , #-76 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-80 ]
	@ %12= icmp slt i32 %11, 0
	@ br i1 %12, label %13, label %18
	LDR r5, [ r11 , #-80 ]
	cmp r5 , #0
	BLT	.rand18
	B	.rand19

.rand18:
	@ %14= getelementptr [3 x i32],[3 x i32]* @seed , i32 0, i32 2
	movw	r4, #:lower16:seed
	movt	r4, #:upper16:seed

	STR r4, [ r11 , #-84 ]
	LDR r5, [ r11 , #-84 ]
	ADD	r4, r5, #8
	STR r4, [ r11 , #-88 ]
	@ %15 = load i32, i32* %14
	LDR r7, [ r11 , #-88 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-92 ]
	@ %16 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-96 ]
	LDR r7, [ r11 , #-96 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-100 ]
	@ %17 = add i32 %15, %16
	LDR r5, [ r11 , #-92 ]
	LDR r6, [ r11 , #-100 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-104 ]
	@ store i32 %17, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-108 ]
	LDR r5, [ r11 , #-104 ]
	LDR r7, [ r11 , #-108 ]
	STR r5, [ r7 ]
	@ br label %18
	B	.rand19

.rand19:
	@ %19 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-112 ]
	LDR r7, [ r11 , #-112 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-116 ]
	@ ret i32 %19
	LDR r6, [ r11 , #-116 ]
	mov	r0, r6
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

	.size	rand, .-rand
	.align	2
	.arch armv7ve
	.syntax unified
	.arm
	.fpu vfpv4
	.global	main
	.type	main, %function
main:
.main20:
	Push { LR }
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #88
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
	@ %3 = call i32 @getint()
	BLX	getint
	mov	r4, r0
	STR r4, [ r11 , #-32 ]
	@ store i32 %3, i32* %2
	LDR r5, [ r11 , #-32 ]
	LDR r7, [ r11 , #-28 ]
	STR r5, [ r7 ]
	@ %4 = call i32 @getint()
	BLX	getint
	mov	r4, r0
	STR r4, [ r11 , #-36 ]
	@ store i32 %4, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-40 ]
	LDR r5, [ r11 , #-36 ]
	LDR r7, [ r11 , #-40 ]
	STR r5, [ r7 ]
	@ br label %5
	B	.main21

.main21:
	@ %6 = load i32, i32* %2
	LDR r7, [ r11 , #-28 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-44 ]
	@ %7= icmp sgt i32 %6, 0
	@ br i1 %7, label %8, label %19
	LDR r5, [ r11 , #-44 ]
	cmp r5 , #0
	BGT	.main22
	B	.main23

.main22:
	@ %9 = load i32, i32* %2
	LDR r7, [ r11 , #-28 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-48 ]
	@ %10 = sub i32 %9, 1
	LDR r5, [ r11 , #-48 ]
	SUB	r4, r5, #1
	STR r4, [ r11 , #-52 ]
	@ store i32 %10, i32* %2
	LDR r5, [ r11 , #-52 ]
	LDR r7, [ r11 , #-28 ]
	STR r5, [ r7 ]
	@ %11 = call i32 @rand()
	BLX	rand
	mov	r4, r0
	STR r4, [ r11 , #-56 ]
	@ %12 = srem i32 %11, 300000
	movw	r4, 37856
	movt	r4, 4

	STR r4, [ r11 , #-60 ]
	LDR r6, [ r11 , #-56 ]
	mov	r0, r6
	LDR r6, [ r11 , #-60 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-64 ]
	@ store i32 %12, i32* %1
	LDR r5, [ r11 , #-64 ]
	LDR r7, [ r11 , #-24 ]
	STR r5, [ r7 ]
	@ %13 = call i32 @rand()
	BLX	rand
	mov	r4, r0
	STR r4, [ r11 , #-68 ]
	@ %14 = srem i32 %13, 2
	mov	r4, #2
	STR r4, [ r11 , #-72 ]
	LDR r6, [ r11 , #-68 ]
	mov	r0, r6
	LDR r6, [ r11 , #-72 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-76 ]
	@ store i32 %14, i32* %0
	LDR r5, [ r11 , #-76 ]
	LDR r7, [ r11 , #-20 ]
	STR r5, [ r7 ]
	@ %15= getelementptr [10000 x i32],[10000 x i32]* @a , i32 0, i32 0
	movw	r4, #:lower16:a
	movt	r4, #:upper16:a

	STR r4, [ r11 , #-80 ]
	@ %16 = load i32, i32* %1
	LDR r7, [ r11 , #-24 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-84 ]
	@ %17 = load i32, i32* %0
	LDR r7, [ r11 , #-20 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-88 ]
	@ %18 = call i32 @set(i32* %15,i32 %16,i32 %17)
	LDR r6, [ r11 , #-88 ]
	mov	r2, r6
	LDR r6, [ r11 , #-84 ]
	mov	r1, r6
	LDR r6, [ r11 , #-80 ]
	mov	r0, r6
	BLX	set
	mov	r4, r0
	STR r4, [ r11 , #-92 ]
	@ br label %5
	B	.main21

.main23:
	@ %20= getelementptr [10000 x i32],[10000 x i32]* @a , i32 0, i32 0
	movw	r4, #:lower16:a
	movt	r4, #:upper16:a

	STR r4, [ r11 , #-96 ]
	@ call void @putarray(i32 10000,i32* %20)
	LDR r6, [ r11 , #-96 ]
	mov	r1, r6
	movw	r4, 10000
	movt	r4, 0

	STR r4, [ r11 , #-100 ]
	LDR r6, [ r11 , #-100 ]
	mov	r0, r6
	BLX	putarray
	mov	r4, r0
	STR r4, [ r11 , #-104 ]
	@ ret i32 0
	mov	r0, #0
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

	.size	main, .-main

