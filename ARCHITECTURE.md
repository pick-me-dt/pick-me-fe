# 픽미 차량 앱 구조

단일 `:app` 모듈입니다. 최상위 패키지를 미래 모듈 경계로 나눴습니다. 나중에 모듈로 쪼갤 때도 패키지 단위로 옮기기만 하면 됩니다.

## 1. 폴더 구조

```
app/src/main/kotlin/com/pickmedt/car/
├─ app/        앱 시작과 연결
│              PickmeApplication, MainActivity, AppContainer(수동 DI), AppRoot(최상위 화면),
│              EtaReporter(진행 중 주문의 ETA 주기 전송)
├─ core/       Android·SDK에 의존하지 않는 순수 Kotlin
│  ├─ model/   주문·매장·메뉴·금액·ETA·경로·주행 신호 모델
│  ├─ rule/    2차 기획 규칙 (주행 맥락 판정, ETA 최신성, 추천 변경 강조), 차량번호 형식
│  └─ time/    Clock (테스트·시연에서 시간 주입)
├─ data/       서버와 주고받는 데이터
│  ├─ order/    OrderRepository 인터페이스와 값 타입 (견적·예약 전 확인 조건)
│  ├─ catalog/  CatalogRepository: 경로상 매장, 매장 메뉴
│  ├─ settings/ UserSettingsRepository: 정보 이용 동의, 차량번호
│  ├─ fake/     가짜 서버·가짜 매장 목록·메모리 설정 (실서버 연동 전 개발·시연·테스트용)
│  ├─ local/    기기 저장 (SharedPreferences 설정)
│  └─ remote/   실서버 연동 (OkHttp·kotlinx.serialization·Socket.IO). 1차 기획 서버 기준, 2차 기획과 다른 점은 앱에서 메움
├─ platform/   차량 플랫폼 (Pleos SDK·AAOS Car API)
│  ├─ drive/   기어·속도·주행 규제 신호
│  │  ├─ fake/ 모의 신호
│  │  └─ car/  AAOS Car API 구현 (기어·속도·주행 규제)
│  └─ navi/    매장 입구까지 남은 시간(ETA), 내비게이션 경로
│     ├─ scripted/ 모의 ETA·모의 경로, 실측 첫 값으로 시작하는 모의 ETA(NaviSeededEtaSource)
│     └─ pleos/    NaviHelper 구현(PleosNavi): 경로 유무·남은 시간·내비 열기
├─ feature/    화면 (Figma 화면 코드 단위, 4장)
│              <화면>Screen(상태 없는 Compose) + toXxxUiState()·xxxMessage()(순수 함수, 단위 테스트)
│              여러 화면을 오가는 흐름은 <영역>Controller(단계 StateFlow, 단위 테스트)
├─ demo/       시연 컨트롤 (Figma 프로토타입 하단 영역)
└─ ui/
   ├─ theme/     색 토큰, Asta Sans 글자 스타일, DesignScale(Figma 840 폭 비율)
   ├─ component/ Pleos 공식 컴포넌트(ivi_title·ivi_basic_button·ivi_text_field·체크박스)와
   │             픽미 공용 컴포넌트(문구형 화면 MessageScreen, 카드형 본문 StatusCardBody 등)
   └─ text/      조사·시각·금액 문구

app/src/main/res/  drawable(Figma SVG를 변환한 벡터), font(Asta Sans)
app/src/debug/     debug 빌드 전용 설정 (로컬 서버 평문 HTTP 허용)
app/src/test/      단위 테스트. main과 같은 패키지 구조, 공용 도구는 testing/, 화면 스크린샷은 screenshot/
design/            Figma SVG 원본(figma-assets/), 글꼴 라이선스(licenses/)
```

### 크기 단위

화면 수치는 Figma 앱 영역(840×1184) 값을 그대로 `designDp`·`designSp`로 씁니다(예: `40.designDp`). `ProvideDesignScale`은 `min(창 폭÷840, 창 높이÷1184)` 비율로 Figma 영역을 가장 크게 넣고 가운데 둡니다. Pleos 작업 영역은 전체일 때 1682×1210(가로), 분할일 때 840×1210(세로)입니다 ([게이트 결과](docs/plans/2026-11-10-emulator-gates.md)).

### 화면 확인

에뮬레이터 없이 JVM에서 화면을 840×1184 PNG로 그립니다(Roborazzi). 결과는 `app/build/outputs/roborazzi/`에 생깁니다.

```bash
./gradlew :app:recordRoborazziDebug
```

인터페이스는 영역 바로 아래(`data/order`, `data/catalog`, `data/settings`, `platform/drive`, `platform/navi`)에 두고, 구현은 하위 패키지(`fake`, `local`, `remote`, `car`, `pleos`, `scripted`)에 둡니다.

## 2. 경계 규칙

빌드할 때마다 `checkBoundaries` 작업이 아래 import를 검사합니다. 위반하면 빌드가 실패합니다. 직접 실행하려면 `./gradlew :app:checkBoundaries`를 씁니다.

