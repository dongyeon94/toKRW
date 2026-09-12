#!/bin/bash
# 아이폰에 toKRW 를 다시 설치한다.
#
# 무료 Apple 계정으로 서명한 앱은 프로비저닝 프로파일이 7일이면 만료된다.
# 만료되면 앱이 열리지 않는데, 폰을 USB 로 연결하고 이 스크립트를 돌리면 7일이 다시 시작된다.
# 앱을 지울 필요는 없다. 덮어쓰기로 설치된다.

set -euo pipefail
cd "$(dirname "$0")"

SCHEME=toKRW
BUNDLE_ID=com.dongyeon.tokrw
DERIVED=build/device

echo "▸ 연결된 아이폰을 찾는 중"
TMP=$(mktemp -t tokrw-devices)
trap 'rm -f "$TMP"' EXIT
xcrun devicectl list devices --json-output "$TMP" >/dev/null

DEVICE=$(python3 - "$TMP" <<'PY'
import json, sys
devices = json.load(open(sys.argv[1]))["result"]["devices"]
for d in devices:
    if (d["hardwareProperties"].get("platform") == "iOS"
            and d["connectionProperties"].get("tunnelState") == "connected"):
        print(d["identifier"], d["deviceProperties"].get("name", ""), sep="\t")
        break
PY
)

if [ -z "$DEVICE" ]; then
    echo "✗ 연결된 아이폰이 없습니다. USB 로 연결하고 잠금을 푼 뒤 다시 실행하세요." >&2
    exit 1
fi

DEVICE_ID=${DEVICE%%$'\t'*}
DEVICE_NAME=${DEVICE#*$'\t'}
echo "  $DEVICE_NAME"

echo "▸ 빌드하고 서명하는 중"
mkdir -p "$DERIVED"
xcodebuild -project "$SCHEME.xcodeproj" -scheme "$SCHEME" -configuration Debug \
    -destination "id=$DEVICE_ID" -derivedDataPath "$DERIVED" \
    -allowProvisioningUpdates -allowProvisioningDeviceRegistration \
    build > "$DERIVED.log" 2>&1 || {
        echo "✗ 빌드 실패. 자세한 내용은 $DERIVED.log 를 보세요." >&2
        grep -E "error:" "$DERIVED.log" | head -5 >&2 || true
        exit 1
    }

echo "▸ 설치하는 중"
xcrun devicectl device install app --device "$DEVICE_ID" \
    "$DERIVED/Build/Products/Debug-iphoneos/$SCHEME.app" > /dev/null

echo "▸ 실행하는 중"
xcrun devicectl device process launch --device "$DEVICE_ID" "$BUNDLE_ID" > /dev/null

echo "✓ 완료. 앞으로 7일 동안 쓸 수 있습니다."
