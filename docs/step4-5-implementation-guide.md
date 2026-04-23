# Step 4~5 구현/확인 가이드

## 1. 목적

이 문서는 Step 4와 Step 5를 한 번에 밀지 않고, **구현 순서**, **확인 순서**, **테스트 순서**로 나눠서 따라갈 수 있게 정리한 문서다.

현재 기준으로 이미 준비된 선행 구성은 다음과 같다.

- MQTT
  - `MqttSubscriberNode`
  - `MqttPublisherNode`
- MODBUS
  - `ModbusTcpClient`
  - `ModbusTcpSimulator`
  - `ModbusReaderNode`
  - `ModbusWriterNode`
- 실행 확인용 runner
  - MQTT runner
  - MODBUS runner

즉 Step 4부터는 **규칙 처리 레이어**를 올리고, Step 5에서는 **MQTT + MODBUS + Rule**을 묶은 통합 검증으로 넘어가면 된다.

## 2. 권장 구현 순서

### 2-1. RuleNode 먼저 구현

가장 먼저 만들 클래스:

- `src/main/java/com/fbp/engine/node/RuleNode.java`

최소 요구:

- `AbstractNode` 상속
- 입력 포트 `"in"`
- 출력 포트 `"match"`, `"mismatch"`
- `Predicate<Message>` 기반 생성자
- `onProcess()`에서 `condition.test(message)` 결과에 따라 분기

이걸 먼저 하는 이유:

- 구현이 가장 단순하다.
- 이후 `RuleExpression`, `CompositeRuleNode`, `TimeWindowRuleNode` 전부의 기준점이 된다.
- MQTT/MODBUS 통합 플로우에 바로 끼울 수 있다.

### 2-2. RuleNodeTest 바로 작성

테스트 파일:

- `src/test/java/com/fbp/engine/Node/RuleNodeTest.java`

먼저 닫아야 하는 항목:

1. 조건 만족 -> `match`
2. 조건 불만족 -> `mismatch`
3. 포트 구성
4. null 필드 처리
5. 다수 메시지 분기

이 단계에서 `CollectorNode`를 사용하면 된다.

### 2-3. RuleExpression 구현

다음 클래스:

- `src/main/java/com/fbp/engine/core/RuleExpression.java`

지원 범위:

- 연산자: `>`, `>=`, `<`, `<=`, `==`, `!=`
- 숫자 비교
- 문자열 비교

권장 구현 순서:

1. `parse(String expression)`
2. 내부 필드:
   - `field`
   - `operator`
   - `value`
3. `evaluate(Message message)`

처음에는 아래 같은 단순 표현식만 지원하면 충분하다.

- `"temperature > 30.0"`
- `"status == ON"`

복잡한 괄호, AND/OR, 함수 호출은 여기서 하지 않는 것이 맞다.

### 2-4. RuleExpressionTest 작성

테스트 파일:

- `src/test/java/com/fbp/engine/core/RuleExpressionTest.java`

닫아야 하는 항목:

1. 숫자 비교
2. 문자열 비교
3. 모든 연산자
4. 잘못된 표현식
5. 필드 없음

### 2-5. RuleNode 문자열 생성자 추가

그 다음에 `RuleNode` 오버로딩 생성자를 추가한다.

예:

```java
public RuleNode(String id, String expression) {
    this(id, RuleExpression.parse(expression)::evaluate);
}
```

이렇게 하면 Step 4-2가 `RuleExpression`과 자연스럽게 연결된다.

### 2-6. CompositeRuleNode 구현

파일:

- `src/main/java/com/fbp/engine/node/CompositeRuleNode.java`

이건 도전 과제지만, 구현하려면 아래 순서가 좋다.

1. `Operator` enum 추가
   - `AND`
   - `OR`
2. `conditions(List<Predicate<Message>>)` 저장
3. `addCondition(Predicate<Message>)`
4. `addCondition(String field, String op, Object value)`
5. `onProcess()`에서 operator에 따라 평가

기본 동작은 명세대로 두면 된다.

- AND + 빈 조건 -> `match`
- OR + 빈 조건 -> `mismatch`

### 2-7. TimeWindowRuleNode는 마지막에 구현

파일:

- `src/main/java/com/fbp/engine/node/TimeWindowRuleNode.java`

이건 가장 마지막에 두는 게 맞다.

이유:

- 시간 창 관리
- 큐 정리
- 중복 alert 처리 기준
- 테스트 타이밍

이 네 가지가 동시에 들어가서 Step 4에서 제일 까다롭다.

권장 구현 순서:

1. `Predicate<Message> condition`
2. `windowMs`
3. `threshold`
4. `Queue<Long> events`
5. `onProcess()`에서
   - 조건 평가
   - 만족 시 시간 기록
   - 오래된 이벤트 제거
   - threshold 판단 후 `alert` 또는 `pass`

