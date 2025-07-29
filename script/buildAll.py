# 虽然我可以直接写一个gradle任务，但是我闲，所以我拿python写
############################### 设置 ###############################
ENABLE_GRADLE_OUTPUT = True  # 是否显示gradle的输出
ENABLE_GRADLE_OUTPUT_INFO = False  # 是否在gradle的命令行加入--info <-显示致死量日志
OUTPUT_DIR = "build_jars"  # 输出目录
VERSION_CONFIGS_DIR = "configs"  # 版本配置目录
#################################################################
import os
import re
import json
import sys
import shutil
import time
import subprocess
from pathlib import Path
from typing import Dict, List, Optional

sys.stdout.reconfigure(encoding='utf-8')
cur_path = Path.cwd()
version_configs_path = cur_path / VERSION_CONFIGS_DIR
output_dir = cur_path / OUTPUT_DIR
_gradle_args: List[str] = ["--info"] if ENABLE_GRADLE_OUTPUT_INFO else []
_gradle_output = subprocess.DEVNULL if not ENABLE_GRADLE_OUTPUT else None

class JavaFinder:
    def __init__(self):
        self.java_home = os.environ.get('JAVA_HOME')
        self.java_exe = None
    
    def validate_java_path(self, path: Path) -> Optional[Path]:
        exe_path = None
        if path.is_dir():
            exe_path = path / "bin" / "java"
            if not exe_path.exists():
                return None
            return exe_path.parent.parent
        
        if path.name.startswith("java") and path.exists():
            return path.parent.parent
        
        return None
    
    def find(self):
        if len(sys.argv) > 1:
            user_path = Path(sys.argv[1]).resolve()
            if (home := self.validate_java_path(user_path)) is not None:
                self.java_home = home
        if not self.java_home:
            print('错误: 未找到JAVA_HOME环境变量')
            sys.exit(1)
        self.java_exe = Path(self.java_home) / "bin" / "java"
        if sys.platform.startswith('win'):
            self.java_exe = self.java_exe.with_suffix('.exe')
        
        if not self.java_exe.exists():
            print(f'错误: Java可执行文件不存在 - {self.java_exe}')
            sys.exit(1)

class VersionParser:
    @staticmethod
    def parse(file_path: Path) -> dict:
        config = {}
        with open(file_path, 'r', encoding='utf-8') as f:
            config = json.loads(f.read())
        return config

def get_java_version(java_exe: Path) -> int:
    result = subprocess.run(
        [str(java_exe), '-version'],
        stderr=subprocess.PIPE,
        stdout=subprocess.DEVNULL,
        encoding='utf-8',
        errors='replace'
    )
    
    version_match = re.search(r'version "(\d+)[\.\d]*"', result.stderr)
    if not version_match:
        print('错误: 无法识别Java版本')
        return -1
    
    return int(version_match.group(1))

def build_gradle_command(java_exe: Path, task: str, arg: str) -> List[str]:
    classpath = cur_path / 'gradle' / 'wrapper' / 'gradle-wrapper.jar'
    cmd = [
        str(java_exe),
        '-classpath', str(classpath),
        'org.gradle.wrapper.GradleWrapperMain',
        task
    ]
    
    if arg.strip():
        cmd.append(arg)
    
    cmd.extend(_gradle_args)
    return cmd

def call_gradle_task(task: str, arg: str = "") -> bool:
    cmd = build_gradle_command(java.java_exe, task, arg)
    print(f"[执行] {' '.join(cmd)}")
    
    try:
        result = subprocess.run(
            cmd,
            cwd=cur_path,
            stdout=_gradle_output,
            stderr=subprocess.STDOUT,
            check=True,
            encoding='utf-8',
            errors='replace'
        )
        return True
    except subprocess.CalledProcessError as e:
        print(f"任务执行失败: {e.cmd}")
        if e.output:
            print("错误输出:\n", e.output)
        return False

def copy_build_libs(platform: str) -> None:
    libs_dir = cur_path / platform / "build" / "libs"
    if not libs_dir.exists():
        print(f"警告: 构建目录不存在 - {libs_dir}")
        return
    
    for file in libs_dir.iterdir():
        if file.is_file() and should_copy(file.name):
            try:
                shutil.copy2(file, output_dir)
                print(f"已复制: {file.name}")
            except Exception as e:
                print(f"复制失败 {file.name}: {e}")

def should_copy(filename: str) -> bool:
    return all((
        not filename.endswith("dev-shadow.jar"),
        not filename.endswith("sources.jar"),
        not filename.endswith("javadoc.jar"),
        filename.endswith(".jar")
    ))

if __name__ == "__main__":
    java = JavaFinder()
    java.find()
    java_version = get_java_version(java.java_exe)
    print(f"当前Java版本: {java_version}, 路径: {java.java_exe}")

    version_configs = {}
    for config_file in version_configs_path.glob("*.json"):
        version_name = config_file.stem
        try:
            config = VersionParser.parse(config_file)
            version_configs[version_name] = config
            print(f"已加载配置: {config_file} -> {config['common']['minecraft_version']}")
        except Exception as e:
            print(f"加载配置失败 {config_file.name}: {e}")
            sys.exit(1)
    
    for ver, config in version_configs.items():
        required_ver = int(config["common"].get('java_version', 17))
        if required_ver > java_version:
            print(f"错误: {ver} 需要Java {required_ver}+ (当前 {java_version})")
            sys.exit(1)

    if output_dir.exists():
        shutil.rmtree(output_dir, ignore_errors=True)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    start_time = time.time()
    print(f"\n开始构建 {len(version_configs)} 个版本:")
    #call_gradle_task("native:buildWin")
    for version, config in version_configs.items():
        try :
            if config["skip_build"]:
                print(f"跳过构建: {version}")
                continue
        except KeyError:
            pass
        print(f"\n=== 正在构建 {version} ===")
        print("目标加载器:", ", ".join(config["common"]["platforms"]))
        
        if not call_gradle_task("clean"):
            print(f"清理环境失败，跳过清理")
        
        build_args = f"-Pminecraft_version_config={version}"
        if not call_gradle_task("build", build_args):
            print(f"构建 {version} 失败")
            continue
        
        print("\n复制构建产物:")
        for platform in config["common"]["platforms"]:
            copy_build_libs(platform.strip())
    
    total_time = time.time() - start_time
    print(f"\n构建完成! 总耗时: {total_time:.2f}s")
    print(f"输出目录: {output_dir.resolve()}")