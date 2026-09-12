# toKRW (iOS)

Swift + SwiftUI 로 만든 환율 계산 테스트 앱.

## 실행

`ios/toKRW.xcodeproj` 를 Xcode 로 열고 시뮬레이터를 골라 Run 하면 됩니다.
실기기에 올릴 때는 타겟 설정의 Signing & Capabilities 에서 본인 Team 을 고르고,
번들 ID 는 `com.dongyeon.tokrw` 로 잡혀 있습니다. 겹치면 다른 값으로 바꾸세요.

터미널에서 빌드하려면:

```bash
cd ios
xcodebuild -project toKRW.xcodeproj -scheme toKRW \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

## 아이폰에 설치하기

앱스토어를 거치지 않고 본인 아이폰에 넣는 경우입니다.
처음 한 번은 Xcode 에서 Signing & Capabilities 탭의 Team 을 고르고,
아이폰에서 개발자 모드와 개발자 신뢰를 켜야 합니다.

그다음부터는 아이폰을 USB 로 연결하고 이것만 실행하면 됩니다.

```bash
cd ios
./install-device.sh
```

무료 Apple 계정으로 서명한 앱은 **7일이면 열리지 않습니다.**
프로비저닝 프로파일 유효기간이 7일이라 그렇습니다. 인증서는 1년이지만 프로파일이 먼저 만료됩니다.
아이폰에서 신뢰를 다시 눌러도 풀리지 않고, 위 스크립트로 다시 설치해야 7일이 새로 시작됩니다.
앱을 지울 필요는 없고 덮어쓰기로 설치됩니다.

기한을 늘리려면 연 99달러짜리 Apple Developer Program 에 가입하는 방법뿐입니다. 프로파일이 1년으로 늘어납니다.

## 구성

| 파일 | 역할 |
| --- | --- |
| `ContentView.swift` | 화면 전체. 키패드, 통화 선택 시트, 결과 표시 |
| `ConverterViewModel.swift` | 입력 상태와 계산, 숫자 표시 형식 |
| `RateRepository.swift` | 환율 조회, 공급처 이중화, UserDefaults 캐시 |
| `Currency.swift` | 통화 목록과 표시 단위 |
| `toKRWApp.swift` | 진입점 |

처음 한 번은 iOS 시뮬레이터 런타임이 필요합니다. 없으면 `xcodebuild -downloadPlatform iOS` 로 받으세요.

## 환경

- 최소 지원: iOS 16.0
- Swift 5, 세로 방향 고정
- 외부 라이브러리 없음
