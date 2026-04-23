# FBP Engine 코드 리뷰

> 이 문서는 코드 리뷰 기준 문서다. 2026-04-20 기준 일부 항목은 이미 해결되었고, 해결 여부는 아래 상태 요약을 기준으로 본다.

---

## 목차

0. [해결 상태 요약](#0-해결-상태-요약)
1. [과도한(Over-engineering) 부분](#1-과도한over-engineering-부분)
2. [부족한(Under-engineering) 부분](#2-부족한under-engineering-부분)
3. [설계 불일치 / 혼재 패턴](#3-설계-불일치--혼재-패턴)
4. [개선 제안 요약](#4-개선-제안-요약)

---

## 0. 해결 상태 요약

### 해결 완료

- `ProtocolNode.reconnect()`의 scheduler 중복 생성 문제 해결
- `Flow`에서 실제 connection 저장을 단일 메타 구조로 정리하고 조회 API 유지
- `ModbusTcpClient`의 레거시 호환 생성자/메서드 제거
- `MqttSubscriberNode` / `MqttPublisherNode` / `MqttPathDemo`의 무의미한 `persistBuffer` 설정 제거
- `FilterNode`의 `matches()` / `onProcess()` 중복 로직 제거
- `SplitNode`의 부모 `id` shadowing 제거
- `MergeNode`가 실제 입력 포트별로 `_inputPort`를 주입하도록 수정
- `CounterNode`를 `AtomicInteger` 기반으로 수정
- `TimerNode`를 `AtomicInteger` 기반으로 수정
- `SensorNode`를 추상 공통 부모로 구현하고 `TemperatureSensorNode`, `HumiditySensorNode` 공통화
- `HumiditySensorNode`의 FQN 직접 사용 제거
- `EchoProtocolNode.onProcess()` 구현 추가
- 미사용 `MessageListener`, `ProtocolCallback` 제거
- `App.java`의 Maven archetype 기본 `Hello World` 제거
- `pom.xml`의 `release`/`source`/`target` 중복 제거 및 archetype 잔재 제거
- `GeneratorNode`를 `AbstractNode` 상속 구조로 정리
- `FlowEngine`가 flow connection worker thread lifecycle을 직접 관리하도록 수정
- `Flow`에 `getConnection(String connectionId)`와 engine용 connection route 조회 추가
- engine/node 레벨 `System.out.println()`을 logging 중심으로 정리
- connection 인덱스 접근 runner를 ID 기반 조회로 정리
- 학습용 `Demo` 코드를 `src/test/java/com/fbp/engine/demo`로 이동
- `FlowEnginRunner` 오타를 `FlowEngineRunner`로 수정
- `App`, `MqttPathDemo`, `TcpEchoClient`, `TcpEchoServer` 출력/오류 처리를 logging 기반으로 정리

### 아직 남아 있음

- runner 클래스 수 과다 문제
- `Connection`의 `deliver()` / `poll()` 실패 처리 비대칭
- `AbstractNode`의 포트 getter 공개 범위 설계
- `demo.runner` 예제 출력은 여전히 `System.out.println()` 사용
  - 이 부분은 학습용 CLI 안내/결과 출력 성격이 강해서 의도적으로 남겨둔 항목이다.

---

## 1. 과도한(Over-engineering) 부분

### 1-1. `Flow` 클래스 — 연결 메타 정보 단일 관리로 정리됨

**파일**: `core/Flow.java`

이 항목은 정리됐다.

- `@Data` 제거
- 실제 저장 구조는 `connectionInfos` 단일 리스트
- `getConnections()`는 읽기 전용 뷰를 만들어 반환
- `getConnection(String connectionId)`와 engine용 route API 추가

즉, 이전의 “실제 연결 리스트 + 메타 리스트” 이중 저장 문제는 해소된 상태다.

---

### 1-2. Runner 클래스 — `demo.runner`로 분리됐지만 수는 여전히 많음

**파일들**: `demo/runner/*.java`

모든 Runner가 동일한 패턴으로 Thread를 개별 생성합니다:

```java
// 각 Runner마다 이 구조가 반복됨
Thread xxxxThread = new Thread(() -> {
    while (running) {
        try {
            Message message = connection.poll();
            node.process(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }
});
```

위치는 `com.fbp.engine.demo.runner`로 정리했지만, 학습 단계별 runner 수 자체는 여전히 많다.

> **제안**: 모든 Runner에서 `RunnerSupport.startWorker()`를 일관되게 사용하거나, 아니면 `RunnerSupport`를 제거하고 사용 위치를 통일.

---

### 1-3. Runner 클래스 — 위치는 정리됐지만 수는 많음

**파일들**: `src/main/java/com/fbp/engine/demo/runner` 아래 다수 파일

`FlowRunner`, `FourNodePipeRunner`, `ThreeNodeRunner`, `MainRunner`, `FlowEngineRunner`, `FinalFlowEngineRunner`, `FlowEngineCliRunner`, `MultiFlowRunner`, `SplitNodeRunner`, `TransformNodeRunner`, `TemperatureMonitorRunner`, `Step5PerformanceRunner`, `ModbusReaderRunner`, `ModbusAlertRunner`, `ModbusTcpRunner`, `MqttPublisherRunner`, `MqttSubscriberPrintRunner`, `MqttBidirectionalRunner`, `MqttRuleToMqttRunner`, `MqttRuleToModbusRunner`, `MqttModbusRuleRunner`, `TimerModbusRuleRunner`, `TimeFilterRunner`, `EchoProtocolRunner`

이들은 대부분 학습 단계별 실습 코드다.  
현재는 production 핵심 코드와 분리해 `demo.runner` 패키지로 이동했지만, 클래스 수가 많아 탐색 비용은 여전히 있다.

> **현재 상태**: `demo.runner`로 분리 완료  
> **추가 제안**: 단계별 서브패키지(`demo.step1`, `demo.step2` 등)까지 나누면 더 읽기 쉬워진다.

---

### 1-4. `ModbusTcpClient` — 레거시 호환 생성자

**파일**: `protocol/ModbusTcpClient.java`

```java
// 기존 코드 호환용 생성자
public ModbusTcpClient(int timeoutMs) {
    this.host = null;
    this.port = 0;
}

// 기존 코드 호환용 메서드
public void connect(String host, int port) throws IOException {
    ...
}
```

`timeoutMs` 파라미터를 받지만 실제로 적용하지 않고 내부에서 `socket.setSoTimeout(3000)`으로 하드코딩되어 있습니다.  
이 생성자를 사용하면 `host == null` 상태로 `connect()` 호출 시 `IllegalStateException`이 발생하므로 API가 혼란스럽습니다.

> **제안**: 레거시 생성자를 제거하거나, `timeoutMs`를 실제로 사용하도록 수정.

---

### 1-5. `ProtocolNode.reconnect()` — `reconnectScheduler`가 여러 번 생성될 수 있음

**파일**: `core/ProtocolNode.java`

`reconnect()`가 호출될 때마다 새 `ScheduledExecutorService`를 생성합니다.  
만약 `initialize()` 실패 → `reconnect()` → 외부에서 다시 `initialize()` 호출 등 시나리오에서 이전 스케줄러가 제대로 종료되지 않을 수 있습니다.

```java
protected void reconnect() {
    reconnectScheduler = Executors.newSingleThreadScheduledExecutor(); // 이전 것 종료 안 됨
    ...
}
```

> **제안**: `reconnect()` 시작 전에 기존 `reconnectScheduler`가 null이 아니면 먼저 `shutdown()` 호출.

---

### 1-6. `MqttPublisherNode` / `MqttSubscriberNode` — `DisconnectedBufferOptions` 설정이 사실상 무의미

**파일**: `node/MqttPublisherNode.java`, `node/MqttSubscriberNode.java`

```java
DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
bufferOptions.setBufferEnabled(false);
bufferOptions.setPersistBuffer(false);
client.setBufferOpts(bufferOptions);
```

`setBufferEnabled(false)`로 버퍼를 비활성화했으므로 `setPersistBuffer(false)` 설정은 아무 의미가 없습니다. 또 `MqttPathDemo.java`에도 동일 패턴이 중복됩니다.

> **제안**: `setBufferEnabled(false)` 한 줄로 충분. `DisconnectedBufferOptions` 객체 생성 자체가 불필요.

---

### 1-7. `Demo` 패키지 — test 경로로 이동됨

**파일들**: `src/test/java/com/fbp/engine/demo/BlockingQueueDemo.java`, `src/test/java/com/fbp/engine/demo/SynchronizedBufferDemo.java`, `src/test/java/com/fbp/engine/demo/UnSafeBufferDemo.java`

이 항목도 정리됐다.

- 패키지명: `com.fbp.engine.demo`
- 위치: `src/test/java`

즉 학습용 예제가 production 경로에 섞여 있던 문제는 해결된 상태다.

---

### 1-8. `FilterNode` — `matches()` 메서드와 `onProcess()` 로직 중복

**파일**: `node/FilterNode.java`

```java
public boolean matches(Message message) {
    // ... 동일 판단 로직
}

@Override
protected void onProcess(Message message) {
    // ... 동일 판단 로직 다시 작성
}
```

`matches()`와 `onProcess()` 내부의 숫자 비교 로직이 완전히 중복됩니다.  
`onProcess()`에서 `matches()`를 호출하면 중복이 제거됩니다.

> **제안**:
> ```java
> @Override
> protected void onProcess(Message message) {
>     if (matches(message)) {
>         send("out", message);
>     }
> }
> ```

---

### 1-9. `SplitNode` — `id`/`key`/`threshold` 필드 중복 선언

**파일**: `node/SplitNode.java`

```java
public class SplitNode extends AbstractNode {
    private String id;   // AbstractNode에 이미 있음
    private String key;
    private double threshold;
```

`id`는 부모 클래스 `AbstractNode`에 이미 `private final String id`로 선언되어 있어 자식 클래스에서 다시 선언하면 부모의 필드를 가립니다(field shadowing).

> **제안**: `SplitNode`에서 `private String id;` 선언 제거.

---

## 2. 부족한(Under-engineering) 부분

### 2-1. `FlowEngine` — 스레드 생명주기 미관리

**파일**: `core/FlowEngine.java`

`FlowEngine`은 Flow를 등록하고 `startFlow()` / `stopFlow()`를 제공하지만, 실제로 **노드 처리 스레드를 생성하거나 관리하지 않습니다**.  
`startFlow()`는 `flow.initialize()`만 호출할 뿐이고, 스레드 시작/종료는 각 Runner에서 수동으로 처리합니다.

즉, `FlowEngine`이라는 이름을 가지고 있지만 스레드 기반의 실행 루프를 전혀 담당하지 않아 **진정한 엔진** 역할을 하지 못합니다.

> **제안**: `FlowEngine.startFlow()`에서 각 Connection에 대한 워커 스레드(RunnerSupport 활용)를 자동으로 시작하고 `stopFlow()`에서 정리하는 로직 추가.

---

### 2-2. `Connection` — `deliver()`와 `poll()`의 예외 처리 비대칭

**파일**: `core/Connection.java`

`deliver()`는 `InterruptedException`을 throws하는데, `DefaultOutputPort.send()`에서는 이를 catch하고 즉시 return합니다.  
반면 Runner 내 `poll()`은 각 스레드에서 개별 처리합니다.  
`deliver()` 실패(큐 가득 참) 상황에서 메시지 유실이 어디서 발생하는지 명시적으로 표시되지 않습니다.

> **제안**: `Connection`에 `deliver()` 실패 카운터나 콜백을 추가하거나, `DefaultOutputPort`에서 예외를 로깅.

---

### 2-3. `AbstractNode` — `getOutputPort()`/`getInputPort()` 접근 제어 일관성 부재

**파일**: `core/AbstractNode.java`

- `getOutputPort(String name)` → `public`
- `getInputPort(String name)` → `public`
- `send(String name, Message)` → `protected`

외부에서 노드의 포트를 직접 가져올 수 있어 FBP의 캡슐화 원칙에 어긋납니다.  
포트는 `Flow.connect()`를 통해서만 연결되어야 하는데, 현재 Runner들에서 `node.getOutputPort("out").connect(connection)` 같은 직접 접근 코드가 혼재합니다.

> **제안**: `getOutputPort()` / `getInputPort()`를 `package-private`으로 변경하거나, `Flow` 클래스만 접근 가능하도록 제한.

---

### 2-4. `MergeNode` — `_inputPort` 키에 의존하는 암묵적 규약

**파일**: `node/MergeNode.java`

```java
String inputPort = message.get("_inputPort");
```

`MergeNode`는 메시지 페이로드에 `"_inputPort"` 키가 있어야 작동합니다.  
그런데 이 키를 메시지에 넣는 코드가 어디에도 없습니다. 즉, **현재 코드만으로는 `MergeNode`가 정상 동작하지 않습니다**.

`DefaultInputPort.receive()`가 단순히 `owner.process(message)`를 호출할 뿐, 어떤 포트로 들어왔는지 정보를 주입하지 않습니다.

> **제안**: `DefaultInputPort.receive()`에서 메시지에 `_inputPort` 키를 주입하거나, `MergeNode`가 포트별 별도 `InputPort` 구현을 사용하도록 변경.
>
> ```java
> // DefaultInputPort.receive() 개선 예시
> @Override
> public void receive(Message message) {
>     owner.process(message.withEntry("_inputPort", name));
> }
> ```

---

### 2-5. `CounterNode` — `count` 필드 스레드 안전하지 않음

**파일**: `node/CounterNode.java`

```java
private int count = 0;

@Override
protected void onProcess(Message message) {
    count++;
    ...
}
```

`count++`는 원자적 연산이 아닙니다. 멀티스레드 환경에서 여러 스레드가 동시에 `process()`를 호출하면 카운트가 손실될 수 있습니다.

> **제안**: `count`를 `AtomicInteger`로 변경.
>
> ```java
> private final AtomicInteger count = new AtomicInteger(0);
> // count++ → count.incrementAndGet()
> ```

---

### 2-6. `TimerNode` — `tickCount` 스레드 안전하지 않음

**파일**: `node/TimerNode.java`

```java
private int tickCount;

scheduler.scheduleAtFixedRate(() -> {
    Message message = new Message(Map.of("tick", tickCount, ...));
    send("out", message);
    tickCount++;
}, ...);
```

`ScheduledExecutorService`의 태스크 스레드와 외부 접근이 동시에 발생할 경우 `tickCount`가 안전하지 않습니다.  
또한 `tickCount`는 `long`보다는 `AtomicLong`을 사용하는 것이 적합합니다.

> **제안**: `tickCount`를 `AtomicInteger`(또는 `AtomicLong`)으로 변경.

---

### 2-7. `LogNode` / `PrintNode` / `AlertNode` / `CounterNode` — logging으로 통일됨

**파일**: 다수

이 항목은 node/core 레벨에서는 정리됐다.

- `LogNode`, `PrintNode`, `AlertNode`, `CounterNode`, `DefaultOutputPort`, `MqttPublisherNode`, `ModbusWriterNode`를 log 기반으로 수정
- 현재 `System.out.println()`은 주로 runner/demo/network 예제에만 남아 있음

---

### 2-8. `SensorNode` — 빈 클래스

**파일**: `node/SensorNode.java`

```java
public class SensorNode {
}
```

내용이 완전히 비어 있는 클래스입니다. `TemperatureSensorNode`, `HumiditySensorNode`의 공통 부모로 사용하거나 제거해야 합니다.

> **제안**: `TemperatureSensorNode`와 `HumiditySensorNode`의 공통 로직(랜덤값 생성, sensorId/timestamp 추가)을 `SensorNode`로 추출하여 추상 부모 클래스로 활용.

---

### 2-9. `EchoProtocolNode` — `onProcess()` 미구현

**파일**: `node/EchoProtocolNode.java`

```java
@Override
protected void onProcess(Message message) {
    // 비어 있음
}
```

소켓에 연결은 되지만 데이터를 보내거나 받는 로직이 없습니다. 활용 불가 상태입니다.

---

### 2-10. `ProtocolCallback` / `MessageListener` — 미사용 코드

**파일**: `protocol/MessageListener.java`, `protocol/ProtocolCallback.java`

`MessageListener` 인터페이스와 `ProtocolCallback` 구현체가 정의되어 있지만, 실제로 `MqttSubscriberNode`나 어느 코드에서도 사용하지 않습니다.  
`MqttSubscriberNode`는 Paho 라이브러리의 `MqttCallback`을 직접 익명 클래스로 구현합니다.

> **제안**: `MessageListener` / `ProtocolCallback`을 제거하거나, `MqttSubscriberNode`에서 이를 활용하도록 리팩토링.

---

### 2-11. `App.java` — "Hello World!" 진입점이 남아 있음

**파일**: `App.java`

```java
public static void main(String[] args) {
    System.out.println("Hello World!");
}
```

Maven archetype 기본 코드가 그대로 남아 있습니다. 실제 진입점 역할을 하지 않습니다.

> **제안**: 제거하거나, 실제 엔진을 시작하는 코드로 대체.

---

### 2-12. `pom.xml` — `maven.compiler.source`/`target` + `release` 중복 설정

**파일**: `pom.xml`

```xml
<maven.compiler.release>21</maven.compiler.release>
<maven.compiler.source>21</maven.compiler.source>
<maven.compiler.target>21</maven.compiler.target>
```

`maven.compiler.release`를 사용하면 `source`와 `target`을 동시에 설정하므로 세 줄 모두 지정할 필요가 없습니다.

> **제안**: `release`만 남기고 `source`/`target` 제거.
>
> ```xml
> <maven.compiler.release>21</maven.compiler.release>
> ```

---

### 2-13. `pom.xml` — `<url>` / FIXME 주석 미정리

**파일**: `pom.xml`

```xml
<!-- FIXME change it to the project's website -->
<url>http://www.example.com</url>
```

Maven archetype 기본 주석과 예시 URL이 그대로 남아 있습니다.

---

## 3. 설계 불일치 / 혼재 패턴

### 3-1. `GeneratorNode`가 `AbstractNode`를 상속하지 않음

**파일**: `node/GeneratorNode.java`

모든 노드는 `AbstractNode`를 상속하는 규칙인데, `GeneratorNode`만 `Node` 인터페이스를 직접 구현합니다.  
그러다보니 `id` 필드, `initialize()`, `shutdown()`을 직접 구현해 코드가 중복됩니다.

> **제안**: `GeneratorNode`도 `AbstractNode`를 상속.

---

### 3-2. `HumiditySensorNode` — 완전한 패키지명 직접 사용

**파일**: `node/HumiditySensorNode.java`

```java
protected void onProcess(com.fbp.engine.message.Message message) {
    com.fbp.engine.message.Message sensorMessage = new com.fbp.engine.message.Message(java.util.Map.of(
```

같은 파일에서 FQN(Fully Qualified Name)을 직접 쓰고 있어 가독성이 나쁩니다.  
(다른 센서 노드 `TemperatureSensorNode`는 정상적으로 `import`를 사용)

> **제안**: `import com.fbp.engine.message.Message;`와 `import java.util.Map;` 추가.

---

### 3-3. `InputPort` / `OutputPort` 인터페이스 — `public` 중복 수식어

**파일**: `core/interfaces/InputPort.java`, `core/interfaces/OutputPort.java`

```java
public interface InputPort {
    public String getName();   // interface 메서드는 기본이 public
    public void receive(Message message);
}
```

인터페이스 메서드에 `public`을 명시적으로 붙이는 것은 Java에서 불필요한 중복입니다.

---

### 3-4. `FlowRunner` — connection ID 조회 방식으로 정리됨

**파일**: `demo/runner/FlowRunner.java`, `demo/runner/FinalFlowEngineRunner.java`

이 항목은 정리됐다.

- `Flow.getConnection(String connectionId)` 추가
- 기존 인덱스 접근 runner를 ID 기반 조회로 수정

---

## 4. 개선 제안 요약

| 구분 | 파일 | 문제 | 심각도 | 상태 |
|------|------|------|--------|------|
| 과도 | `Flow.java` | `connections` + `connectionInfos` 이중 관리 | ⭐⭐ | 해결 |
| 과도 | 다수 Runner | 같은 스레드 패턴 21번 반복, `RunnerSupport` 미활용 | ⭐⭐⭐ | 일부 남음 |
| 과도 | `ModbusTcpClient.java` | 타임아웃 파라미터를 받고 무시하는 레거시 생성자 | ⭐⭐ | 해결 |
| 과도 | `ProtocolNode.java` | 재연결 스케줄러 누수 가능 | ⭐⭐ | 해결 |
| 과도 | `FilterNode.java` | `matches()`와 `onProcess()` 로직 중복 | ⭐ | 해결 |
| 과도 | `SplitNode.java` | 부모 필드 `id` 재선언(shadowing) | ⭐⭐ | 해결 |
| 과도 | `Demo/` 패키지 | 학습 코드가 production 경로, 패키지명 대문자 | ⭐ | 해결 |
| 부족 | `FlowEngine.java` | 스레드 생명주기 관리 없음 (이름뿐인 엔진) | ⭐⭐⭐ | 해결 |
| 부족 | `MergeNode.java` | `_inputPort` 키를 주입하는 코드가 없음 (동작 불가) | ⭐⭐⭐ | 해결 |
| 부족 | `CounterNode.java` | `count` 비원자적 연산 (스레드 안전 X) | ⭐⭐ | 해결 |
| 부족 | `TimerNode.java` | `tickCount` 비원자적 연산 (스레드 안전 X) | ⭐⭐ | 해결 |
| 부족 | `SensorNode.java` | 빈 클래스, 활용 안 됨 | ⭐ | 해결 |
| 부족 | `EchoProtocolNode.java` | `onProcess()` 미구현 | ⭐⭐ | 해결 |
| 부족 | `MessageListener.java` | 정의만 되고 아무 곳에서도 사용 안 됨 | ⭐ | 해결 |
| 부족 | `App.java` | Hello World 코드 그대로 | ⭐ | 해결 |
| 불일치 | `GeneratorNode.java` | 혼자만 `AbstractNode` 미상속 | ⭐⭐ | 해결 |
| 불일치 | `HumiditySensorNode.java` | import 없이 FQN 직접 사용 | ⭐ | 해결 |
| 불일치 | `FlowRunner` 등 | 커넥션을 인덱스로 접근, 순서 변경 시 버그 위험 | ⭐⭐ | 해결 |
| 불일치 | `pom.xml` | `release`와 `source`/`target` 중복, FIXME 미정리 | ⭐ | 해결 |

> ⭐⭐⭐ = 기능에 직접 영향 / ⭐⭐ = 잠재적 버그·유지보수 문제 / ⭐ = 코드 품질 문제
