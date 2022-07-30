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
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #700
	mov	r4, r0
	STR r4, [ r11 , #-156 ]
	mov	r4, r1
	STR r4, [ r11 , #-160 ]
	mov	r4, r2
	STR r4, [ r11 , #-164 ]
	@ %3 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #8
	STR r4, [ r11 , #-168 ]
	@ %4 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #12
	STR r4, [ r11 , #-172 ]
	@ %5 = alloca [31 x i32]
	SUB	SP, SP, #124
	SUB	r4, r11, #136
	STR r4, [ r11 , #-176 ]
	@ %6 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #140
	STR r4, [ r11 , #-180 ]
	@ %7 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #144
	STR r4, [ r11 , #-184 ]
	@ %8 = alloca i32*
	SUB	SP, SP, #4
	SUB	r4, r11, #148
	STR r4, [ r11 , #-188 ]
	@ ret = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #152
	STR r4, [ r11 , #-192 ]
	@ store i32* %0, i32** %8
	LDR r5, [ r11 , #-156 ]
	LDR r7, [ r11 , #-188 ]
	STR r5, [ r7 ]
	@ store i32 %1, i32* %7
	LDR r5, [ r11 , #-160 ]
	LDR r7, [ r11 , #-184 ]
	STR r5, [ r7 ]
	@ store i32 %2, i32* %6
	LDR r5, [ r11 , #-164 ]
	LDR r7, [ r11 , #-180 ]
	STR r5, [ r7 ]
	@ %9= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 0
	@ call void @memset(i32* %9,i32 0,i32 124)
	mov	r2, #124
	mov	r1, #0
	LDR r6, [ r11 , #-176 ]
	mov	r0, r6
	BLX	memset
	mov	r4, r0
	STR r4, [ r11 , #-196 ]
	@ %10= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 0
	@ store i32 1, i32* %10
	mov	r4, #1
	STR r4, [ r11 , #-200 ]
	LDR r5, [ r11 , #-200 ]
	LDR r7, [ r11 , #-176 ]
	STR r5, [ r7 ]
	@ %11= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 1
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #4
	STR r4, [ r11 , #-204 ]
	@ %12= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 0
	@ %13 = load i32, i32* %12
	LDR r7, [ r11 , #-176 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-208 ]
	@ %14 = mul i32 %13, 2
	mov	r4, #2
	STR r4, [ r11 , #-212 ]
	LDR r5, [ r11 , #-208 ]
	LDR r6, [ r11 , #-212 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-216 ]
	@ store i32 %14, i32* %11
	LDR r5, [ r11 , #-216 ]
	LDR r7, [ r11 , #-204 ]
	STR r5, [ r7 ]
	@ %15= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 2
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #8
	STR r4, [ r11 , #-220 ]
	@ %16= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 1
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #4
	STR r4, [ r11 , #-224 ]
	@ %17 = load i32, i32* %16
	LDR r7, [ r11 , #-224 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-228 ]
	@ %18 = mul i32 %17, 2
	mov	r4, #2
	STR r4, [ r11 , #-232 ]
	LDR r5, [ r11 , #-228 ]
	LDR r6, [ r11 , #-232 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-236 ]
	@ store i32 %18, i32* %15
	LDR r5, [ r11 , #-236 ]
	LDR r7, [ r11 , #-220 ]
	STR r5, [ r7 ]
	@ %19= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 3
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #12
	STR r4, [ r11 , #-240 ]
	@ %20= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 2
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #8
	STR r4, [ r11 , #-244 ]
	@ %21 = load i32, i32* %20
	LDR r7, [ r11 , #-244 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-248 ]
	@ %22 = mul i32 %21, 2
	mov	r4, #2
	STR r4, [ r11 , #-252 ]
	LDR r5, [ r11 , #-248 ]
	LDR r6, [ r11 , #-252 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-256 ]
	@ store i32 %22, i32* %19
	LDR r5, [ r11 , #-256 ]
	LDR r7, [ r11 , #-240 ]
	STR r5, [ r7 ]
	@ %23= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 4
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #16
	STR r4, [ r11 , #-260 ]
	@ %24= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 3
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #12
	STR r4, [ r11 , #-264 ]
	@ %25 = load i32, i32* %24
	LDR r7, [ r11 , #-264 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-268 ]
	@ %26 = mul i32 %25, 2
	mov	r4, #2
	STR r4, [ r11 , #-272 ]
	LDR r5, [ r11 , #-268 ]
	LDR r6, [ r11 , #-272 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-276 ]
	@ store i32 %26, i32* %23
	LDR r5, [ r11 , #-276 ]
	LDR r7, [ r11 , #-260 ]
	STR r5, [ r7 ]
	@ %27= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 5
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #20
	STR r4, [ r11 , #-280 ]
	@ %28= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 4
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #16
	STR r4, [ r11 , #-284 ]
	@ %29 = load i32, i32* %28
	LDR r7, [ r11 , #-284 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-288 ]
	@ %30 = mul i32 %29, 2
	mov	r4, #2
	STR r4, [ r11 , #-292 ]
	LDR r5, [ r11 , #-288 ]
	LDR r6, [ r11 , #-292 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-296 ]
	@ store i32 %30, i32* %27
	LDR r5, [ r11 , #-296 ]
	LDR r7, [ r11 , #-280 ]
	STR r5, [ r7 ]
	@ %31= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 6
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #24
	STR r4, [ r11 , #-300 ]
	@ %32= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 5
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #20
	STR r4, [ r11 , #-304 ]
	@ %33 = load i32, i32* %32
	LDR r7, [ r11 , #-304 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-308 ]
	@ %34 = mul i32 %33, 2
	mov	r4, #2
	STR r4, [ r11 , #-312 ]
	LDR r5, [ r11 , #-308 ]
	LDR r6, [ r11 , #-312 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-316 ]
	@ store i32 %34, i32* %31
	LDR r5, [ r11 , #-316 ]
	LDR r7, [ r11 , #-300 ]
	STR r5, [ r7 ]
	@ %35= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 7
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #28
	STR r4, [ r11 , #-320 ]
	@ %36= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 6
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #24
	STR r4, [ r11 , #-324 ]
	@ %37 = load i32, i32* %36
	LDR r7, [ r11 , #-324 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-328 ]
	@ %38 = mul i32 %37, 2
	mov	r4, #2
	STR r4, [ r11 , #-332 ]
	LDR r5, [ r11 , #-328 ]
	LDR r6, [ r11 , #-332 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-336 ]
	@ store i32 %38, i32* %35
	LDR r5, [ r11 , #-336 ]
	LDR r7, [ r11 , #-320 ]
	STR r5, [ r7 ]
	@ %39= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 8
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #32
	STR r4, [ r11 , #-340 ]
	@ %40= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 7
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #28
	STR r4, [ r11 , #-344 ]
	@ %41 = load i32, i32* %40
	LDR r7, [ r11 , #-344 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-348 ]
	@ %42 = mul i32 %41, 2
	mov	r4, #2
	STR r4, [ r11 , #-352 ]
	LDR r5, [ r11 , #-348 ]
	LDR r6, [ r11 , #-352 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-356 ]
	@ store i32 %42, i32* %39
	LDR r5, [ r11 , #-356 ]
	LDR r7, [ r11 , #-340 ]
	STR r5, [ r7 ]
	@ %43= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 9
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #36
	STR r4, [ r11 , #-360 ]
	@ %44= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 8
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #32
	STR r4, [ r11 , #-364 ]
	@ %45 = load i32, i32* %44
	LDR r7, [ r11 , #-364 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-368 ]
	@ %46 = mul i32 %45, 2
	mov	r4, #2
	STR r4, [ r11 , #-372 ]
	LDR r5, [ r11 , #-368 ]
	LDR r6, [ r11 , #-372 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-376 ]
	@ store i32 %46, i32* %43
	LDR r5, [ r11 , #-376 ]
	LDR r7, [ r11 , #-360 ]
	STR r5, [ r7 ]
	@ %47= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 10
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #40
	STR r4, [ r11 , #-380 ]
	@ %48= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 9
	LDR r5, [ r11 , #-176 ]
	ADD	r4, r5, #36
	STR r4, [ r11 , #-384 ]
	@ %49 = load i32, i32* %48
	LDR r7, [ r11 , #-384 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-388 ]
	@ %50 = mul i32 %49, 2
	mov	r4, #2
	STR r4, [ r11 , #-392 ]
	LDR r5, [ r11 , #-388 ]
	LDR r6, [ r11 , #-392 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-396 ]
	@ store i32 %50, i32* %47
	LDR r5, [ r11 , #-396 ]
	LDR r7, [ r11 , #-380 ]
	STR r5, [ r7 ]
	@ store i32 10, i32* %4
	mov	r4, #10
	STR r4, [ r11 , #-400 ]
	LDR r5, [ r11 , #-400 ]
	LDR r7, [ r11 , #-172 ]
	STR r5, [ r7 ]
	@ br label %51
	B	.set2

.set2:
	@ %52 = load i32, i32* %4
	LDR r7, [ r11 , #-172 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-404 ]
	@ %53= icmp slt i32 %52, 30
	@ br i1 %53, label %54, label %65
	LDR r5, [ r11 , #-404 ]
	cmp r5 , #30
	BLT	.set3
	B	.set5

.set3:
	@ %55 = load i32, i32* %4
	LDR r7, [ r11 , #-172 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-408 ]
	@ %56 = add i32 %55, 1
	LDR r5, [ r11 , #-408 ]
	ADD	r4, r5, #1
	STR r4, [ r11 , #-412 ]
	@ store i32 %56, i32* %4
	LDR r5, [ r11 , #-412 ]
	LDR r7, [ r11 , #-172 ]
	STR r5, [ r7 ]
	@ %57 = load i32, i32* %4
	LDR r7, [ r11 , #-172 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-416 ]
	@ %58= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %57
	mov	r4, 4
	STR r4, [ r11 , #-420 ]
	LDR r5, [ r11 , #-416 ]
	LDR r6, [ r11 , #-420 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-424 ]
	LDR r5, [ r11 , #-176 ]
	LDR r6, [ r11 , #-424 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-428 ]
	@ %59 = load i32, i32* %4
	LDR r7, [ r11 , #-172 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-432 ]
	@ %60 = sub i32 %59, 1
	LDR r5, [ r11 , #-432 ]
	SUB	r4, r5, #1
	STR r4, [ r11 , #-436 ]
	@ %61= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %60
	mov	r4, 4
	STR r4, [ r11 , #-440 ]
	LDR r5, [ r11 , #-436 ]
	LDR r6, [ r11 , #-440 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-444 ]
	LDR r5, [ r11 , #-176 ]
	LDR r6, [ r11 , #-444 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-448 ]
	@ %62 = load i32, i32* %61
	LDR r7, [ r11 , #-448 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-452 ]
	@ %63 = mul i32 %62, 2
	mov	r4, #2
	STR r4, [ r11 , #-456 ]
	LDR r5, [ r11 , #-452 ]
	LDR r6, [ r11 , #-456 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-460 ]
	@ store i32 %63, i32* %58
	LDR r5, [ r11 , #-460 ]
	LDR r7, [ r11 , #-428 ]
	STR r5, [ r7 ]
	@ br label %64
	B	.set4

.set4:
	@ br label %51
	B	.set2

.set5:
	@ store i32 0, i32* %3
	mov	r4, #0
	STR r4, [ r11 , #-464 ]
	LDR r5, [ r11 , #-464 ]
	LDR r7, [ r11 , #-168 ]
	STR r5, [ r7 ]
	@ %66 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-468 ]
	@ %67 = sdiv i32 %66, 30
	mov	r4, #30
	STR r4, [ r11 , #-472 ]
	LDR r5, [ r11 , #-468 ]
	LDR r6, [ r11 , #-472 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-476 ]
	@ %68= icmp sge i32 %67, 10000
	@ br i1 %68, label %69, label %70
	movw	r4, 10000
	movt	r4, 0

	STR r4, [ r11 , #-480 ]
	LDR r5, [ r11 , #-476 ]
	LDR r6, [ r11 , #-480 ]
	cmp r5 , r6
	BGE	.set6
	B	.set7

.set6:
	@ store i32 0, i32* ret
	mov	r4, #0
	STR r4, [ r11 , #-484 ]
	LDR r5, [ r11 , #-484 ]
	LDR r7, [ r11 , #-192 ]
	STR r5, [ r7 ]
	@ br label %ret
	B	.set18

.set7:
	@ %71 = load i32*, i32** %8
	LDR r7, [ r11 , #-188 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-488 ]
	@ %72 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-492 ]
	@ %73 = sdiv i32 %72, 30
	mov	r4, #30
	STR r4, [ r11 , #-496 ]
	LDR r5, [ r11 , #-492 ]
	LDR r6, [ r11 , #-496 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-500 ]
	@ %74= getelementptr i32,i32* %71 , i32 %73
	mov	r4, 4
	STR r4, [ r11 , #-504 ]
	LDR r5, [ r11 , #-500 ]
	LDR r6, [ r11 , #-504 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-508 ]
	LDR r5, [ r11 , #-488 ]
	LDR r6, [ r11 , #-508 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-512 ]
	@ %75 = load i32, i32* %74
	LDR r7, [ r11 , #-512 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-516 ]
	@ %76 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-520 ]
	@ %77 = srem i32 %76, 30
	mov	r4, #30
	STR r4, [ r11 , #-524 ]
	LDR r6, [ r11 , #-520 ]
	mov	r0, r6
	LDR r6, [ r11 , #-524 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-528 ]
	@ %78= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %77
	mov	r4, 4
	STR r4, [ r11 , #-532 ]
	LDR r5, [ r11 , #-528 ]
	LDR r6, [ r11 , #-532 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-536 ]
	LDR r5, [ r11 , #-176 ]
	LDR r6, [ r11 , #-536 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-540 ]
	@ %79 = load i32, i32* %78
	LDR r7, [ r11 , #-540 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-544 ]
	@ %80 = sdiv i32 %75, %79
	LDR r5, [ r11 , #-516 ]
	LDR r6, [ r11 , #-544 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-548 ]
	@ %81 = srem i32 %80, 2
	mov	r4, #2
	STR r4, [ r11 , #-552 ]
	LDR r6, [ r11 , #-548 ]
	mov	r0, r6
	LDR r6, [ r11 , #-552 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-556 ]
	@ %82 = load i32, i32* %6
	LDR r7, [ r11 , #-180 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-560 ]
	@ %83= icmp ne i32 %81, %82
	@ br i1 %83, label %84, label %131
	LDR r5, [ r11 , #-556 ]
	LDR r6, [ r11 , #-560 ]
	cmp r5 , r6
	BNE	.set8
	B	.set17

.set8:
	@ %85 = load i32*, i32** %8
	LDR r7, [ r11 , #-188 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-564 ]
	@ %86 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-568 ]
	@ %87 = sdiv i32 %86, 30
	mov	r4, #30
	STR r4, [ r11 , #-572 ]
	LDR r5, [ r11 , #-568 ]
	LDR r6, [ r11 , #-572 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-576 ]
	@ %88= getelementptr i32,i32* %85 , i32 %87
	mov	r4, 4
	STR r4, [ r11 , #-580 ]
	LDR r5, [ r11 , #-576 ]
	LDR r6, [ r11 , #-580 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-584 ]
	LDR r5, [ r11 , #-564 ]
	LDR r6, [ r11 , #-584 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-588 ]
	@ %89 = load i32, i32* %88
	LDR r7, [ r11 , #-588 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-592 ]
	@ %90 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-596 ]
	@ %91 = srem i32 %90, 30
	mov	r4, #30
	STR r4, [ r11 , #-600 ]
	LDR r6, [ r11 , #-596 ]
	mov	r0, r6
	LDR r6, [ r11 , #-600 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-604 ]
	@ %92= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %91
	mov	r4, 4
	STR r4, [ r11 , #-608 ]
	LDR r5, [ r11 , #-604 ]
	LDR r6, [ r11 , #-608 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-612 ]
	LDR r5, [ r11 , #-176 ]
	LDR r6, [ r11 , #-612 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-616 ]
	@ %93 = load i32, i32* %92
	LDR r7, [ r11 , #-616 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-620 ]
	@ %94 = sdiv i32 %89, %93
	LDR r5, [ r11 , #-592 ]
	LDR r6, [ r11 , #-620 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-624 ]
	@ %95 = srem i32 %94, 2
	mov	r4, #2
	STR r4, [ r11 , #-628 ]
	LDR r6, [ r11 , #-624 ]
	mov	r0, r6
	LDR r6, [ r11 , #-628 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-632 ]
	@ %96= icmp eq i32 %95, 0
	@ br i1 %96, label %97, label %106
	LDR r5, [ r11 , #-632 ]
	cmp r5 , #0
	BEQ	.set9
	B	.set12

.set9:
	@ %98 = load i32, i32* %6
	LDR r7, [ r11 , #-180 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-636 ]
	@ %99= icmp eq i32 %98, 1
	@ br i1 %99, label %100, label %105
	LDR r5, [ r11 , #-636 ]
	cmp r5 , #1
	BEQ	.set10
	B	.set11

.set10:
	@ %101 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-640 ]
	@ %102 = srem i32 %101, 30
	mov	r4, #30
	STR r4, [ r11 , #-644 ]
	LDR r6, [ r11 , #-640 ]
	mov	r0, r6
	LDR r6, [ r11 , #-644 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-648 ]
	@ %103= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %102
	mov	r4, 4
	STR r4, [ r11 , #-652 ]
	LDR r5, [ r11 , #-648 ]
	LDR r6, [ r11 , #-652 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-656 ]
	LDR r5, [ r11 , #-176 ]
	LDR r6, [ r11 , #-656 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-660 ]
	@ %104 = load i32, i32* %103
	LDR r7, [ r11 , #-660 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-664 ]
	@ store i32 %104, i32* %3
	LDR r5, [ r11 , #-664 ]
	LDR r7, [ r11 , #-168 ]
	STR r5, [ r7 ]
	@ br label %105
	B	.set11

.set11:
	@ br label %106
	B	.set12

.set12:
	@ %107 = load i32*, i32** %8
	LDR r7, [ r11 , #-188 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-668 ]
	@ %108 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-672 ]
	@ %109 = sdiv i32 %108, 30
	mov	r4, #30
	STR r4, [ r11 , #-676 ]
	LDR r5, [ r11 , #-672 ]
	LDR r6, [ r11 , #-676 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-680 ]
	@ %110= getelementptr i32,i32* %107 , i32 %109
	mov	r4, 4
	STR r4, [ r11 , #-684 ]
	LDR r5, [ r11 , #-680 ]
	LDR r6, [ r11 , #-684 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-688 ]
	LDR r5, [ r11 , #-668 ]
	LDR r6, [ r11 , #-688 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-692 ]
	@ %111 = load i32, i32* %110
	LDR r7, [ r11 , #-692 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-696 ]
	@ %112 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-700 ]
	@ %113 = srem i32 %112, 30
	mov	r4, #30
	STR r4, [ r11 , #-704 ]
	LDR r6, [ r11 , #-700 ]
	mov	r0, r6
	LDR r6, [ r11 , #-704 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-708 ]
	@ %114= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %113
	mov	r4, 4
	STR r4, [ r11 , #-712 ]
	LDR r5, [ r11 , #-708 ]
	LDR r6, [ r11 , #-712 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-716 ]
	LDR r5, [ r11 , #-176 ]
	LDR r6, [ r11 , #-716 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-720 ]
	@ %115 = load i32, i32* %114
	LDR r7, [ r11 , #-720 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-724 ]
	@ %116 = sdiv i32 %111, %115
	LDR r5, [ r11 , #-696 ]
	LDR r6, [ r11 , #-724 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-728 ]
	@ %117 = srem i32 %116, 2
	mov	r4, #2
	STR r4, [ r11 , #-732 ]
	LDR r6, [ r11 , #-728 ]
	mov	r0, r6
	LDR r6, [ r11 , #-732 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-736 ]
	@ %118= icmp eq i32 %117, 1
	@ br i1 %118, label %119, label %130
	LDR r5, [ r11 , #-736 ]
	cmp r5 , #1
	BEQ	.set13
	B	.set16

.set13:
	@ %120 = load i32, i32* %6
	LDR r7, [ r11 , #-180 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-740 ]
	@ %121= icmp eq i32 %120, 0
	@ br i1 %121, label %122, label %129
	LDR r5, [ r11 , #-740 ]
	cmp r5 , #0
	BEQ	.set14
	B	.set15

.set14:
	@ %123 = load i32, i32* %3
	LDR r7, [ r11 , #-168 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-744 ]
	@ %124 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-748 ]
	@ %125 = srem i32 %124, 30
	mov	r4, #30
	STR r4, [ r11 , #-752 ]
	LDR r6, [ r11 , #-748 ]
	mov	r0, r6
	LDR r6, [ r11 , #-752 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-756 ]
	@ %126= getelementptr [31 x i32],[31 x i32]* %5 , i32 0, i32 %125
	mov	r4, 4
	STR r4, [ r11 , #-760 ]
	LDR r5, [ r11 , #-756 ]
	LDR r6, [ r11 , #-760 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-764 ]
	LDR r5, [ r11 , #-176 ]
	LDR r6, [ r11 , #-764 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-768 ]
	@ %127 = load i32, i32* %126
	LDR r7, [ r11 , #-768 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-772 ]
	@ %128 = sub i32 %123, %127
	LDR r5, [ r11 , #-744 ]
	LDR r6, [ r11 , #-772 ]
	SUB	r4, r5, r6
	STR r4, [ r11 , #-776 ]
	@ store i32 %128, i32* %3
	LDR r5, [ r11 , #-776 ]
	LDR r7, [ r11 , #-168 ]
	STR r5, [ r7 ]
	@ br label %129
	B	.set15

.set15:
	@ br label %130
	B	.set16

.set16:
	@ br label %131
	B	.set17

.set17:
	@ %132 = load i32*, i32** %8
	LDR r7, [ r11 , #-188 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-780 ]
	@ %133 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-784 ]
	@ %134 = sdiv i32 %133, 30
	mov	r4, #30
	STR r4, [ r11 , #-788 ]
	LDR r5, [ r11 , #-784 ]
	LDR r6, [ r11 , #-788 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-792 ]
	@ %135= getelementptr i32,i32* %132 , i32 %134
	mov	r4, 4
	STR r4, [ r11 , #-796 ]
	LDR r5, [ r11 , #-792 ]
	LDR r6, [ r11 , #-796 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-800 ]
	LDR r5, [ r11 , #-780 ]
	LDR r6, [ r11 , #-800 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-804 ]
	@ %136 = load i32*, i32** %8
	LDR r7, [ r11 , #-188 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-808 ]
	@ %137 = load i32, i32* %7
	LDR r7, [ r11 , #-184 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-812 ]
	@ %138 = sdiv i32 %137, 30
	mov	r4, #30
	STR r4, [ r11 , #-816 ]
	LDR r5, [ r11 , #-812 ]
	LDR r6, [ r11 , #-816 ]
	SDIV	r4, r5, r6
	STR r4, [ r11 , #-820 ]
	@ %139= getelementptr i32,i32* %136 , i32 %138
	mov	r4, 4
	STR r4, [ r11 , #-824 ]
	LDR r5, [ r11 , #-820 ]
	LDR r6, [ r11 , #-824 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-828 ]
	LDR r5, [ r11 , #-808 ]
	LDR r6, [ r11 , #-828 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-832 ]
	@ %140 = load i32, i32* %139
	LDR r7, [ r11 , #-832 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-836 ]
	@ %141 = load i32, i32* %3
	LDR r7, [ r11 , #-168 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-840 ]
	@ %142 = add i32 %140, %141
	LDR r5, [ r11 , #-836 ]
	LDR r6, [ r11 , #-840 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-844 ]
	@ store i32 %142, i32* %135
	LDR r5, [ r11 , #-844 ]
	LDR r7, [ r11 , #-804 ]
	STR r5, [ r7 ]
	@ store i32 0, i32* ret
	mov	r4, #0
	STR r4, [ r11 , #-848 ]
	LDR r5, [ r11 , #-848 ]
	LDR r7, [ r11 , #-192 ]
	STR r5, [ r7 ]
	@ br label %ret
	B	.set18

.set18:
	@ %143 = load i32, i32* ret
	LDR r7, [ r11 , #-192 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-852 ]
	@ ret i32 %143
	LDR r6, [ r11 , #-852 ]
	mov	r0, r6
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
.rand19:
	Push { LR }
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #120
	@ ret = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #8
	STR r4, [ r11 , #-12 ]
	@ %0 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-16 ]
	LDR r7, [ r11 , #-16 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-20 ]
	@ %1= getelementptr [3 x i32],[3 x i32]* @seed , i32 0, i32 0
	movw	r4, #:lower16:seed
	movt	r4, #:upper16:seed

	STR r4, [ r11 , #-24 ]
	@ %2 = load i32, i32* %1
	LDR r7, [ r11 , #-24 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-28 ]
	@ %3 = mul i32 %0, %2
	LDR r5, [ r11 , #-20 ]
	LDR r6, [ r11 , #-28 ]
	MUL	r4, r5, r6
	STR r4, [ r11 , #-32 ]
	@ %4= getelementptr [3 x i32],[3 x i32]* @seed , i32 0, i32 1
	movw	r4, #:lower16:seed
	movt	r4, #:upper16:seed

	STR r4, [ r11 , #-36 ]
	LDR r5, [ r11 , #-36 ]
	ADD	r4, r5, #4
	STR r4, [ r11 , #-40 ]
	@ %5 = load i32, i32* %4
	LDR r7, [ r11 , #-40 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-44 ]
	@ %6 = add i32 %3, %5
	LDR r5, [ r11 , #-32 ]
	LDR r6, [ r11 , #-44 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-48 ]
	@ store i32 %6, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-52 ]
	LDR r5, [ r11 , #-48 ]
	LDR r7, [ r11 , #-52 ]
	STR r5, [ r7 ]
	@ %7 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-56 ]
	LDR r7, [ r11 , #-56 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-60 ]
	@ %8= getelementptr [3 x i32],[3 x i32]* @seed , i32 0, i32 2
	movw	r4, #:lower16:seed
	movt	r4, #:upper16:seed

	STR r4, [ r11 , #-64 ]
	LDR r5, [ r11 , #-64 ]
	ADD	r4, r5, #8
	STR r4, [ r11 , #-68 ]
	@ %9 = load i32, i32* %8
	LDR r7, [ r11 , #-68 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-72 ]
	@ %10 = srem i32 %7, %9
	LDR r6, [ r11 , #-60 ]
	mov	r0, r6
	LDR r6, [ r11 , #-72 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-76 ]
	@ store i32 %10, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-80 ]
	LDR r5, [ r11 , #-76 ]
	LDR r7, [ r11 , #-80 ]
	STR r5, [ r7 ]
	@ %11 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-84 ]
	LDR r7, [ r11 , #-84 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-88 ]
	@ %12= icmp slt i32 %11, 0
	@ br i1 %12, label %13, label %18
	LDR r5, [ r11 , #-88 ]
	cmp r5 , #0
	BLT	.rand20
	B	.rand21

.rand20:
	@ %14= getelementptr [3 x i32],[3 x i32]* @seed , i32 0, i32 2
	movw	r4, #:lower16:seed
	movt	r4, #:upper16:seed

	STR r4, [ r11 , #-92 ]
	LDR r5, [ r11 , #-92 ]
	ADD	r4, r5, #8
	STR r4, [ r11 , #-96 ]
	@ %15 = load i32, i32* %14
	LDR r7, [ r11 , #-96 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-100 ]
	@ %16 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-104 ]
	LDR r7, [ r11 , #-104 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-108 ]
	@ %17 = add i32 %15, %16
	LDR r5, [ r11 , #-100 ]
	LDR r6, [ r11 , #-108 ]
	ADD	r4, r5, r6
	STR r4, [ r11 , #-112 ]
	@ store i32 %17, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-116 ]
	LDR r5, [ r11 , #-112 ]
	LDR r7, [ r11 , #-116 ]
	STR r5, [ r7 ]
	@ br label %18
	B	.rand21

.rand21:
	@ %19 = load i32, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-120 ]
	LDR r7, [ r11 , #-120 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-124 ]
	@ store i32 %19, i32* ret
	LDR r5, [ r11 , #-124 ]
	LDR r7, [ r11 , #-12 ]
	STR r5, [ r7 ]
	@ br label %ret
	B	.rand22

.rand22:
	@ %20 = load i32, i32* ret
	LDR r7, [ r11 , #-12 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-128 ]
	@ ret i32 %20
	LDR r6, [ r11 , #-128 ]
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
.main23:
	Push { LR }
	Push { r11 }
	ADD	r11, SP, #4
	SUB	SP, SP, #100
	@ %0 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #8
	STR r4, [ r11 , #-24 ]
	@ %1 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #12
	STR r4, [ r11 , #-28 ]
	@ %2 = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #16
	STR r4, [ r11 , #-32 ]
	@ ret = alloca i32
	SUB	SP, SP, #4
	SUB	r4, r11, #20
	STR r4, [ r11 , #-36 ]
	@ %3 = call i32 @getint()
	BLX	getint
	mov	r4, r0
	STR r4, [ r11 , #-40 ]
	@ store i32 %3, i32* %2
	LDR r5, [ r11 , #-40 ]
	LDR r7, [ r11 , #-32 ]
	STR r5, [ r7 ]
	@ %4 = call i32 @getint()
	BLX	getint
	mov	r4, r0
	STR r4, [ r11 , #-44 ]
	@ store i32 %4, i32* @staticvalue
	movw	r4, #:lower16:staticvalue
	movt	r4, #:upper16:staticvalue

	STR r4, [ r11 , #-48 ]
	LDR r5, [ r11 , #-44 ]
	LDR r7, [ r11 , #-48 ]
	STR r5, [ r7 ]
	@ br label %5
	B	.main24

.main24:
	@ %6 = load i32, i32* %2
	LDR r7, [ r11 , #-32 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-52 ]
	@ %7= icmp sgt i32 %6, 0
	@ br i1 %7, label %8, label %20
	LDR r5, [ r11 , #-52 ]
	cmp r5 , #0
	BGT	.main25
	B	.main27

.main25:
	@ %9 = load i32, i32* %2
	LDR r7, [ r11 , #-32 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-56 ]
	@ %10 = sub i32 %9, 1
	LDR r5, [ r11 , #-56 ]
	SUB	r4, r5, #1
	STR r4, [ r11 , #-60 ]
	@ store i32 %10, i32* %2
	LDR r5, [ r11 , #-60 ]
	LDR r7, [ r11 , #-32 ]
	STR r5, [ r7 ]
	@ %11 = call i32 @rand()
	BLX	rand
	mov	r4, r0
	STR r4, [ r11 , #-64 ]
	@ %12 = srem i32 %11, 300000
	movw	r4, 37856
	movt	r4, 4

	STR r4, [ r11 , #-68 ]
	LDR r6, [ r11 , #-64 ]
	mov	r0, r6
	LDR r6, [ r11 , #-68 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-72 ]
	@ store i32 %12, i32* %1
	LDR r5, [ r11 , #-72 ]
	LDR r7, [ r11 , #-28 ]
	STR r5, [ r7 ]
	@ %13 = call i32 @rand()
	BLX	rand
	mov	r4, r0
	STR r4, [ r11 , #-76 ]
	@ %14 = srem i32 %13, 2
	mov	r4, #2
	STR r4, [ r11 , #-80 ]
	LDR r6, [ r11 , #-76 ]
	mov	r0, r6
	LDR r6, [ r11 , #-80 ]
	mov	r1, r6
	BLX	__aeabi_idivmod
	mov	r4, r1
	STR r4, [ r11 , #-84 ]
	@ store i32 %14, i32* %0
	LDR r5, [ r11 , #-84 ]
	LDR r7, [ r11 , #-24 ]
	STR r5, [ r7 ]
	@ %15= getelementptr [10000 x i32],[10000 x i32]* @a , i32 0, i32 0
	movw	r4, #:lower16:a
	movt	r4, #:upper16:a

	STR r4, [ r11 , #-88 ]
	@ %16 = load i32, i32* %1
	LDR r7, [ r11 , #-28 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-92 ]
	@ %17 = load i32, i32* %0
	LDR r7, [ r11 , #-24 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-96 ]
	@ %18 = call i32 @set(i32* %15,i32 %16,i32 %17)
	LDR r6, [ r11 , #-96 ]
	mov	r2, r6
	LDR r6, [ r11 , #-92 ]
	mov	r1, r6
	LDR r6, [ r11 , #-88 ]
	mov	r0, r6
	BLX	set
	mov	r4, r0
	STR r4, [ r11 , #-100 ]
	@ br label %19
	B	.main26

.main26:
	@ br label %5
	B	.main24

.main27:
	@ %21= getelementptr [10000 x i32],[10000 x i32]* @a , i32 0, i32 0
	movw	r4, #:lower16:a
	movt	r4, #:upper16:a

	STR r4, [ r11 , #-104 ]
	@ call void @putarray(i32 10000,i32* %21)
	LDR r6, [ r11 , #-104 ]
	mov	r1, r6
	movw	r4, 10000
	movt	r4, 0

	STR r4, [ r11 , #-108 ]
	LDR r6, [ r11 , #-108 ]
	mov	r0, r6
	BLX	putarray
	mov	r4, r0
	STR r4, [ r11 , #-112 ]
	@ store i32 0, i32* ret
	mov	r4, #0
	STR r4, [ r11 , #-116 ]
	LDR r5, [ r11 , #-116 ]
	LDR r7, [ r11 , #-36 ]
	STR r5, [ r7 ]
	@ br label %ret
	B	.main28

.main28:
	@ %22 = load i32, i32* ret
	LDR r7, [ r11 , #-36 ]
	LDR r4, [ r7 ]
	STR r4, [ r11 , #-120 ]
	@ ret i32 %22
	LDR r6, [ r11 , #-120 ]
	mov	r0, r6
	SUB	SP, r11, #4
	Pop { r11 }
	Pop { PC }

	.size	main, .-main

