#!/usr/bin/env bash
# Wrapper for the JVM that NeoFormRuntime uses to run external tools (via the
# createMinecraftArtifacts `--java-executable`). NeoForm's decompile step launches
# Vineflower with a fixed, small -Xmx that OOMs on modern Minecraft, so when we
# detect a Vineflower invocation we raise the heap.
set -euo pipefail

if [[ -n "${SR_JAVA_EXECUTABLE:-}" && -x "${SR_JAVA_EXECUTABLE}" ]]; then
  real_java="${SR_JAVA_EXECUTABLE}"
elif [[ -x "/usr/lib/jvm/zing-21/bin/java" ]]; then
  real_java="/usr/lib/jvm/zing-21/bin/java"
elif [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  real_java="${JAVA_HOME}/bin/java"
else
  real_java="$(command -v java)"
fi

# Heap to give Vineflower; overridable via SR_DECOMPILE_XMX (e.g. "16G").
decompile_xmx="${SR_DECOMPILE_XMX:-12G}"

is_vineflower=false
for arg in "$@"; do
  if [[ "${arg}" == *vineflower* ]]; then
    is_vineflower=true
    break
  fi
done

args=()
for arg in "$@"; do
  if [[ "${is_vineflower}" == true && "${arg}" == -Xmx* ]]; then
    args+=("-Xmx${decompile_xmx}")
  else
    args+=("${arg}")
  fi
done

exec "${real_java}" "${args[@]}"
