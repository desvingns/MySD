#!/usr/bin/env bash
# Project-local MP runner for MySD.
# Emits exactly one JSON object on stdout; command output is kept in a temp log.

set -uo pipefail

SCREENSHOT_RECORD_NEEDED="${1:-false}"

json_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "$value"
}

ERRORS=()
add_error() {
  ERRORS+=("$1")
}

errors_json() {
  local index=0 output='[' error
  for error in "${ERRORS[@]}"; do
    [ "$index" -gt 0 ] && output+=','
    output+="\"$(json_escape "$error")\""
    index=$((index + 1))
  done
  output+=']'
  printf '%s' "$output"
}

path_for_bash() {
  local candidate="$1"
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -u "$candidate" 2>/dev/null || printf '%s' "$candidate"
  else
    printf '%s' "$candidate"
  fi
}

valid_java_home() {
  local candidate="$1"
  [ -n "$candidate" ] && {
    [ -x "$candidate/bin/java" ] || [ -x "$candidate/bin/java.exe" ]
  }
}

valid_android_home() {
  local candidate="$1"
  [ -n "$candidate" ] && {
    [ -d "$candidate/platforms" ] ||
    [ -d "$candidate/platform-tools" ] ||
    [ -d "$candidate/build-tools" ]
  }
}

emit_result() {
  local pass="$1" tests="$2" lint="$3" coverage="$4" screenshots="$5"
  printf '{"pass":%s,"tests":"%s","lint":"%s","detekt":"n/a (not configured)","coverage":"%s","screenshots":"%s","errors":%s}\n' \
    "$pass" \
    "$(json_escape "$tests")" \
    "$(json_escape "$lint")" \
    "$(json_escape "$coverage")" \
    "$(json_escape "$screenshots")" \
    "$(errors_json)"
}

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || {
  add_error "not a git repo"
  emit_result false "unknown" "unknown" "n/a (not configured)" "skipped"
  exit 0
}
cd "$REPO_ROOT" || {
  add_error "cannot cd to repository root"
  emit_result false "unknown" "unknown" "n/a (not configured)" "skipped"
  exit 0
}

# Keep a valid caller-provided Java runtime. Otherwise prefer Android Studio JBR
# candidates, including the standard Windows Git Bash locations.
JAVA_HOME_BASH="$(path_for_bash "${JAVA_HOME:-}")"
if valid_java_home "$JAVA_HOME_BASH"; then
  export JAVA_HOME="$JAVA_HOME_BASH"
else
  for candidate in \
      "${LOCALAPPDATA:-}/Programs/Android Studio/jbr" \
      "/c/Program Files/Android/Android Studio/jbr" \
      "/c/Program Files/Android/Android Studio/jbr/Contents/Home" \
      "$HOME/.jbr/jbr_jcef-17"* \
      "/snap/android-studio/current/jbr" \
      "/opt/android-studio/jbr" \
      "/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
    candidate="$(path_for_bash "$candidate")"
    if valid_java_home "$candidate"; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if valid_java_home "${JAVA_HOME:-}"; then
  export PATH="$JAVA_HOME/bin:$PATH"
else
  add_error "JAVA_HOME is not set to a usable JDK/JBR"
fi

# Respect ANDROID_HOME/ANDROID_SDK_ROOT when usable; otherwise discover the
# standard per-user Windows SDK location without changing or installing files.
ANDROID_HOME_BASH="$(path_for_bash "${ANDROID_HOME:-}")"
ANDROID_SDK_ROOT_BASH="$(path_for_bash "${ANDROID_SDK_ROOT:-}")"
if valid_android_home "$ANDROID_HOME_BASH"; then
  export ANDROID_HOME="$ANDROID_HOME_BASH"
elif valid_android_home "$ANDROID_SDK_ROOT_BASH"; then
  export ANDROID_HOME="$ANDROID_SDK_ROOT_BASH"
else
  for candidate in \
      "${LOCALAPPDATA:-}/Android/Sdk" \
      "$HOME/AppData/Local/Android/Sdk" \
      "/c/Users/${USERNAME:-${USER:-unknown}}/AppData/Local/Android/Sdk" \
      "/c/Android/Sdk" \
      "/opt/android-sdk"; do
    candidate="$(path_for_bash "$candidate")"
    if valid_android_home "$candidate"; then
      export ANDROID_HOME="$candidate"
      break
    fi
  done
