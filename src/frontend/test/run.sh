#!/bin/bash
# 没有参数或第一个参数为0表示性能和功能测试都执行，1功能，2性能

testcase="$(pwd)/testcase"
#out_directory="$(pwd)/LL"
sysy_directory="$(pwd)/sysyLL"
error_directory="$(pwd)/error"

functional_testcase=$testcase"/functional/"
functional_out=$sysy_directory"/functional/"
functional_error=$error_directory"/functional/"
performance_testcase=$testcase"/performance/"
performance_out=$sysy_directory"/performance/"
performance_error=$error_directory"/performance/"

if [[ $# == 0 ]] || [[ $1 == 0 ]] || [[ $1 == 1 ]];then

    file=("$(find "$functional_testcase" -type f)")
    program=""
    input=""
    output=""
    num=0
    all=0
    rm -rf $functional_error*

    for i in ${file[*]}
    do
        if [ "${i##*.}"x = "c"x ];then

            program=$(echo $(basename $i .c))
            llvm-link $functional_out$program".ll" lib.ll -o out.ll

        elif [ "${i##*.}"x = "in"x ];then

            input=$(cat $i)

        elif [ "${i##*.}"x = "out"x ];then

            echo "$input"|lli out.ll > $functional_out$program"_ll.out" 2>/dev/null #注意echo "$input"要加双引号，否则input中的'*'会被当做当前目录内容来输出
            ret=$?
            if [ $(tail -n1 $functional_out$program"_ll.out" | wc -l) -eq 0 ];then
                if [ -s $functional_out$program"_ll.out" ];then
                    echo '' >> $functional_out$program"_ll.out"
                fi
            fi
            echo $ret >> $functional_out$program"_ll.out"
            input=""
            diff -b $functional_out$program"_ll.out"  $i > /dev/null
            if [ $? != 0 ];then
                mkdir $functional_error$program
                cp $functional_testcase$program".c" $functional_error$program
                cp $functional_testcase$program".in" $functional_error$program
                cp $functional_out$program"_ll.out" $functional_error$program
                cp $i $functional_error$program
                cp $functional_out$program".ll" $functional_error$program
                echo "-----"
                echo "source: "$functional_testcase$program".c"
                echo "ans: "$i
                # echo "-----"
                # cat $i
                # echo "-----"
                echo "output: "$functional_out$program"_ll.out"
                # echo "-----"
                # cat $functional_out$program"_ll.out"
                # echo "-----"
                echo "sysy: "$functional_out$program".ll"
                echo ""
                let "num++"
            fi
            let "all++"
        fi
    done
    echo "functional error:"$num
    echo "functional total:"$all

fi

if [[ $# == 0 ]] || [[ $1 == 0 ]] || [[ $1 == 2 ]];then

    file=("$(find "$performance_testcase" -type f)")
    program=""
    input=""
    output=""
    num=0
    all=0
    rm -rf $performance_error*
    currTime=$(date +"%Y-%m-%d %T")
    timeInfo="$(pwd)/timer/timeInfo"
    echo $currTime > $timeInfo

    for i in ${file[*]}
    do
        if [ "${i##*.}"x = "c"x ];then

            program=$(echo $(basename $i .c))
            llvm-link $performance_out$program".ll" lib.ll -o out.ll

        elif [ "${i##*.}"x = "in"x ];then

            input=$(cat $i)

        elif [ "${i##*.}"x = "out"x ];then

            echo "-----$program-----">>$timeInfo
            echo "$input"|lli out.ll > $performance_out$program"_ll.out" 2>>$timeInfo
            echo "">>$timeInfo
            ret=$?
            if [ $(tail -n1 $performance_out$program"_ll.out" | wc -l) -eq 0 ];then
                if [ -s $performance_out$program"_ll.out" ];then
                    echo '' >> $performance_out$program"_ll.out"
                fi
            fi
            echo $ret >> $performance_out$program"_ll.out"
            input=""
            diff -b $performance_out$program"_ll.out"  $i > /dev/null
            if [ $? != 0 ];then
                mkdir $performance_error$program
                cp $performance_testcase$program".c" $performance_error$program
                cp $performance_testcase$program".in" $performance_error$program
                cp $performance_out$program"_ll.out" $performance_error$program
                cp $i $performance_error$program
                cp $performance_out$program".ll" $performance_error$program
                echo "-----"
                echo "source: "$performance_testcase$program".c"
                echo "ans: "$i
                echo "output: "$performance_out$program"_ll.out"
                echo "sysy: "$performance_out$program".ll"
                echo ""
                let "num++"
            fi
            let "all++"
        fi
    done
    echo "performance error:"$num
    echo "performance total:"$all

fi