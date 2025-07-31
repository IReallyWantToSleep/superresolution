import os
import re
import json
import sys
import shutil
import time
import utils
import uploader_mcmod

from pathlib import Path
from typing import Dict, List, Optional

if len(sys.argv) < 3:
    print("使用: python upload.py <MCMOD_UUID> <MCMOD_PHPSESSID>")
    sys.exit(1)
#################
INPUT_DIR = "build_jars"
MCMOD_UUID = sys.argv[1]
MCMOD_PHPSESSID = sys.argv[2]
#################
print("MCMOD_UUID", MCMOD_UUID)
print("MCMOD_PHPSESSID", MCMOD_PHPSESSID)
cur_path = Path.cwd()
input_dir = cur_path / INPUT_DIR

files = list(input_dir.glob("*.jar"))
uploader = uploader_mcmod.MCMODModUploader(
    "17888",
    {
        "_uuid": MCMOD_UUID,
        "PHPSESSID": MCMOD_PHPSESSID,
    },
)
for file in files:
    info = utils.parse_version_string(file.stem)
    print("info", info)
    print(
        uploader.upload_file(
            file_path=file,
            mc_version=', '.join(info["mc_versions"]),
            platform_ids=[1],
            api_ids=[utils.to_mcmod_api_string(info["loader"])],
            tag_names=["client"],
        )
    )
