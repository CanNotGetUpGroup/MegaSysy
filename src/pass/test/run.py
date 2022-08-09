import os

# testcase="D:\JavaProject\MegaSysy\oldtestcases"
testcase = "C:\\Users\\14947.LAPTOP-M038335P\\Documents\\CodeField\\Compiler\\MegaSysy\\FrontedTest\\testcase"
out_directory = "C:\\Users\\14947.LAPTOP-M038335P\\Documents\\CodeField\\Compiler\\MegaSysy\\FrontedTest\\pass"
classpath = "C:\\Users\\14947.LAPTOP-M038335P\Documents\CodeField\Compiler\MegaSysy\out\production\MegaSysy;C:\\Users\\14947.LAPTOP-M038335P\\Documents\\CodeField\\Compiler\\MegaSysy\lib\\antlr-4.8-complete.jar"

functional_testcase = testcase+"\\functional\\"
functional_out_pass = out_directory+"\\functional\\"
functional_file_list = []

for file in os.listdir(functional_testcase):
    if os.path.splitext(file)[1] == ".c":
        functional_file_list.append(os.path.splitext(file)[0])

for file in functional_file_list:
    print(functional_testcase+file+".c:")
    a = os.system("java -classpath "+classpath+" pass.test.PassTest " +
                  functional_testcase+file+".c "+functional_out_pass+file+".ll")

performance_testcase = testcase+"\\performance\\"
performance_out_pass = out_directory+"\\performance\\"
performance_file_list = []

for file in os.listdir(performance_testcase):
    if os.path.splitext(file)[1] == ".c":
        performance_file_list.append(os.path.splitext(file)[0])

for file in performance_file_list:
    print(performance_testcase+file+".c:")
    a = os.system("java -classpath "+classpath+" pass.test.PassTest " +
                  performance_testcase+file+".c "+performance_out_pass+file+".ll")

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
