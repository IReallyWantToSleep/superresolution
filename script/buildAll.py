#虽然我可以直接写一个gradle任务，但是我闲，所以我拿python写
###############################设置###############################
ENABLE_GRADLE_OUTPUT = True #是否显示gradle的输出
ENABLE_GRADLE_OUTPUT_INFO = False #是否在gradle的命令行加入--info <-显示致死量日志
OUTPUT_DIR = "build_jars" #输出目录
VERSIOM_CONFIGS_DIR = "versionConfigs" #版本配置文件目录
#################################################################
import os
import re
import math
import sys
import shutil
import time
import subprocess
import io
from pathlib import Path
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
cur_path = Path(os.path.abspath('.'))
version_configs_path = cur_path / VERSIOM_CONFIGS_DIR
version_files = os.listdir(version_configs_path)
versions = []
__gradle_cmdline = ""
__gradle_args = ""
if not ENABLE_GRADLE_OUTPUT: 
    __gradle_cmdline = " > " + os.devnull 
if ENABLE_GRADLE_OUTPUT_INFO:
    __gradle_args = "--info"

java_home = os.environ.get('JAVA_HOME')
def find_java():
    global java_home
    if (len(sys.argv) > 1):
        path = Path(sys.argv[1])
        if (os.path.exists(path)):
            if os.path.isdir(path):
                if os.path.isfile(path/"bin"/"java"):
                    java_home = path
                else:
                    print(f'Java路径/程序路径/??? {path} 不可用,憋憋憋')
                    exit(1)
            else:
                if os.path.isfile(path):
                    java_home = str(path.parent.parent)
                else:
                    print(f'Java路径/程序路径/??? {path} 不可用,憋憋憋')
                    exit(1)
        else:
            print(f'Java路径/程序路径/??? {path} 不存在,憋憋憋')
            exit(1)
    if java_home == None or java_home == "":
        print('Java不可用,憋憋憋')
        exit(1)
find_java()
java_exe = java_home + '/bin/java'
for version_file in version_files:
    if version_file.endswith('.properties'):
        versions.append(version_file.split('.properties')[0])
def get_java_version():
    cmd = f'{java_exe} -version'
    p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = p.communicate()
    version_info = err.decode('ascii')
    match = re.search(r'\"(\d+\.\d+|\d+)', version_info)
    if match:
        version_code = eval(match.group(1))
        _ver_a, _ver_b = math.modf(version_code)
        if _ver_a > 1:
            return int(_ver_a)
        else:
            return int(_ver_b)
    return -1
java_version = get_java_version()
def build_gradle_cmd(java_exe, task, arg):
    classpath = cur_path / 'gradle' / 'wrapper' / 'gradle-wrapper.jar'
    cmd = [
        java_exe,
        '-classpath',
        str(classpath),
        'org.gradle.wrapper.GradleWrapperMain',
        task,
        arg,
        __gradle_args
    ]
    
    return cmd
def parse_version_config(version:str):
    path = version_configs_path / f'{version}.properties'
    text = ""
    data = {}
    with open(path,"r",encoding="utf8") as f:
        text = f.read()
    lines = text.split("\n")
    line_index = 1
    for line in lines:
        line = re.sub(r'#.*','',line)
        line = line.strip()
        if (len(line) < 2):
            continue
        try:
            key = line.split("=")[0]
            if len(line.split("=")) == 1:
                value = None
            else:
                value = line.split("=")[1]
            if ("," in value):
                value = value.split(",")
            data.update({key:value})
        except:
            print(f'{path} 无法解析 line:{line_index} {lines[line_index-1]}')
        line_index += 1
    return data
version_configs = {}
for version in versions:
    version_configs.update({version:parse_version_config(version)})
def check_java_version():
    for version in version_configs.keys():
        if (eval(version_configs[version]['java_version']) > java_version):
            print(f'Java版本过低 {version}需要Java版本{version_configs[version]["java_version"]} 憋憋憋')
            print(f'可使用 `{os.path.abspath(__file__)} Java可执行程序位置/Java目录` 来指定要使用的java')
            exit(1)

def call_gradle_task(task:str,arg:str):
    cmd = build_gradle_cmd(java_exe,task,arg)
    print("执行命令"," ".join(cmd))
    code = os.system(" ".join(cmd) + __gradle_cmdline)#subprocess.call(cmd,cwd=str(cur_path), shell=True,encoding="utf8")
    return code == 0

def call_gradle_task_with_log(log:str,task:str,arg:str):
    print(log+'...')
    if (not call_gradle_task(task,arg)):
        print(log+"失败")
def should_copy(name:str):
    if name.endswith("dev-shadow.jar"):
        return False
    if name.endswith("sources.jar"):
        return False
    return True

def copy_build_libs(platform:str):
    path = cur_path / platform / "build" / "libs"
    if (not os.path.isdir(path)):
        print("在复制",path,"时出现错误")
    try:
        for file in os.listdir(path):
            if (os.path.isfile(path / file)):
                if (should_copy(file)):
                    shutil.copy(path / file, output_dir)
    except:
        print("在复制",path,"时出现错误")
    
output_dir = cur_path / OUTPUT_DIR
print('当前Java版本',java_version,"JAVA_HOMO",java_home,"<--HOMO是故意的")
check_java_version()
print('将为这些游戏版本构建模组',", ".join(versions))
print('当前目录',cur_path)
print('输出目录',output_dir)
if (os.path.exists(output_dir)):
    shutil.rmtree(output_dir)
os.mkdir(output_dir)

call_gradle_task_with_log("初始化环境","","")
startTime = time.time()
for version in versions:
    print("构建",version,"的模组中...")
    print("包含的加载器",", ".join(version_configs[version]["platforms"]))
    call_gradle_task_with_log("清理环境","clean","")
    call_gradle_task_with_log("构建模组","build","-Pminecraft_version="+version)
    for platform in version_configs[version]["platforms"]:
        copy_build_libs(platform)
endTime = time.time()

print('已为这些游戏版本构建模组',", ".join(versions),"用时",str(round(endTime-startTime,2))+"s")
