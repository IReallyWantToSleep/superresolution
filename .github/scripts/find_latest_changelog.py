import os, re, sys

changelogs_dir = sys.argv[1] if len(sys.argv) > 1 else 'changelogs'
semver_pattern = re.compile(r'^(\d+\.\d+\.\d+(-[a-zA-Z]+(\.[\d]+)?)*)\.md$')

PRE_RELEASE_PRIORITY = ['alpha', 'beta', 'pre']


def parse_semver(v):
    core, _, pre = v.partition('-')
    major, minor, patch = map(int, core.split('.'))
    if pre:
        pre_parts = []
        for p in pre.split('.'):
            try:
                pre_parts.append((0, int(p)))
            except ValueError:
                idx = PRE_RELEASE_PRIORITY.index(p) if p in PRE_RELEASE_PRIORITY else len(PRE_RELEASE_PRIORITY)
                pre_parts.append((1, idx, p))
        return (major, minor, patch, 0, pre_parts)
    return (major, minor, patch, 1, [])


versions = []
for f in os.listdir(changelogs_dir):
    m = semver_pattern.match(f)
    if m:
        versions.append((parse_semver(m.group(1)), f))

if not versions:
    raise SystemExit('No valid changelog files found')

versions.sort(key=lambda x: x[0], reverse=True)
print(versions[0][1])