fi
if [ -n "${ANDROID_HOME:-}" ]; then
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
else
  add_error "ANDROID_HOME is not set to a usable Android SDK"
fi

LOG_FILE="${TMPDIR:-/tmp}/mysd-mp-runner-android-$$.log"
: >"$LOG_FILE" 2>/dev/null || LOG_FILE="/dev/null"

./gradlew :game:test :app:assembleDebug --no-daemon >"$LOG_FILE" 2>&1
BUILD_EXIT=$?

TOTAL=0
FAILED=0
SKIPPED=0
TEST_XML_ROOT="$REPO_ROOT/game/build/test-results/test"
if [ -d "$TEST_XML_ROOT" ]; then
  while IFS= read -r xml; do
    [ -n "$xml" ] || continue
    suite=$(tr '\n' ' ' <"$xml" | grep -o '<testsuite [^>]*>' | head -n 1 || true)
    [ -n "$suite" ] || continue
    tests=$(printf '%s' "$suite" | grep -o 'tests="[0-9]*"' | grep -o '[0-9]*' | head -n 1 || true)
    failures=$(printf '%s' "$suite" | grep -o 'failures="[0-9]*"' | grep -o '[0-9]*' | head -n 1 || true)
    errors=$(printf '%s' "$suite" | grep -o 'errors="[0-9]*"' | grep -o '[0-9]*' | head -n 1 || true)
    skipped=$(printf '%s' "$suite" | grep -o 'skipped="[0-9]*"' | grep -o '[0-9]*' | head -n 1 || true)
    TOTAL=$((TOTAL + ${tests:-0}))
    FAILED=$((FAILED + ${failures:-0} + ${errors:-0}))
    SKIPPED=$((SKIPPED + ${skipped:-0}))
  done < <(find "$TEST_XML_ROOT" -type f -name 'TEST-*.xml' -print 2>/dev/null)
fi

if [ "$TOTAL" -gt 0 ]; then
  TESTS_RESULT="$((TOTAL - FAILED - SKIPPED)) passed / $FAILED failed / $SKIPPED skipped"
elif [ "$BUILD_EXIT" -eq 0 ]; then
  TESTS_RESULT="ok"
else
  TESTS_RESULT="failed (gradle exit=$BUILD_EXIT)"
fi

if [ "$BUILD_EXIT" -ne 0 ]; then
  add_error "gradle :game:test :app:assembleDebug exit=$BUILD_EXIT"
  while IFS= read -r line; do
    [ -n "$line" ] && add_error "$line"
  done < <(grep -E 'FAILURE:|BUILD FAILED|^e: |error:' "$LOG_FILE" | head -n 5 || true)
fi

LINT_LOG="${TMPDIR:-/tmp}/mysd-mp-lint-android-$$.log"
: >"$LINT_LOG" 2>/dev/null || LINT_LOG="/dev/null"
./gradlew :app:lintDebug --no-daemon >"$LINT_LOG" 2>&1
LINT_EXIT=$?
if [ "$LINT_EXIT" -eq 0 ]; then
  LINT_RESULT="ok"
else
  LINT_RESULT="failed (gradle exit=$LINT_EXIT)"
  add_error "gradle :app:lintDebug exit=$LINT_EXIT"
  while IFS= read -r line; do
    [ -n "$line" ] && add_error "$line"
  done < <(grep -E 'FAILURE:|BUILD FAILED|error:' "$LINT_LOG" | head -n 5 || true)
fi

if [ "$SCREENSHOT_RECORD_NEEDED" = "true" ]; then
  SCREENSHOTS_RESULT="not run (not configured)"
  add_error "screenshots requested but no screenshot runner is configured"
else
  SCREENSHOTS_RESULT="skipped"
fi

PASS=true
[ "$BUILD_EXIT" -eq 0 ] || PASS=false
[ "$LINT_EXIT" -eq 0 ] || PASS=false
[ "$SCREENSHOT_RECORD_NEEDED" = "true" ] && PASS=false

emit_result "$PASS" "$TESTS_RESULT" "$LINT_RESULT" "n/a (not configured)" "$SCREENSHOTS_RESULT"
