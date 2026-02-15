#!/usr/bin/env bash
set -euo pipefail

REAL_JAVA=""
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  REAL_JAVA="${JAVA_HOME}/bin/java"
else
  REAL_JAVA="$(command -v java)"
fi

if [[ -z "${REAL_JAVA}" ]]; then
  echo "java executable not found" >&2
  exit 1
fi

ARGS=()
for arg in "$@"; do
  if [[ "${arg}" == -Xmx* ]]; then
    ARGS+=("-Xmx12G")
  else
    ARGS+=("${arg}")
  fi
done

exec "${REAL_JAVA}" "${ARGS[@]}"
