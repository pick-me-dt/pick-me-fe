# 픽미 차량 앱 구조

단일 `:app` 모듈입니다. 최상위 패키지를 미래 모듈 경계로 나눴습니다. 나중에 모듈로 쪼갤 때도 패키지 단위로 옮기기만 하면 됩니다.

## 1. 폴더 구조

```
app/src/main/kotlin/com/pickmedt/car/
├─ app/        앱 시작과 연결
│              PickmeApplication, MainActivity, AppContainer(수동 DI), AppRoot(최상위 화면),
│              EtaReporter(진행 중 주문의 ETA 주기 전송)
├─ core/       Android·SDK에 의존하지 않는 순수 Kotlin
│  ├─ model/   주문·매장·금액·ETA·주행 신호 모델
│  ├─ rule/    2차 기획 규칙 (주행 맥락 판정, ETA 최신성, 추천 변경 강조)
│  └─ time/    Clock (테스트·시연에서 시간 주입)
├─ data/       서버와 주고받는 데이터
│  ├─ order/   OrderRepository 인터페이스와 값 타입
│  ├─ fake/    가짜 서버 (실서버 연동 전 개발·시연용)
│  └─ remote/  실서버 연동 (Retrofit·Socket.IO) — 11/13 예정
├─ platform/   차량 플랫폼 (Pleos SDK·AAOS Car API)
│  ├─ drive/   기어·속도·주행 규제 신호
│  │  ├─ fake/ 모의 신호
│  │  └─ car/  AAOS Car API 구현 — 게이트 G2·G4 이후
│  └─ navi/    매장 입구까지 남은 시간(ETA)
│     ├─ scripted/ 모의 ETA
│     └─ pleos/    NaviHelper 구현 — 게이트 G3 이후
├─ feature/    화면 (Figma 화면 코드 단위, 4장)
├─ demo/       시연 컨트롤 (Figma 프로토타입 하단 영역)
└─ ui/         공용 테마·컴포넌트

app/src/debug/   debug 빌드 전용 설정 (로컬 서버 평문 HTTP 허용)
app/src/test/    단위 테스트. main과 같은 패키지 구조, 공용 도구는 testing/
```

인터페이스는 영역 바로 아래(`data/order`, `platform/drive`, `platform/navi`)에 두고, 구현은 하위 패키지(`fake`, `remote`, `car`, `pleos`, `scripted`)에 둡니다.

## 2. 경계 규칙

빌드할 때마다 `checkBoundaries` 작업이 아래 import를 검사합니다. 위반하면 빌드가 실패합니다. 직접 실행하려면 `./gradlew :app:checkBoundaries`를 씁니다.

| 패키지 | import 금지 | 이유 |
| --- | --- | --- |
| `core` | `android.*`, `androidx.*`, Pleos SDK, 네트워크, 앱의 다른 패키지 | 규칙을 JVM 단위 테스트로 검증 |
| `data` | Pleos SDK, `platform`·`feature`·`ui`·`demo`·`app` | 서버 데이터와 차량 신호를 분리 |
| `data` (`remote` 제외) | 네트워크 (Retrofit·OkHttp·Socket.IO) | 네트워크 코드는 `data/remote`에만 |
| `platform` | 네트워크, `data`·`feature`·`ui`·`demo`·`app` | 차량 신호만 다룸 |
| `feature` | Pleos SDK, 네트워크, 구현 하위 패키지(`data/fake`·`data/remote`·`platform/*/*`), `demo`·`app` | 화면은 인터페이스만 사용. 가짜 ↔ 실서버, 모의 ↔ 실차 교체가 화면에 영향 없음 |
| `ui` | Pleos SDK, 네트워크, `data`·`platform`·`feature`·`demo`·`app` | 공용 컴포넌트는 데이터 출처를 모름 |
| `demo` | Pleos SDK, 네트워크, `feature`·`app`, `data/remote` | 시연 컨트롤은 가짜·모의 구현만 조작 |
| `app` | 없음 | 모든 것을 연결하는 곳 |

Pleos SDK(`ai.pleos.*`)와 AAOS Car API(`android.car.*`)는 `platform/` 아래에서만 import할 수 있습니다.

## 3. 구현 선택

가짜·실서버, 모의·실차 구현은 `app/AppContainer.kt`에서만 고릅니다. 화면과 규칙 코드는 어떤 구현이 쓰이는지 모릅니다.

| 인터페이스 | 지금 구현 | 다음 구현 |
| --- | --- | --- |
| `OrderRepository` | `data/fake/FakeOrderRepository` | `data/remote` (서버 2차 기획 반영 후) |
| `DriveSignalSource` | `platform/drive/fake/FakeDriveSignalSource` | `platform/drive/car` |
| `EtaSource` | `platform/navi/scripted/ScriptedEtaSource` | `platform/navi/pleos` (NaviHelper) |

## 4. 기능 패키지와 Figma 화면

Figma `prototype` 페이지의 화면 코드를 기준으로 나눕니다.

| 패키지 | Figma | 화면 |
| --- | --- | --- |
| `feature/launch` | V00 | 스플래시, 초기 상태 확인, 앱 실행 실패, 첫 이용 안내, 정보 이용 동의, 정보 접근 불가, 차량번호 등록·입력 오류, 진행 주문 복귀 |
| `feature/home` | V01 | 시작, 등록 내역 없음, 매장 선택, 메뉴 선택, 경로 없음, 이용 매장 없음 |
| `feature/reserve` | V02 | 예약 확인, 품절, 가격 변경 재확인, 매장 접수 종료 |
| `feature/progress` | V03·V04·V05·V07 | 매장 확인 중, 예약 거절, 응답 지연, 제조 대기·제조 중·준비 완료, 연결·ETA 오류, 제조 전·후 지연, 수령 완료 |
| `feature/order` | V06 | 주문 상세, 취소 요청 확인, 취소 확인 중, 취소 완료, 취소 거절, 주행 중 상세 제한 |
| `feature/settings` | V08 | 설정·등록 정보 관리, 이용 안내·정보 처리 |

매장 화면(S01~S03)은 백엔드 저장소의 매장 웹(`pick-me-be/public/`) 범위입니다.

## 5. 화면을 추가할 때

1. `feature/<영역>/`에 `<화면>Screen.kt`(Compose)와 필요하면 `<화면>ViewModel.kt`를 만듭니다.
2. 데이터는 `data/order`의 인터페이스, 차량 신호는 `platform/*`의 인터페이스만 받습니다.
3. 주행 중 허용 여부는 `core/rule`의 `resolveDriveContext()` 결과(`allowsDetail`)로 판단합니다.
4. 공용으로 쓰일 컴포넌트는 `ui/`로 올립니다.
