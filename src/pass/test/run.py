import os

# testcase="D:\JavaProject\MegaSysy\oldtestcases"
testcase="D:\JavaProject\MegaSysy\\testcase"
out_directory="E:\VMshare\\bisai\pass"
classpath="D:\JavaProject\MegaSysy\out\production\MegaSysy;D:\JavaProject\MegaSysy\lib\\antlr-4.10.1-complete.jar"

functional_testcase=testcase+"\\functional\\"
functional_out_pass=out_directory+"\\functional\\"
functional_file_list=[]

for file in os.listdir(functional_testcase):
    if os.path.splitext(file)[1] == ".sy":
        functional_file_list.append(os.path.splitext(file)[0])

for file in functional_file_list:
    print(functional_testcase+file+".sy:")
    a=os.system("java -classpath "+classpath+" pass.test.PassTest "+functional_testcase+file+".sy "+functional_out_pass+file+".ll")

performance_testcase=testcase+"\\performance\\"
performance_out_pass=out_directory+"\\performance\\"
performance_file_list=[]

for file in os.listdir(performance_testcase):
    if os.path.splitext(file)[1] == ".sy":
        performance_file_list.append(os.path.splitext(file)[0])

for file in performance_file_list:
    print(performance_testcase+file+".sy:")
    a=os.system("java -classpath "+classpath+" pass.test.PassTest "+performance_testcase+file+".sy "+performance_out_pass+file+".ll")

# thu_testcase=testcase+"\\thu_test\\"
# thu_out=out_directory+"\\thu_test\\"
# thu_file_list=[]
#
# for file in os.listdir(thu_testcase):
#     if os.path.splitext(file)[1] == ".sy":
#         thu_file_list.append(os.path.splitext(file)[0])
#
# for file in thu_file_list:
#     print(thu_testcase+file+".sy:")
#     a=os.system("java -classpath "+classpath+" frontend.test.FrontTest "+thu_testcase+file+".sy "+thu_out+file+".ll")

