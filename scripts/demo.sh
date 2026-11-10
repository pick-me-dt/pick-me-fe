#!/usr/bin/env bash
# 시연 컨트롤을 터미널에서 보낸다. 영상 촬영 때 앱 화면에 시연 버튼이 보이지 않게 하려고 쓴다.
# debug 빌드에만 수신기가 있다. 명령 목록: ./scripts/demo.sh help
#
#   ./scripts/demo.sh panel off   # 시연 컨트롤 숨기기 (앱을 다시 켜도 유지)
#   ./scripts/demo.sh accept      # 매장 수락
#   ./scripts/demo.sh eta 6       # 남은 시간 6분
#   ./scripts/demo.sh delay 4     # 정체로 4분 늘리기
set -euo pipefail

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
cmd="${1:-help}"
arg="${2:-}"

# adb shell은 빈 문자열 인자를 버리므로 arg는 값이 있을 때만 붙인다
extras=(--es cmd "$cmd")
if [ -n "$arg" ]; then extras+=(--es arg "$arg"); fi

out=$("$ADB" shell am broadcast --user current \
  -n com.pickmedt.car/.debug.DemoCommandReceiver \
  -a com.pickmedt.car.DEMO \
  "${extras[@]}")

# 결과 문구만 보여 준다. 수신기가 없으면(release 빌드·앱 미설치) adb 출력을 그대로 보여 준다
result=$(printf '%s\n' "$out" | sed -n 's/^Broadcast completed: result=[0-9-]*, data="\(.*\)"$/\1/p')
if [ -z "$result" ] && ! printf '%s' "$out" | grep -q 'data="'; then
  printf '%s\n' "$out"
  exit 1
fi
printf '%b\n' "$result"
