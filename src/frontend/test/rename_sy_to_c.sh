#!/bin/bash
testcase="$(pwd)/testcase"
functional_testcase=$testcase"/functional/"
performance_testcase=$testcase"/performance/"

file=("$(find "$functional_testcase" -type f)")

for i in ${file[*]}
do    
    if [ "${i##*.}"x = "sy"x ];then

        echo $i
        program=$(echo $(basename $i .sy))
        mv $i $functional_testcase$program".c"

    fi
done

file=("$(find "$performance_testcase" -type f)")

for i in ${file[*]}
do    
    if [ "${i##*.}"x = "sy"x ];then

        echo $i
        program=$(echo $(basename $i .sy))
        mv $i $performance_testcase$program".c"

    fi
done

