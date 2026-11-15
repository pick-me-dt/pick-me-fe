# 픽미 (Pickme)

**가는 길에, 평소 그 한 잔.** 차량의 도착 예정 시간(ETA)에 맞춰 매장이 음료를 준비하는 Pleos Connect 드라이브스루 예약 앱입니다.

## 실행 방법

1. Pleos Connect Emulator v2.1.0을 설치하고 AVD를 만듭니다([설치 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)). 에뮬레이터는 콜드 부팅으로 켭니다.
2. APK를 설치합니다.

   ```bash
   adb install -r apk/pickme-0.1.0.apk
   ```

   같은 패키지의 debug 빌드가 이미 설치돼 있으면 서명이 달라 설치가 거부됩니다. 그럴 때는 먼저 지웁니다.

   ```bash
   adb uninstall com.pickmedt.car
   ```

3. 앱 서랍에서 「픽미」를 엽니다. 처음 실행하면 첫 이용 안내, 정보 이용 동의, 차량번호 입력을 차례로 거칩니다.
4. 화면 아래 시연 컨트롤에서 「내비 경로(양재)」를 눌러 내비 경로를 잡습니다. 경로가 없으면 매장을 고를 때 「이동 경로를 먼저 설정해 주세요」가 나옵니다.
5. 매장과 메뉴를 골라 예약합니다. 매장 직원 역할은 시연 컨트롤 「매장」 묶음의 버튼으로 합니다(매장 수락, 제조 시작, 준비 완료, 수령 완료 등).

소스에서 빌드해 설치할 때는 아래 명령을 씁니다.

```bash
./gradlew :app:installDebug
```

## 테스트 환경 및 필요한 설정

### 테스트 환경

| 항목 | 값 |
| --- | --- |
| 실행 대상 | Pleos Connect Emulator v2.1.0 (Android 14, API 34) |
| 개발 PC | macOS 27.2 (Apple Silicon), 에뮬레이터 GPU `host` |
| minSdk / compileSdk / targetSdk | 28 / 36 / 35 |
| 빌드 | Gradle 8.14.3, Android Gradle Plugin 8.13.1, Kotlin 2.2.20, JDK 17 이상 |
| Pleos SDK | NaviHelper 3.0.5, Vehicle 2.3.2 |

### 필요한 설정

- **서버**: 필요 없습니다. APK는 앱 안의 가짜 서버로 동작합니다(매장 목록, 주문, 매장 응답).
- **권한**: 따로 허용할 권한이 없습니다.
  - 기어(`CAR_POWERTRAIN`)와 내비(`NAVI_ROUTE`·`NAVI_ROUTE_SEARCH`·`NAVI_CUSTOM_MAP`) 권한은 normal이라 설치하면 바로 허용됩니다.
  - 속도(`CAR_SPEED`)는 요청하지 않습니다. 주행 여부는 기어와 주행 규제로 판단합니다.
- **AVD**: 콜드 부팅으로 켜고, RAM은 4GB로 썼습니다. 명령줄로 켤 때는 아래처럼 합니다.

  ```bash
  ~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2_1 -no-snapshot-load -no-snapshot-save -gpu host
  ```

- **기어 P/D**: 에뮬레이터 Virtual Console에서 바꿉니다. 포트를 연결한 뒤 브라우저로 `http://127.0.0.1:8088/`을 열고 「Emulation Controls → Gear Selection」을 씁니다.

  ```bash
  adb forward tcp:8088 tcp:8088
  ```

- **내비**: 에뮬레이터에 들어 있는 Pleos 내비를 씁니다. 에뮬레이터에서는 차량 위치가 움직이지 않습니다. 그래서 남은 시간은 내비의 첫 값에서 시작해 모의로 줄어듭니다.

## Pleos Connect 에뮬레이터 버전과 화면 규격

| 항목 | 값 |
| --- | --- |
| 에뮬레이터 | Pleos Connect Emulator v2.1.0 (시스템 이미지 `pleos_car_emulator`, Android 14 · API 34, arm64-v8a) |
| 화면 | 2560×1440, 160dpi(mdpi). 이미지에 든 기기 정의 `Pleos_Connect_v2_0`으로 만든 AVD `Pleos_Connect_v2_1` |
| 앱 창: 작업 영역 전체 | 1682×1210 (가로). 세로 화면 두 칸을 나란히 놓고, 누르는 요소는 왼쪽(운전석 쪽) 칸에 둡니다 |
| 앱 창: 작업 영역 분할 | 840×1210 (세로). 내비와 나란히 띄울 때입니다 |
| 디자인 기준 | Figma 앱 영역 840×1184. 창에 맞게 비율을 유지한 채 가장 크게 넣고 가운데 둡니다 |

## APK

`apk/pickme-0.1.0.apk` (release, 버전 0.1.0)
