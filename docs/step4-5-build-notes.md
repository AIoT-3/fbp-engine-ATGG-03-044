# Step 4~5 구현 메모

## 1. 이번 단계에서 추가된 핵심 클래스

- `RuleNode`
- `RuleExpression`
- `CompositeRuleNode`
- `TimeWindowRuleNode`

## 2. 구현하면서 중요했던 점

### 2-1. RuleNode는 메시지를 바꾸지 않고 분기만 한다

- `match`
- `mismatch`

분기만 담당하고 payload는 그대로 보내는 것이 가장 단순하고 테스트도 쉽다.

### 2-2. RuleExpression은 처음부터 크게 만들지 않는 게 맞다

현재 구현은:

- 숫자 비교
- 문자열 비교
- 단순 3토큰 표현식

만 지원한다.

괄호, 중첩 조건, 복합 표현식까지 한 번에 넣으면 디버깅 비용이 급격히 커진다.

### 2-3. CompositeRuleNode는 기본 동작 정의가 중요하다

빈 조건일 때 기본값을 먼저 정해야 테스트가 깔끔해진다.

- AND -> `match`
- OR -> `mismatch`

### 2-4. TimeWindowRuleNode는 이벤트 제거 순서가 핵심이다

이 순서가 맞다.

1. 조건 평가
2. 만족 시 시간 추가
3. 오래된 이벤트 제거
4. threshold 판단

이 순서가 바뀌면 alert 카운트가 쉽게 어긋난다.

## 3. 통합 구현에서 중요했던 점

### 3-1. RuleNode의 match는 fan-out이 필요하다

Step 4 통합 플로우는 `match`가 한 군데로만 가면 안 된다.

- MQTT 알림 발행
- MODBUS 제어 명령

둘 다 동시에 가야 하므로 출력 포트 다중 연결이 중요하다.

### 3-2. MODBUS 시뮬레이터 stop()은 기존 client socket도 닫아야 했다

기존에는 `ServerSocket`만 닫으면 accept만 멈추고 이미 연결된 client는 계속 살아 있었다.

그래서:

- “시뮬레이터 중지”
- “연결 끊김 처리”

테스트 의미가 약해졌다.

이번 정리에서는 active client socket도 같이 닫도록 보완했다.

### 3-3. MQTT 재연결 테스트와 장기 실행 테스트는 자동화 비용이 크다

다음 항목은 코드만으로 완전히 자동화하기 어렵다.

- Broker 재시작 후 재연결
- 5분 장기 안정성 테스트

그래서 현재는:

- 일반 integration test는 자동 실행 가능
- 수동 개입이 필요한 테스트는 `@Disabled`

로 분리했다.

## 4. 내 수준에서 특히 헷갈릴 수 있는 지점

### 4-1. RuleNode와 ThresholdFilterNode 차이

- `ThresholdFilterNode`
  - 숫자 하나 비교
  - alert / normal
- `RuleNode`
  - 조건식을 일반화
  - match / mismatch

즉 RuleNode는 ThresholdFilterNode의 일반화 버전으로 보면 된다.

### 4-2. RuleExpression과 CompositeRuleNode 차이

- `RuleExpression`
  - 조건 하나
  - 예: `temperature > 30`
- `CompositeRuleNode`
  - 조건 여러 개 조합
  - AND / OR

### 4-3. TimeWindowRuleNode는 “메시지 내용”보다 “시간 기록”이 핵심

이 노드는 메시지 자체를 저장하는 게 아니라,

- 조건을 만족한 시각 목록

을 관리해서 threshold를 판단한다.

## 5. 남은 확장 포인트

- Rule을 JSON/YAML로 외부화
- 재연결 테스트 완전 자동화
- MQTT/MODBUS end-to-end 성능 측정 고도화
- TimeWindowRuleNode의 중복 alert 억제 정책 추가
