#!/usr/bin/env bash

set -uo pipefail

if [[ ! -x ./gradlew ]]; then
    printf 'Error: run this script from the dab2c-executor project root; ./gradlew was not found or is not executable.\n' >&2
    exit 1
fi

timestamp="$(date '+%Y-%m-%d_%H-%M')"
report="runtime-dependencies-${timestamp}.md"

{
    printf '# Executor runtime dependencies\n\n'
    printf 'Generated: `%s`\n\n' "$timestamp"
    printf 'Project: `:executor-application`  \n'
    printf 'Configuration: `runtimeClasspath`\n\n'
    printf '```text\n'

    if ./gradlew :executor-application:dependencies \
        --configuration runtimeClasspath \
        --console=plain 2>&1; then
        gradle_status=0
    else
        gradle_status=$?
    fi

    printf '```\n'
} > "$report"

printf 'Report created: %s\n' "$report"
exit "$gradle_status"