| 패키지 | import 금지 | 이유 |
| --- | --- | --- |
| `core` | `android.*`, `androidx.*`, Pleos SDK, 네트워크, 앱의 다른 패키지 | 규칙을 JVM 단위 테스트로 검증 |
| `data` | Pleos SDK, `platform`·`feature`·`ui`·`demo`·`app` | 서버 데이터와 차량 신호를 분리 |
| `data` (`remote` 제외) | 네트워크 (Retrofit·OkHttp·Socket.IO) | 네트워크 코드는 `data/remote`에만 |
| `platform` | 네트워크, `data`·`feature`·`ui`·`demo`·`app` | 차량 신호만 다룸 |
| `feature` | Pleos SDK, 네트워크, 구현 하위 패키지(`data/fake`·`data/local`·`data/remote`·`platform/*/*`), `demo`·`app` | 화면은 인터페이스만 사용. 가짜 ↔ 실서버, 모의 ↔ 실차 교체가 화면에 영향 없음 |
| `ui` | Pleos SDK, 네트워크, `data`·`platform`·`feature`·`demo`·`app` | 공용 컴포넌트는 데이터 출처를 모름 |
| `demo` | Pleos SDK, 네트워크, `feature`·`app`, `data/remote` | 시연 컨트롤은 가짜·모의 구현만 조작 |
| `app` | 없음 | 모든 것을 연결하는 곳 |

Pleos SDK(`ai.pleos.*`)와 AAOS Car API(`android.car.*`)는 `platform/` 아래에서만 import할 수 있습니다.

## 3. 구현 선택

가짜·실서버, 모의·실차 구현은 `app/AppContainer.kt`에서만 고릅니다. 화면과 규칙 코드는 어떤 구현이 쓰이는지 모릅니다.

| 인터페이스 | 지금 구현 | 다음 구현 |
| --- | --- | --- |
| `OrderRepository` | 빌드 옵션 `-Ppickme.serverUrl`이 있으면 `data/remote/RemoteOrderRepository`, 없으면 `data/fake/FakeOrderRepository` | 서버가 2차 기획을 반영하면 상태 대응표·타이밍 계산 정리 ([실서버 테스트](docs/plans/2026-11-10-server-e2e.md)) |
| `CatalogRepository` | 같은 기준으로 `data/remote/RemoteCatalog` / `data/fake/FakeCatalog` | — |
| `UserSettingsRepository` | `data/local/PrefsUserSettings` (테스트는 `data/fake/InMemoryUserSettings`) | 차량번호 서버 등록 API가 정해지면 함께 저장 |
| `DriveSignalSource` | 차량(AAOS): `platform/drive/car/CarDriveSignalSource`, 그 밖: `platform/drive/fake/FakeDriveSignalSource` | — |
| `EtaSource` | 차량: `NaviSeededEtaSource`(실제 내비 첫 값 + 모의 감소), 그 밖: `platform/navi/scripted/ScriptedEtaSource` | 실제 위치가 움직이는 환경(실차·모의 주행)에서는 `PleosNavi`만 사용 |
| `RouteSource` | 차량: `platform/navi/pleos/PleosNavi`, 그 밖: `platform/navi/scripted/ScriptedRouteSource` | — |

## 4. 기능 패키지와 Figma 화면

Figma `prototype` 페이지의 화면 코드를 기준으로 나눕니다.

| 패키지 | Figma | 화면 |
| --- | --- | --- |
| `feature/launch` | V00 | 스플래시, 초기 상태 확인, 앱 실행 실패, 첫 이용 안내, 정보 이용 동의, 정보 접근 불가, 차량번호 등록·입력 오류, 진행 주문 복귀 |
| `feature/home` | V01 | 시작, 등록 내역 없음, 매장 선택, 메뉴 선택, 경로 없음, 이용 매장 없음 |
| `feature/reserve` | V02 | 예약 확인, 품절, 가격 변경 재확인, 매장 접수 종료 |
| `feature/progress` | V03·V04·V05·V07, V06-C·D·E | 주문 단계에 따라 자동으로 바뀌는 화면: 매장 확인 중, 예약 거절, 응답 지연, 제조 대기·제조 중·준비 완료, 연결·ETA 오류, 제조 전·후 지연, 수령 완료, 취소 확인 중·완료·거절 |
| `feature/order` | V06-A·B·L | 사용자가 여는 화면: 주문 상세, 취소 요청 확인, 주행 중 상세 제한 |
| `feature/settings` | V08 | 설정·등록 정보 관리, 이용 안내·정보 처리 |

- V06의 취소 확인 중·완료·거절은 주문 단계(서버 상태)로 결정되므로 `feature/progress`가 그립니다. 같은 카드형 본문은 `ui/component/StatusCardBody`를 함께 씁니다.
- V00·V01 예외·V02 예외·V08은 「eyebrow → 제목 → 안내 → 작은 안내 + 버튼 목록」 구성이라 `ui/component/MessageScreen` 하나로 그립니다. 기능 패키지는 문구와 버튼을 정하는 순수 함수(`xxxMessage()`)만 둡니다.
- 홈 흐름(V01·V02)은 `feature/home/HomeController`가 단계를 들고, V02 화면은 `feature/reserve`가 그립니다. 둘을 잇는 일은 `app/AppRoot`가 합니다.
- 설정의 차량번호 변경은 `feature/launch`의 차량번호 화면(V00-I·IE)을 다시 씁니다. 기능 패키지끼리 import하지 않도록 `app/AppRoot`에서 연결합니다.

매장 화면(S01~S03)은 백엔드 저장소의 매장 웹(`pick-me-be/public/`) 범위입니다.

## 5. 화면을 추가할 때

1. `feature/<영역>/`에 `<화면>Screen.kt`(Compose)와 필요하면 `<화면>ViewModel.kt`를 만듭니다.
2. 데이터는 `data/order`의 인터페이스, 차량 신호는 `platform/*`의 인터페이스만 받습니다.
3. 주행 중 허용 여부는 `core/rule`의 `resolveDriveContext()` 결과(`allowsDetail`)로 판단합니다.
4. 공용으로 쓰일 컴포넌트는 `ui/`로 올립니다.
