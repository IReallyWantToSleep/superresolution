def parse_version_string(version_string: str):
    """
    解析类似superresolution-fabric-1.20.1-0.6.2-alpha.1的文件名称转换成版本信息
    {
        "loader":"fabric",
        "mc_version":"1.20.1",
        "mod_version":"0.6.2-alpha.1",
    }
    """
    version_string = version_string.replace(".jar", "")
    parts = version_string.split("-")
    if len(parts) < 4:
        raise ValueError("Invalid version string format")

    loader = parts[1]
    mc_version = parts[2]
    mod_version = "-".join(parts[3:])

    return {
        "loader": loader,
        "mc_version": mc_version,
        "mod_version": mod_version,
    }


def to_mcmod_api_string(loader: str):
    loader_mapping = {
        "forge": 1,
        "fabric": 2,
        "neoforge": 13,
    }
    return loader_mapping.get(loader, 0)
