import os, sys
from pathlib import Path

python_executable = Path(sys.executable)
path = Path(__file__).resolve().parent
#############glslang#############
print("Initializing glslang...")
os.chdir(path / "SRNativeMain/third_party/glslang")
os.system(f"{python_executable} update_glslang_sources.py")