import os 
import json
from pathlib import Path
from typing import Dict, List, Optional

script_path = Path(__file__).resolve()
cur_path = script_path.parent.parent

i18n_dir = cur_path / "common" / "src" / "main" / "resources" / "assets" / "super_resolution" / "lang"
def load_i18n_files() -> Dict[str, Dict[str, str]]:
    i18n_data = {}
    for file in i18n_dir.glob("*.json"):
        with file.open("r", encoding="utf-8") as f:
            try:
                data = json.load(f)
                i18n_data[file.stem] = data
            except json.JSONDecodeError as e:
                print(f"Error parsing {file.name}: {e}")
    return i18n_data
i18n_data = load_i18n_files()
if __name__ == "__main__":
    zh_cn_keys = set(i18n_data.get("zh_cn", {}).keys())
    for lang, translations in i18n_data.items():
        if lang == "zh_cn":
            continue
        missing_keys = zh_cn_keys - set(translations.keys())
        if missing_keys:
            print(f"Language '{lang}' is missing {len(missing_keys)} keys:")
            for key in missing_keys:
                print(f"  - {key}")
        else:        
            print(f"Language '{lang}' has all keys.")

        extra_keys = set(translations.keys()) - zh_cn_keys
        if extra_keys:
            print(f"Language '{lang}' has {len(extra_keys)} extra keys not in zh_cn:")
            for key in extra_keys:
                print(f"  - {key}")
