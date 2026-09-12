#!/usr/bin/env python3
"""USD 기준 환율 표를 받아 web/rates.json 으로 굽는다.

깃허브 액션이 하루 한 번 돌리는 스크립트다. 이렇게 구워 둔 파일이 있으면
공급처가 막히거나 폰이 오프라인일 때도 하루 이내의 값으로 계산할 수 있다.
"""

import json
import pathlib
import ssl
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

# 웹 앱이 쓰는 것과 같은 공급처, 같은 순서다.
PROVIDERS = [
    ("open.er-api.com", "https://open.er-api.com/v6/latest/USD"),
    ("frankfurter.dev", "https://api.frankfurter.dev/v1/latest?base=USD"),
]

OUTPUT = pathlib.Path(__file__).resolve().parent.parent / "web" / "rates.json"


def ssl_context() -> ssl.SSLContext:
    """파이썬 설치본에 따라 CA 목록이 비어 있는 경우가 있어 certifi 를 먼저 본다."""
    try:
        import certifi
        return ssl.create_default_context(cafile=certifi.where())
    except ImportError:
        return ssl.create_default_context()


def fetch(url: str) -> dict:
    request = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=20, context=ssl_context()) as response:
        if response.status != 200:
            raise RuntimeError(f"HTTP {response.status}")
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    reasons = []

    for label, url in PROVIDERS:
        try:
            body = fetch(url)
            rates = {
                code: float(value)
                for code, value in (body.get("rates") or {}).items()
                if isinstance(value, (int, float)) and value > 0
            }
            rates["USD"] = 1.0
            if "KRW" not in rates:
                raise RuntimeError("응답에 KRW 가 없습니다")

            payload = {
                "base": "USD",
                "source": label,
                "fetchedAt": int(datetime.now(timezone.utc).timestamp() * 1000),
                "fetchedAtText": datetime.now(timezone.utc).isoformat(timespec="seconds"),
                "rates": dict(sorted(rates.items())),
            }

            OUTPUT.parent.mkdir(parents=True, exist_ok=True)
            OUTPUT.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n",
                              encoding="utf-8")
            print(f"{label} 에서 {len(rates)}개 통화를 받았습니다. 1 USD = {rates['KRW']:.4f} KRW")
            return 0

        except (urllib.error.URLError, OSError, ValueError, RuntimeError) as error:
            reasons.append(f"{label}: {error}")

    print("환율을 가져오지 못했습니다: " + " / ".join(reasons), file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
