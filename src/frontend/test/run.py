import os

# testcase="D:\JavaProject\MegaSysy\oldtestcases"
testcase="D:\JavaProject\MegaSysy\\testcase"
out_directory="E:\VMshare\\bisai\sysyLL"
classpath="D:\JavaProject\MegaSysy\out\production\MegaSysy;D:\JavaProject\MegaSysy\lib\\antlr-4.9.3-complete.jar"

functional_testcase=testcase+"\\functional\\"
functional_out=out_directory+"\\functional\\"
functional_file_list=[]

for file in os.listdir(functional_testcase):
    if os.path.splitext(file)[1] == ".sy":
        functional_file_list.append(os.path.splitext(file)[0])

for file in functional_file_list:
    print(functional_testcase+file+".sy:")
    a=os.system("java -classpath "+classpath+" frontend.test.FrontTest "+functional_testcase+file+".sy "+functional_out+file+".ll")

performance_testcase=testcase+"\\performance\\"
performance_out=out_directory+"\\performance\\"
performance_file_list=[]

for file in os.listdir(performance_testcase):
    if os.path.splitext(file)[1] == ".sy":
        performance_file_list.append(os.path.splitext(file)[0])

for file in performance_file_list:
    print(performance_testcase+file+".sy:")
    a=os.system("java -classpath "+classpath+" frontend.test.FrontTest "+performance_testcase+file+".sy "+performance_out+file+".ll")

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

