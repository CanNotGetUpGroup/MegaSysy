import os

# testcase="D:/JavaProject/MegaSysy/oldtestcases"
abspath=os.path.abspath(os.path.join(os.getcwd(), "..")) # D:/JavaProject/MegaSysy
testcase=abspath+"/testcase"
out_directory=abspath+"/MC"
classpath=abspath+"/out/production/MegaSysy;"+abspath+"/lib/antlr-4.8-complete.jar"
arg=0
if len(sys.argv) == 1:
    arg=int(sys.argv[1])

functional_testcase=testcase+"/functional/"
functional_out_pass=out_directory+"/functional/"
functional_file_list=[]

if arg == 1 or arg == 0:
    for file in os.listdir(functional_testcase):
        if os.path.splitext(file)[1] == ".sy":
            functional_file_list.append(os.path.splitext(file)[0])

    for file in functional_file_list:
        print(functional_testcase+file+".sy:")
        a=os.system("java -classpath "+classpath+" Compiler -S -o "+functional_out_pass+file+".s "+functional_testcase+file+".sy -O2")

performance_testcase=testcase+"/performance/"
performance_out_pass=out_directory+"/performance/"
performance_file_list=[]

if arg == 2 or arg == 0:
    for file in os.listdir(performance_testcase):
        if os.path.splitext(file)[1] == ".sy":
            performance_file_list.append(os.path.splitext(file)[0])

    for file in performance_file_list:
        print(performance_testcase+file+".sy:")
        a=os.system("java -classpath "+classpath+" Compiler -S -o "+performance_out_pass+file+".s "+performance_testcase+file+".sy -O2")

# thu_testcase=testcase+"/thu_test/"
# thu_out=out_directory+"/thu_test/"
# thu_file_list=[]
#
# for file in os.listdir(thu_testcase):
#     if os.path.splitext(file)[1] == ".sy":
#         thu_file_list.append(os.path.splitext(file)[0])
#
# for file in thu_file_list:
#     print(thu_testcase+file+".sy:")
#     a=os.system("java -classpath "+classpath+" frontend.test.FrontTest "+thu_testcase+file+".sy "+thu_out+file+".ll")

