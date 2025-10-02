libraries = [
    "libraries/fsr/linux64/libSuperResolutionFSR+linux64+debug.so",
    "libraries/fsr/linux64/libSuperResolutionFSR+linux64+release.so",
    "libraries/fsr/win64/libSuperResolutionFSR+win64+release.dll",
    "libraries/fsr/win64/libSuperResolutionFSR+win64+debug.dll",
    "libraries/main/linux64/libSuperResolution+linux64+debug.so",
    "libraries/main/linux64/libSuperResolution+linux64+release.so",
    "libraries/main/win64/libSuperResolution+win64+release.dll",
    "libraries/main/win64/libSuperResolution+win64+debug.dll",
    "libraries/xess/win64/libSuperResolutionXeSS+win64+release.dll",
    "libraries/xess/win64/libSuperResolutionXeSS+win64+debug.dll",
]

base_url = "https://github.com/187J3X1-114514/superresolution/raw/refs/heads/assets"
save_dir = "common/src/main/resources/lib/"
import requests
import os
from pathlib import Path
import time

script_dir = Path(__file__).resolve().parent
parent_dir = script_dir.parent

save_dir = parent_dir / save_dir

save_dir.mkdir(parents=True, exist_ok=True)

for lib_path in libraries:
    download_url = f"{base_url}/{lib_path}"

    local_path = save_dir / Path(lib_path).name

    print(f"正在下载: {lib_path}")
    print(f"URL: {download_url}")
    print(f"保存到: {local_path}")

    try:
        response = requests.get(download_url, stream=True)
        response.raise_for_status()
        data_size = int(response.headers.get('Content-Length', 0))
        downloaded_size = 0
        downloaded_size_pre_250ms = 0
        start_time = time.time()
        last_time = start_time
        with open(local_path, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)
                downloaded_size += len(chunk)
                now = time.time()
                if now - last_time >= 0.25 or downloaded_size == data_size:
                    elapsed = now - start_time
                    speed = downloaded_size / elapsed if elapsed > 0 else 0
                    percent = int((downloaded_size / data_size) * 100) if data_size else 0
                    print(f"\r下载进度: {percent}% {downloaded_size}bytes/{data_size}bytes 速度: {speed/1024:.2f} KB/s", end='')
                    last_time = now

        print(f"下载成功: {lib_path}\n")
    except Exception as e:
        print(f"下载失败: {lib_path}")
        print(f"  错误: {e}\n")

