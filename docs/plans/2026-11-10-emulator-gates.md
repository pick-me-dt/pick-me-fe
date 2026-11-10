# 에뮬레이터 게이트 결과 (2026-11-10)

Pleos Connect Emulator v2.1.0(API 34, arm64)에서 앱을 실행해 확인한 결과입니다. Mac은 Apple M3 Pro 18GB입니다.

## 환경

- **AVD**: 시스템 이미지에 들어 있는 기기 정의 `Pleos_Connect_v2_0`(2560×1440, mdpi)로 `Pleos_Connect_v2_1`을 만들었습니다. 콜드 부팅을 켜고 RAM은 4GB입니다.
- **실행 명령**
  ```bash
  ~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2_1 -no-snapshot-load -no-snapshot-save -gpu host
  ```
- **Virtual Console**: `adb forward tcp:8088 tcp:8088` 후 `http://127.0.0.1:8088/`을 엽니다. 기어는 「Emulation Controls → Gear Selection」에 있습니다.

## 결과

| 게이트 | 결과 | 확인한 내용 |
| --- | --- | --- |
| G1 실행 | 통과 | 콜드 부팅 후 앱 설치·실행(시작 1.5초)이 됩니다. NaviHelper 초기화도 성공합니다. 차량 키보드(`ai.umos.inputmethod/.service.CarIME`)로 한글 차량번호를 입력하고 저장했습니다 |
| G2 기어 | 통과 | Virtual Console에서 P↔D를 바꾸면 앱 로그에 `기어 raw=8 → DRIVE`, `raw=4 → PARK`가 찍힙니다. Car API `GEAR_SELECTION`을 쓰고, 권한 `CAR_POWERTRAIN`은 normal입니다 |
| G3 ETA | 부분 통과 | 아래 「G3 상세」 참고 |
| G4 주행 규제 | 통과 (명령 제외) | D단이면 `CarUxRestrictions`가 바로 `requiresDO=true`(active=80)가 됩니다. 앱은 DO 선언으로 계속 보이고, 예약·등록 버튼은 비활성이 됩니다. 문서의 `adb shell cmd hmg_car_service … driving_regulation on`은 이 에뮬레이터에 서비스가 없어 실행되지 않습니다 (`Can't find service: hmg_car_service`) |
| G5 서버 | 확인 안 함 | 실서버 연동(11/13) 때 확인합니다 |

### G3 상세

- **필요 권한**: NaviHelper 호출에는 normal 권한 두 개가 필요합니다. 경로 정보 조회는 `pleos.car.permission.NAVI_ROUTE`, 경로 요청은 `pleos.car.permission.NAVI_ROUTE_SEARCH`입니다.
- **경로 요청**: `requestRoute`로 양재점(서버 시드 좌표 37.4705, 127.035)을 요청하면 내비게이션이 열리고 추천 경로 「28분 · 9.7km」가 나옵니다.
- **값과 단위**: `getDestinationInfo()`의 결과는 `remainTime=1680`, `remainDistance=9700`입니다. 따라서 **remainTime은 초, remainDistance는 미터**입니다(28분 = 1680초).
- **시간에 따른 변화는 없음**: 에뮬레이터에서는 차량 위치가 움직이지 않아 값이 그대로입니다.
  - 주차 25초: 1680 그대로
  - Virtual Console에서 D + 가속(최대 240km/h): 1680 그대로. 내비 화면도 「28분 · 9.7km」 그대로입니다
  - `adb emu geo fix`로 위치를 옮겨도 1680 그대로
  - Virtual Console에는 위치·경로 모의 기능이 없습니다
- **영상에 주는 영향**: 시연 영상의 ETA 변화는 모의 ETA(`ScriptedEtaSource`)로 보여 주고, 영상에 「모의」라고 표기해야 합니다(계획서 G3 대체안). 경로 요청과 첫 남은 시간(28분)은 실제 내비 값을 쓸 수 있습니다.

## 화면 크기

| Pleos 화면 배치 | 앱 창 크기 | Figma(840×1184)와 비교 |
| --- | --- | --- |
| 오른쪽 작업 영역 전체 | 1682×1210 (가로) | 비율이 다릅니다 |
| 작업 영역 분할(⋮ 손잡이) | 840×1210 (세로) | 폭이 Figma와 같습니다 |

- **바꾼 점**: 처음에는 폭 기준으로 배율을 계산해 전체 영역에서 화면이 2배로 커져 잘렸습니다. 이제 `min(폭÷840, 높이÷1184)`로 계산하고 Figma 비율 영역을 가운데 둡니다.
- **시연 컨트롤 위치**: 가로 창에서는 앱 영역 오른쪽에, 세로 창에서는 아래에 둡니다. 분할 영역(840×1210)에서는 시연 컨트롤만큼 앱 영역이 줄어듭니다(약 0.78배). 촬영할 때는 「접기」로 줄입니다.
- **확인 안 함**: 전체 영역(1682×1210)에서 시연 컨트롤을 옆에 둔 화면은 아직 보지 않았습니다. 확인한 것은 분할 영역뿐입니다.

## 남은 일

- **NaviHelper 정식 구현**: 지금 코드(`platform/navi/pleos/PleosNaviProbe`)는 확인용입니다. 다음을 `EtaSource`·`RouteSource` 구현으로 옮겨야 합니다.
  - 경로 유무: `RouteStateInfo`. 확인 결과 안내 중에도 `isRouting=false`, `naviMode=ROUTE_GUIDING`이므로 경로 유무 판단은 `naviMode`로 해야 합니다.
  - 남은 시간: `DestinationInfo.remainTime`
  - 「내비게이션 열기」: `requestNavigationScreen`
- **속도 권한**: `CAR_SPEED`는 런타임 허용이 필요한 권한이라 지금은 허용되지 않은 상태입니다. 화면은 주차 여부(기어·주행 규제)만 쓰므로 영향은 없습니다.
