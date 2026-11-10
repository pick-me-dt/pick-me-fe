# 픽미 (Pickme) 차량 앱

가는 길에, 평소 그 한 잔. 차량의 도착 예정 시간(ETA)에 맞춰 매장이 준비하는 드라이브스루 예약 앱입니다. Pleos Connect(Android Automotive OS)에서 동작합니다.

- 서버: [pick-me-be](https://github.com/pick-me-dt/pick-me-be)
- 구조: [ARCHITECTURE.md](ARCHITECTURE.md)

## 현재 상태

시연 흐름 화면(V01 시작 → V02 예약 확인 → V03 매장 확인 중 → V04 제조 대기·제조 중·준비 완료 → V05 제조 전·후 지연 → V07 수령 완료)과 예외·취소 화면(V03-R 예약 거절, V03-T 응답 지연, V04-E 연결·ETA 오류, V06 주문 상세·취소 요청·취소 확인 중·완료·거절·주행 중 제한)을 구현했습니다. 서버와 차량 신호는 가짜 구현으로 동작하고, 앱 아래 시연 컨트롤로 매장 수락·제조·ETA 변경·기어 변경을 재현합니다.

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

화면 스크린샷(840×1184, `app/build/outputs/roborazzi/`):

```bash
./gradlew :app:recordRoborazziDebug
```

## 사용한 글꼴

[Asta Sans](https://github.com/42dot/Asta-Sans) (42dot, SIL Open Font License 1.1). 라이선스 전문은 `design/licenses/AstaSans-OFL.txt`에 있습니다.

## 테스트 환경

| 항목 | 값 |
| --- | --- |
| 실행 대상 | Pleos Connect Emulator v2.1.0 (Android API 34) |
| minSdk / compileSdk / targetSdk | 28 / 36 / 35 |
| 빌드 | Gradle 8.14.3, Android Gradle Plugin 8.13.1, Kotlin 2.2.20, JDK 17 이상 |
| Pleos SDK | NaviHelper 3.0.5, Vehicle 2.3.2 |

## APK

제출용 release APK는 `apk/` 폴더에 둡니다.
