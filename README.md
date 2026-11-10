# 픽미 (Pickme) 차량 앱

가는 길에, 평소 그 한 잔. 차량의 도착 예정 시간(ETA)에 맞춰 매장이 준비하는 드라이브스루 예약 앱입니다. Pleos Connect(Android Automotive OS)에서 동작합니다.

- 서버: [pick-me-be](https://github.com/pick-me-dt/pick-me-be)
- 구조: [ARCHITECTURE.md](ARCHITECTURE.md)

## 현재 상태

화면 구현 전 뼈대 단계입니다. 서버와 차량 신호는 가짜 구현으로 동작하고, 앱 아래 시연 컨트롤로 매장 수락·제조·ETA 변경·기어 변경을 재현합니다.

## 실행 방법

1. Android Studio에 Pleos Connect Emulator v2.1.0(API 34)을 설치하고 AVD를 콜드 부팅으로 만듭니다. ([설치 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator))
2. 이 저장소를 Android Studio로 열거나 터미널에서 빌드합니다.

```bash
./gradlew :app:installDebug
```

단위 테스트:

```bash
./gradlew :app:testDebugUnitTest
```

## 테스트 환경

| 항목 | 값 |
| --- | --- |
| 실행 대상 | Pleos Connect Emulator v2.1.0 (Android API 34) |
| minSdk / compileSdk / targetSdk | 28 / 36 / 35 |
| 빌드 | Gradle 8.14.3, Android Gradle Plugin 8.13.1, Kotlin 2.2.20, JDK 17 이상 |
| Pleos SDK | NaviHelper 3.0.5, Vehicle 2.3.2 |

## APK

제출용 release APK는 `apk/` 폴더에 둡니다.
