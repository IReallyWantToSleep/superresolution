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
import shutil as shell_shutil
from pathlib import Path
from typing import Dict, List, Optional

sys.stdout.reconfigure(encoding='utf-8')
script_path = Path(__file__).resolve()
cur_path = script_path.parent.parent
version_configs_path = cur_path / VERSION_CONFIGS_DIR
output_dir = cur_path / OUTPUT_DIR
_gradle_args: List[str] = ["--info"] if ENABLE_GRADLE_OUTPUT_INFO else []
_gradle_output = subprocess.DEVNULL if not ENABLE_GRADLE_OUTPUT else None

print("GRADLE_HOME: ", os.environ.get('GRADLE_HOME', 'Not set').strip())

def resolve_gradle_user_home() -> Path:
    env_user_home = os.environ.get('GRADLE_USER_HOME')
    if env_user_home:
        return Path(env_user_home).expanduser().resolve()

    default_home = Path.home() / '.gradle'
    try:
        default_home.mkdir(parents=True, exist_ok=True)
        if os.access(default_home, os.W_OK):
            return default_home
    except Exception:
        pass

    fallback_home = cur_path / '.gradle-user-home'
    fallback_home.mkdir(parents=True, exist_ok=True)
    return fallback_home.resolve()

gradle_user_home = resolve_gradle_user_home()
os.environ['GRADLE_USER_HOME'] = str(gradle_user_home)
print("GRADLE_USER_HOME:", gradle_user_home)

class JavaFinder:
    def __init__(self):
        self.java_home = os.environ.get('JAVA_HOME')
        self.java_exe = None

    @staticmethod
    def _is_java_bin(path: Path) -> bool:
        lowered = path.name.lower()
        return lowered == "java" or lowered == "java.exe"

    def _find_from_path(self) -> Optional[Path]:
        java_cmd = shell_shutil.which("java")
        if java_cmd:
            resolved = Path(java_cmd).resolve()
            home = self.validate_java_path(resolved)
            if home is not None:
                return home

        javac_cmd = shell_shutil.which("javac")
        if javac_cmd:
            resolved = Path(javac_cmd).resolve()
            if resolved.parent.name == "bin":
                candidate_home = resolved.parent.parent
                if self.validate_java_path(candidate_home) is not None:
                    return candidate_home
        return None
    
    def validate_java_path(self, path: Path) -> Optional[Path]:
        exe_path = None
        if path.is_dir():
            exe_path = path / "bin" / "java"
            if sys.platform.startswith('win'):
                exe_path = exe_path.with_suffix('.exe')
            if not exe_path.exists():
                return None
            return exe_path.parent.parent
        
        if self._is_java_bin(path) and path.exists():
            if path.parent.name == "bin":
                return path.parent.parent
            return path.parent
        
        return None
    
    def find(self):
        if len(sys.argv) > 1:
            user_path = Path(sys.argv[1]).resolve()
            if (home := self.validate_java_path(user_path)) is not None:
                self.java_home = home

        if not self.java_home:
            if (home := self._find_from_path()) is not None:
                self.java_home = home

        if not self.java_home:
            print('Error: No compatible Java runtime was found. Set JAVA_HOME or add java to PATH.')
            print('Tip: You can also pass a Java path as an argument, for example: python script/buildAll.py /path/to/jdk')
            sys.exit(1)

        self.java_home = str(Path(self.java_home).resolve())
        os.environ.setdefault('JAVA_HOME', self.java_home)
        self.java_exe = Path(self.java_home) / "bin" / "java"
        if sys.platform.startswith('win'):
            self.java_exe = self.java_exe.with_suffix('.exe')
        
        if not self.java_exe.exists():
            print(f'Error: Java executable does not exist - {self.java_exe}')
            sys.exit(1)

        print(f"JAVA_HOME: {self.java_home}")

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
        print('Error: Unable to identify Java version')
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
    print(f"[Run] {' '.join(cmd)}")
    
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
        print(f"Task failed: {e.cmd}")
        if e.output:
            print("Error output:\n", e.output)
        return False

def copy_build_libs(platform: str) -> None:
    libs_dir = cur_path / platform / "build" / "libs"
    if not libs_dir.exists():
        print(f"Warning: Build directory does not exist - {libs_dir}")
        return
    
    for file in libs_dir.iterdir():
        if file.is_file() and should_copy(file.name):
            try:
                shutil.copy2(file, output_dir)
                print(f"Copied: {file.name}")
            except Exception as e:
                print(f"Failed to copy {file.name}: {e}")

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
    print(f"Current Java version: {java_version}, path: {java.java_exe}")

    version_configs = {}
    for config_file in version_configs_path.glob("*.json"):
        version_name = config_file.stem
        try:
            config = VersionParser.parse(config_file)
            version_configs[version_name] = config
            print(f"Loaded configuration: {config_file} -> {config['common']['minecraft_version']}")
        except Exception as e:
            print(f"Failed to load configuration {config_file.name}: {e}")
            sys.exit(1)
    
    for ver, config in version_configs.items():
        required_ver = int(config["common"].get('java_version', 17))
        if required_ver > java_version:
            print(f"Error: {ver} requires Java {required_ver}+ (current: {java_version})")
            sys.exit(1)

    if output_dir.exists():
        shutil.rmtree(output_dir, ignore_errors=True)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    start_time = time.time()
    print(f"\nBuilding {len(version_configs)} version(s):")
    #call_gradle_task("native:buildWin")
    for version, config in version_configs.items():
        try :
            if config["skip_build"]:
                print(f"Skipping build: {version}")
                continue
        except KeyError:
            pass
        print(f"\n=== Building {version} ===")
        print("Target loaders:", ", ".join(config["common"]["platforms"]))
        call_gradle_task("clean")
        for platform in config["common"]["platforms"] + ["common"]:
            try:
                shutil.rmtree(f"{platform}/build", ignore_errors=True)
                print(f"Cleaned build directory: {platform}/build")
            except Exception as e:
                print(f"Failed to clean build directory {platform}: {e}")
        build_args = f"-Pminecraft_version_config={version}"
        if not call_gradle_task("build", build_args):
            print(f"Build failed: {version}")
            continue
        
        print("\nCopying build artifacts:")
        for platform in config["common"]["platforms"]:
            copy_build_libs(platform.strip())
    
    total_time = time.time() - start_time
    print(f"\nBuild complete! Total time: {total_time:.2f}s")
    print(f"Output directory: {output_dir.resolve()}")
