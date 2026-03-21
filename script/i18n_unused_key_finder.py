import os 
import json
from pathlib import Path
from typing import Dict, List, Optional
import re
import i18n_validator
script_path = Path(__file__).resolve()
cur_path = script_path.parent.parent

forge_src_dir = cur_path / "forge" / "src" / "main" / "java"
neoforge_src_dir = cur_path / "neoforge" / "src" / "main" / "java"
fabric_src_dir = cur_path / "fabric" / "src" / "main" / "java"
common_src_dir = cur_path / "common" / "src" / "main" / "java"

def find_i18n_keys_in_java_files(file_path: Path) -> List[str]:
    i18n_keys = []

    with file_path.open("r", encoding="utf-8") as f:
        text = f.read()
        #Component.translatable("superresolution.screen.config.options.label.enable_upscale")
        #Text.translatable("superresolution.screen.config.options.label.enable_upscale")
        regexB = r'translatable\(\s*"([^"]+)"\s*\)'
        i18n_keys.extend(re.findall(regexB, text))
    return i18n_keys

def find_all_i18n_keys_in_directory(directory: Path) -> Dict[str, List[str]]:
    all_i18n_keys = {}
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                file_path = Path(root) / file
                keys = find_i18n_keys_in_java_files(file_path)
                if keys:
                    all_i18n_keys[file_path] = keys
    return all_i18n_keys

forge_keys = find_all_i18n_keys_in_directory(forge_src_dir)
neoforge_keys = find_all_i18n_keys_in_directory(neoforge_src_dir)
fabric_keys = find_all_i18n_keys_in_directory(fabric_src_dir)
common_keys = find_all_i18n_keys_in_directory(common_src_dir)

used_keys = {}
for keys in [forge_keys, neoforge_keys, fabric_keys, common_keys]:
    used_keys.update(keys)

for lang, translations in i18n_validator.i18n_data.items():
    unused_keys = set(translations.keys()) - set().union(*used_keys.values())
    if unused_keys:
        for key in unused_keys:
            print(f"Unused key in {lang}: {key}")