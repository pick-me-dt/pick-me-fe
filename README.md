# 픽미 (Pickme) 차량 앱

가는 길에, 평소 그 한 잔. 차량의 도착 예정 시간(ETA)에 맞춰 매장이 준비하는 드라이브스루 예약 앱입니다. Pleos Connect(Android Automotive OS)에서 동작합니다.

- 서버: [pick-me-be](https://github.com/pick-me-dt/pick-me-be)
- 구조: [ARCHITECTURE.md](ARCHITECTURE.md)

## 현재 상태

Figma 프로토타입의 차량 화면을 모두 구현했습니다.

- 시작·첫 이용(V00): 스플래시, 초기 상태 확인, 앱 실행 실패, 첫 이용 안내, 정보 이용 동의, 정보 접근 불가, 차량번호 등록·입력 오류, 진행 주문 복귀
- 홈(V01): 시작, 등록 내역 없음, 매장 선택, 메뉴 선택, 경로 없음, 이용 매장 없음
- 예약(V02): 예약 확인, 메뉴 품절, 가격 변경 재확인, 매장 접수 종료
- 진행(V03~V07): 매장 확인 중·예약 거절·응답 지연, 제조 대기·제조 중·준비 완료, 연결·ETA 오류, 제조 전·후 지연, 주문 상세·취소 요청·취소 확인 중·완료·거절·주행 중 제한, 수령 완료
- 설정(V08): 설정·등록 정보 관리, 이용 안내·정보 처리

서버·매장 목록·내비게이션 경로·차량 신호는 가짜 구현으로 동작합니다. 동의 여부와 차량번호만 기기에 저장합니다.

### 시연 컨트롤

앱 아래에 붙어 있고, 실제 주문·차량과 연동되지 않습니다. 세 묶음으로 나눠 보여 줍니다.

| 묶음 | 버튼 |
| --- | --- |
| 시작·홈·예약 | 첫 실행, 앱 다시 시작, 조회 실패, 설정 열기, 등록 내역 없음, 경로 없음, 매장 없음, 조건 해제, 메뉴 품절, 가격 변경, 접수 종료 |
| 매장 | 매장 수락·거절, 응답 지연, 제조 시작, 준비 완료, 수령 완료, 취소 승인·거절 |
| 이동 | ETA 6분, 지연 +4분, 연결 끊김·복구, P단, D단, 내비 경로(양재), 내비 조회 |

에뮬레이터(차량)에서는 기어를 Car API로 읽으므로 P단·D단 버튼 대신 Virtual Console에서 바꿉니다. 「내비 경로(양재)」는 실제 내비게이션에 양재점 경로를 요청하고, 「내비 조회」는 남은 시간을 logcat(`PickmeNavi`)에 남깁니다. 차량에서는 경로 유무를 실제 내비로 판단하므로 「경로 없음」 대신 내비에서 안내를 종료합니다. 주문의 ETA는 실제 내비의 첫 남은 시간에서 시작해 모의로 줄어듭니다(에뮬레이터는 위치가 움직이지 않음). 「접기」로 컨트롤을 한 줄로 줄일 수 있습니다.

**영상 촬영: 터미널 명령**

영상에 시연 버튼이 보이지 않게 컨트롤을 숨기고, Mac 터미널에서 같은 조작을 보냅니다. debug 빌드에만 수신기가 있어 release APK로는 조작할 수 없습니다.

```bash
./scripts/demo.sh panel off   # 시연 컨트롤 숨기기 (앱을 다시 켜도 유지, 다시 보이려면 panel on)
./scripts/demo.sh accept      # 매장 수락
./scripts/demo.sh eta 6       # 남은 시간 6분
./scripts/demo.sh delay 4     # 정체로 4분 늘리기
./scripts/demo.sh help        # 전체 명령
```

에뮬레이터에서는 기어를 Virtual Console로 바꿉니다(`park`·`drive` 명령은 모의 기어일 때만).

「초기화」는 항상 보이며 주문·등록 메뉴·확인 조건·ETA·기어를 처음으로 되돌립니다(동의·차량번호는 유지). 「조회 실패」는 「첫 실행」이나 「앱 다시 시작」 전에 눌러야 V00-E가 나옵니다.

## 실행 방법

1. Android Studio에 Pleos Connect Emulator v2.1.0(API 34)을 설치하고 AVD를 콜드 부팅으로 만듭니다. ([설치 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator), 명령줄로 만든 방법과 확인 결과는 [에뮬레이터 게이트 결과](docs/plans/2026-11-10-emulator-gates.md))
2. 이 저장소를 Android Studio로 열거나 터미널에서 빌드합니다.

```bash
./gradlew :app:installDebug
```

실서버(로컬 백엔드, 2차 기획 반영 `pick-me-be` `1ef2b1c`)에 붙여 실행하려면 서버 주소를 넘깁니다. 백엔드 띄우는 법과 테스트 결과는 [실서버 전체 흐름 테스트 (2차 서버)](docs/plans/2026-11-11-server-e2e-v2.md)에 있습니다. 실서버 모드에서도 `scripts/demo.sh`의 매장 명령(`accept`·`prepare` 등)을 같은 이름으로 씁니다.

```bash
./gradlew :app:installDebug -Ppickme.serverUrl=http://10.0.2.2:3000
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
