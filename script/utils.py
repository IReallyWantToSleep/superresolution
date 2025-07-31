ALL_VERSIONS = [
    '1.0', '1.1', '1.2.1', '1.2.2', '1.2.3', '1.2.4', '1.2.5',
    '1.3.1', '1.3.2', '1.4.2', '1.4.4', '1.4.5', '1.4.6', '1.4.7',
    '1.5.1', '1.5.2', '1.6.1', '1.6.2', '1.6.4', '1.7.2', '1.7.3',
    '1.7.4', '1.7.5', '1.7.6', '1.7.7', '1.7.8', '1.7.9', '1.7.10',
    '1.8', '1.8.1', '1.8.2', '1.8.3', '1.8.4', '1.8.5', '1.8.6', '1.8.7', '1.8.8', '1.8.9',
    '1.9', '1.9.1', '1.9.2', '1.9.3', '1.9.4', '1.10', '1.10.1', '1.10.2',
    '1.11', '1.11.1', '1.11.2', '1.12', '1.12.1', '1.12.2', '1.13', '1.13.1', '1.13.2',
    '1.14', '1.14.1', '1.14.2', '1.14.3', '1.14.4', '1.15', '1.15.1', '1.15.2',
    '1.16', '1.16.1', '1.16.2', '1.16.3', '1.16.4', '1.16.5', '1.17', '1.17.1',
    '1.18', '1.18.1', '1.18.2', '1.19', '1.19.1', '1.19.2', '1.19.3', '1.19.4',
    '1.20', '1.20.1', '1.20.2', '1.20.3', '1.20.4', '1.20.5', '1.20.6',
    '1.21', '1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8'
]

def parse_mc_version_range(mc_version_str):
    if "~" in mc_version_str:
        start, end = mc_version_str.split("~")
        try:
            idx1 = ALL_VERSIONS.index(start)
            idx2 = ALL_VERSIONS.index(end)
        except ValueError:
            raise ValueError(f"未知版本号: {mc_version_str}")
        if idx1 > idx2:
            raise ValueError(f"版本范围顺序错误: {mc_version_str}")
        return ALL_VERSIONS[idx1:idx2+1]
    else:
        if mc_version_str not in ALL_VERSIONS:
            raise ValueError(f"未知版本号: {mc_version_str}")
        return [mc_version_str]

def parse_version_string(version_string: str):
    version_string = version_string.replace(".jar", "")
    parts = version_string.split("-")
    if len(parts) < 4:
        raise ValueError("Invalid version string format")

    loader = parts[1]
    mc_version_part = parts[2]
    mod_version = "-".join(parts[3:])
    mc_versions = parse_mc_version_range(mc_version_part)

    return {
        "loader": loader,
        "mc_versions": mc_versions,
        "mod_version": mod_version,
    }

def to_mcmod_api_string(loader: str):
    loader_mapping = {
        "forge": 1,
        "fabric": 2,
        "neoforge": 13,
    }
    return loader_mapping.get(loader, 0)

def mc_version_to_id(version: str):
    """把版本号转成唯一id（1.0-1, 1.1-2, ...）"""
    if version not in ALL_VERSIONS:
        raise ValueError(f"未知版本号: {version}")
    return ALL_VERSIONS.index(version) + 1

if __name__ == "__main__":
    s = "super_resolution-fabric-1.21.6~1.21.8-0.8.0-alpha.1.jar"
    info = parse_version_string(s)
    print(info)
    print([mc_version_to_id(v) for v in info['mc_versions']]) 