## 3. 통합 플로우 구현 순서

### 3-1. Step 4-4 End-to-End 플로우

권장 runner:

- `src/main/java/com/fbp/engine/demo/runner/MqttModbusRuleRunner.java`

권장 흐름:

```text
MqttSubscriberNode(sensor/temp)
  -> RuleNode(temperature > 30)
    -> match -> MqttPublisherNode(alert/temp)
             -> ModbusWriterNode(registerAddress=2, fixedValue=1)
    -> mismatch -> LogNode
```

여기서 핵심은 `match` 경로를 두 갈래로 보내는 것이다.

1. MQTT 알림
2. MODBUS 제어

현재 `OutputPort`가 다중 연결을 지원하면 그대로 두 갈래 연결하면 된다.

### 3-2. Step 5 시나리오 구현 순서

Step 5는 아래 순서가 가장 안전하다.

1. MQTT -> Rule -> MQTT
2. Timer -> MODBUS Reader -> Rule -> MODBUS Writer
3. MQTT -> Rule -> MODBUS

이 순서가 좋은 이유:

- MQTT만 먼저 검증 가능
- MODBUS만 먼저 검증 가능
- 마지막에 크로스 프로토콜을 붙이면 원인 분리가 쉽다

## 4. 테스트 구현 순서

### 4-1. 단위 테스트 먼저

권장 순서:

1. `RuleNodeTest`
2. `RuleExpressionTest`
3. `CompositeRuleNodeTest`
4. `TimeWindowRuleNodeTest`

이 단계는 외부 시스템 없이 끝내는 게 맞다.

### 4-2. MQTT/MODBUS 통합 테스트는 마지막

권장 파일 구조:

- `MqttModbusIntegrationTest`
- `MqttIntegrationTest`
- `ModbusIntegrationTest`
- `CrossProtocolIntegrationTest`

이 중 우선순위는 다음과 같다.

1. `MqttModbusIntegrationTest`
2. `MqttIntegrationTest`
3. `ModbusIntegrationTest`
4. `CrossProtocolIntegrationTest`

이유:

- Step 4 과제 범위는 Rule 중심이다.
- Step 5는 환경/부하/장기 실행까지 포함된다.

## 5. 파일을 보는 순서

구현 후 코드를 읽을 때는 이 순서가 가장 좋다.

1. `ProtocolNode`
2. `MqttSubscriberNode`
3. `MqttPublisherNode`
4. `ModbusReaderNode`
5. `ModbusWriterNode`
6. `RuleNode`
7. `RuleExpression`
8. `CompositeRuleNode`
9. `TimeWindowRuleNode`
10. Step 4 runner
11. Step 5 integration tests

이 순서가 좋은 이유:

- 바깥 프로토콜 계층을 먼저 이해하고
- 그 위에 얹는 규칙 계층을 본 뒤
- 마지막에 플로우와 테스트를 보는 구조이기 때문이다.

## 6. 구현 체크리스트

### Step 4

- [ ] `RuleNode`
- [ ] `RuleNodeTest`
- [ ] `RuleExpression`
- [ ] `RuleExpressionTest`
- [ ] `RuleNode` 문자열 생성자
- [ ] `CompositeRuleNode` (선택)
- [ ] `CompositeRuleNodeTest` (선택)
- [ ] `TimeWindowRuleNode` (도전)
- [ ] `TimeWindowRuleNodeTest` (도전)
- [ ] MQTT + MODBUS + Rule runner

### Step 5

- [ ] MQTT 시나리오 1
- [ ] MODBUS 시나리오 2
- [ ] 크로스 프로토콜 시나리오 3
- [ ] 에러 시뮬레이션 3개 이상
- [ ] throughput / latency 측정
- [ ] integration test 분리
- [ ] `pom.xml`의 `integration` 태그 분리

## 7. 실행 순서 제안

실제로 작업할 때는 아래 순서로 가면 된다.

1. `RuleNode`
2. `RuleNodeTest`
3. `RuleExpression`
4. `RuleExpressionTest`
5. `RuleNode` 문자열 생성자
6. `demo.runner.MqttModbusRuleRunner`
7. `CompositeRuleNode`
8. `TimeWindowRuleNode`
9. Step 5 시나리오 runner
10. Step 5 integration test
11. 에러 시뮬레이션 정리
12. 성능 측정 정리

## 8. 지금 기준으로 가장 먼저 할 것

현재 기준으로 바로 시작할 첫 작업은 이 두 개다.

1. `RuleNode.java`
2. `RuleNodeTest.java`

여기까지 닫히면 Step 4의 중심이 잡힌다.
