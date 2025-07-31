import requests
import os

class MCMODModUploader:
    def __init__(self, class_id, cookies=None):
        self.base_url = "https://modfile-dl.mcmod.cn"
        self.class_id = class_id
        self.session = requests.Session()
        self.headers = {
            "Accept": "*/*",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
            "Cache-Control": "no-cache",
            "Origin": self.base_url,
            "Pragma": "no-cache",
            "Referer": f"{self.base_url}/admin/{self.class_id}/",
            "Sec-Fetch-Dest": "empty",
            "Sec-Fetch-Mode": "cors",
            "Sec-Fetch-Site": "same-origin",
            "X-Requested-With": "XMLHttpRequest",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
        }
        if cookies:
            self.session.cookies.update(cookies)

    def upload_file(self, file_path, mc_version, platform_ids, api_ids, tag_names):
        if not os.path.exists(file_path):
            raise ValueError(f"文件不存在: {file_path}")
        if os.path.getsize(file_path) > 78643200:
            raise ValueError(f"文件过大: {os.path.basename(file_path)}")

        ext = os.path.splitext(file_path)[1][1:].lower()
        if ext not in {"jar", "rar", "zip", "litemod", "mcpack", "mcaddon"}:
            raise ValueError(f"不支持的文件类型: {ext}")

        files = [
            (
                "0",
                (
                    str(os.path.basename(file_path)).replace("~","-"),
                    open(file_path, "rb"),
                    (
                        "application/java-archive"
                        if ext == "jar"
                        else "application/octet-stream"
                    ),
                ),
            )
        ]

        data = {
            "classID": str(self.class_id),
            "mcverList": mc_version,
            "platformList": ",".join(map(str, platform_ids)),
            "apiList": ",".join(map(str, api_ids)),
            "tagList": ",".join(tag_names),
        }
        try:
            response = self.session.post(
                f"{self.base_url}/action/upload/",
                headers=self.headers,
                data=data,
                files=files,
            )
            response.raise_for_status()
            return response.text
        finally:
            for f in files:
                f[1][1].close()

    def _parse_response(self, response):
        result = {
            "status": "success" if response.get("state") == 0 else "error",
            "code": response.get("state"),
            "uploaded": [],
            "updated": [],
            "failed": [],
        }

        if result["status"] == "success":
            result["uploaded"] = response.get("success", {}).get("upload", [])
            result["updated"] = response.get("success", {}).get("update", [])
            result["failed"] = response.get("failed", [])
        else:
            result["failed"] = [
                {
                    "filename": os.path.basename(self.last_uploaded_file),
                    "reason": result["message"],
                }
            ]

        return result
