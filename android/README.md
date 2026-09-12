# toKRW (Android)

Kotlin + Jetpack Compose 로 만든 환율 계산 테스트 앱.

## 실행

Android Studio 에서 `android` 폴더를 열고 Run 을 누르면 됩니다.

터미널에서 바로 돌리려면:

```bash
cd android
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # 처음 한 번만
./gradlew installDebug
adb shell am start -n com.example.tokrw.debug/com.example.tokrw.MainActivity
```

APK 만 필요하면 `./gradlew assembleDebug` 로 만들고
`app/build/outputs/apk/debug/app-debug.apk` 를 가져다 쓰면 됩니다.

## 구성

| 파일 | 역할 |
| --- | --- |
| `MainActivity.kt` | Compose 화면 전체. 키패드, 통화 선택 시트, 결과 표시 |
| `ConverterViewModel.kt` | 입력 상태와 계산, 숫자 표시 형식 |
| `RateRepository.kt` | 환율 조회, 공급처 이중화, SharedPreferences 캐시 |
| `Currency.kt` | 통화 목록과 표시 단위 |

## 환경

- 최소 지원: Android 8.0 (API 26)
- 빌드 대상: API 34
- Gradle 8.13 / AGP 8.9.1 / Kotlin 2.1.0
- JDK 17 이상
