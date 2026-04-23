# FBP Engine 코드 리뷰 2차

> 1차 리뷰 이후 변경된 코드를 기준으로 새롭게 발견된 문제점을 정리합니다.
> 코드는 **일절 수정하지 않고** 문서로만 기록합니다.

---

## ✅ 1차 리뷰 이후 개선된 사항 (잘 고쳤습니다)

| 항목 | 내용 |
|------|------|
| `FlowEngine` 스레드 관리 | `FlowRuntime`으로 워커 스레드를 엔진이 직접 관리 |
| `MergeNode` | `MergeInputPort` 내부 클래스로 `_inputPort` 자동 주입 |
| `CounterNode` | `AtomicInteger`로 교체 |
| `TimerNode` | `AtomicInteger`로 교체 |
| `SplitNode` | 부모 필드 `id` 중복 선언 제거 |
| `FilterNode` | `onProcess()`에서 `matches()` 재사용 |
| `Flow` | `connections`/`connectionInfos` 이중 관리 → `connectionInfos`로 통합, `getConnection(id)` 추가 |
| `GeneratorNode` | `AbstractNode` 상속으로 변경 |
| `SensorNode` | 추상 부모 클래스로 승격, `TemperatureSensorNode`/`HumiditySensorNode` 위임 |
| `HumiditySensorNode` | FQN 직접 사용 → 정상 import 처리 |
| Demo 패키지 | `src/main` → `src/test`로 이동, 패키지명 소문자 `demo` |
| Runner 클래스 | `demo.runner` 패키지로 분리 |
| 노드 로그 | `System.out.println` → `@Slf4j` / `log.*` 통일 |
| `App.java` | Hello World 제거 |
| `ProtocolNode.reconnect()` | `stopReconnectScheduler()` 분리로 누수 방지 |
| `pom.xml` | `source`/`target` 중복 제거, FIXME 제거 |
| `InputPort`/`OutputPort` 인터페이스 | 불필요한 `public` 수식어 제거 |
| `DefaultOutputPort` | `InterruptedException` 로깅 추가 |

---

## 목차

1. [아직 남은 문제](#1-아직-남은-문제)
2. [새로 발견된 문제](#2-새로-발견된-문제)
3. [개선 제안 요약](#3-개선-제안-요약)

---

## 1. 아직 남은 문제

### 1-1. `Connection` — 주석 처리된 코드가 그대로 남아 있음

**파일**: `core/Connection.java`

```java
//private InputPort target;
...
//    public void setTarget(InputPort target){
//        this.target = Objects.requireNonNull(target);
//    }
```

설계 초기에 사용하다 방향이 바뀐 흔적입니다. 주석 처리된 채로 남아 있어 코드 의도를 오해하게 만들 수 있습니다.

> **제안**: 주석 처리된 코드 제거.

---

### 1-2. `Connection` — `id`가 `null`일 수 있는 생성자

**파일**: `core/Connection.java`

```java
public Connection() {       // id = null
    this(100);
}
public Connection(int capacity) {  // id = null
    this.buffer = new LinkedBlockingQueue<>(capacity);
}
```

`Connection()`, `Connection(int capacity)` 생성자로 만든 객체는 `id`가 `null`입니다.  
`FlowEngine.startWorkers()`에서 워커 스레드 이름을 `flowId + "-" + route.connection().getId()`로 만들므로, `id`가 `null`이면 스레드 이름이 `"flowId-null"`이 됩니다.  
`Flow.getConnection(connectionId)` 역시 id 기반 조회이므로 `null` id를 가진 Connection은 조회 불가입니다.

> **제안**: 기본 생성자와 용량 생성자가 실제로 필요한지 검토. `Flow.connect()`에서는 항상 id가 있는 생성자를 사용하므로, 프레임워크 사용자가 직접 `new Connection()`을 호출하는 경우에만 문제가 됩니다. `@Deprecated` 표시 또는 제거 고려.

---

### 1-3. `AbstractNode` — `getOutputPort()`/`getInputPort()`가 여전히 `public`

**파일**: `core/AbstractNode.java`

1차 리뷰에서 지적했지만 변경되지 않았습니다.

```java
public OutputPort getOutputPort(String name) { ... }
public InputPort getInputPort(String name) { ... }
```

외부에서 노드의 포트에 직접 접근할 수 있어 `Flow.connect()` 우회가 가능합니다.  
`MainRunner`에서 여전히 다음처럼 직접 접근합니다:

```java
timerNode.getOutputPort("out").connect(connection1);
```

이는 `Flow` 없이 수동으로 배선하는 데모(`MainRunner`, `ThreeNodeRunner`)에서 의도적으로 필요한 패턴이기도 합니다.

> **의견**: 만약 이 접근을 허용하려면 현 상태가 맞습니다. 그러나 엔진 레벨에서 포트 접근을 통제하고 싶다면 `package-private`으로 변경하고 `Flow`/`FlowEngine`만 같은 패키지에 두는 구조로 전환이 필요합니다.

---

### 1-4. `FlowEngine` — `getFlows()`가 내부 맵을 그대로 반환

**파일**: `core/FlowEngine.java`

```java
public Map<String, Flow> getFlows() {
    return flows;  // 내부 맵 직접 반환
}
```

`getNodes()`엔 `Collections.unmodifiableMap()`이 적용됐지만(`Flow.java`), `FlowEngine.getFlows()`는 그렇지 않습니다. 외부에서 `engine.getFlows().put(...)`, `engine.getFlows().remove(...)` 등을 호출하면 엔진 내부 상태가 망가집니다.

> **제안**: `Collections.unmodifiableMap(flows)`를 반환하거나 복사본을 반환.

---

### 1-5. `FlowEngine` — `stopWorkers()`에서 `InterruptedException` 발생 시 나머지 스레드를 join하지 않음

**파일**: `core/FlowEngine.java`

```java
for (Thread worker : runtime.workers) {
    try {
        worker.join();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;  // ← 여기서 바로 리턴하면 나머지 워커가 정리되지 않음
    }
}
```

여러 워커 스레드 중 첫 번째 `join()`에서 `InterruptedException`이 발생하면 나머지 워커들이 계속 실행됩니다.

> **제안**: `return` 대신 남은 스레드도 `interrupt()`하고 join 재시도하는 방식으로 변경.

---

### 1-6. `MergeNode` — `pending1`/`pending2`가 `volatile` 아님

**파일**: `node/MergeNode.java`

```java
private Message pending1;
private Message pending2;
```

`onProcess()`는 `synchronized`이지만, `pending1`/`pending2` 자체를 synchronized 없이 읽을 가능성이 있는 경우(예: 외부 테스트 코드)를 위해 `volatile`을 붙이거나 `synchronized`를 일관되게 유지하는 것이 명확합니다.  
현재 `onProcess()`만 `synchronized`라면 내부 접근은 안전하지만, `getInputPort()` 오버라이드로 `MergeInputPort.receive()`가 `owner.process()`를 직접 호출하는 동선이므로 실제로는 문제가 없습니다. 다만 코드만 보면 의도가 불명확합니다.

> **제안**: 주석이나 문서로 "receive를 통해 process가 호출되므로 synchronized onProcess로 보호됨"을 명시.

---

### 1-7. `MqttPublisherNode` / `MqttSubscriberNode` — `errorCount`가 스레드 안전하지 않음

**파일**: `node/MqttPublisherNode.java`

```java
private int errorCount;
...
errorCount++;  // 원자적이지 않음
```

`onProcess()`는 여러 스레드에서 호출될 수 있는데, `errorCount`는 일반 `int`입니다.

> **제안**: `AtomicInteger errorCount = new AtomicInteger()`로 변경.

---

### 1-8. `MqttPublisherNode` — `onProcess()`가 `RuntimeException`을 re-throw

**파일**: `node/MqttPublisherNode.java`

```java
} catch (Exception e) {
    errorCount++;
    log.warn("Publish failed: {}", e.getMessage(), e);
    throw new RuntimeException(e);  // FlowEngine 워커 스레드를 죽일 수 있음
}
```

`FlowEngine`의 워커 스레드는 `RuntimeException`을 catch하지 않습니다. 즉, 발행(publish)이 한 번 실패하면 해당 Connection의 워커 스레드가 예외와 함께 종료되어 이후 메시지가 모두 큐에 쌓이기만 하고 처리되지 않습니다.

> **제안**: 워커에서 예외를 catch하거나(`FlowEngine.startWorkers()`), `MqttPublisherNode.onProcess()`에서 re-throw를 제거하고 에러 포트(`"error"`)로 메시지를 라우팅.

---

### 1-9. `LogNode` — `LocalTime` + 별도 포매터와 SLF4J 로그 혼재

**파일**: `node/LogNode.java`

```java
private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

@Override
protected void onProcess(Message message) {
    String now = LocalTime.now().format(FORMATTER);
    log.info("[{}][{}] {}", now, getId(), message.getPayload());
}
```

SLF4J의 `log.info()`는 이미 내부적으로 타임스탬프를 포함합니다. 여기서 별도로 `LocalTime.now()`를 찍으면 로그 출력에 시각이 두 번 나타납니다.

> **제안**: `FORMATTER`와 `now` 변수를 제거하고 `log.info("[{}] {}", getId(), message.getPayload())`로 단순화.

---

### 1-10. `ThresholdFilterNode` — 오타: `filedName` (fieldName이어야 함)

**파일**: `node/ThresholdFilterNode.java`

```java
private final String filedName;  // "filed" → "field" 오타
```

필드명, 파라미터, `this.filedName` 등 세 곳에 오타가 있습니다. 컴파일은 되지만 코드 가독성을 해칩니다.

> **제안**: `filedName` → `fieldName`으로 교정.

---

### 1-11. `CompositeRuleNode` — `conditions.isEmpty()` 체크 순서가 잘못됨

**파일**: `node/CompositeRuleNode.java`

```java
boolean matched = switch (operator) {
    case AND -> conditions.stream().allMatch(...);  // 빈 스트림이면 true (Stream.allMatch 의미상)
    case OR  -> conditions.stream().anyMatch(...);  // 빈 스트림이면 false
};

if (conditions.isEmpty()) {
    matched = operator == Operator.AND;  // 이 코드는 switch 이후에 실행됨 (의미 없음)
}
```

`Stream.allMatch()`는 빈 스트림에 대해 `true`를 반환하므로, `AND`인 경우 `conditions`가 비어 있어도 `matched = true`가 정상적으로 나옵니다.  
`OR`인 경우 빈 스트림 `anyMatch()`는 `false`를 반환하고, 이후 `if (conditions.isEmpty()) matched = false`가 됩니다.  
즉 올바른 값이 나오지만, **switch 이후에 isEmpty 체크를 재수행하는 코드는 의미가 없으며** 코드 가독성을 해칩니다.

> **제안**: 순서를 바꾸거나 주석으로 의도를 명시:
> ```java
> if (conditions.isEmpty()) {
>     if (matched) send("match", message);
>     else send("mismatch", message);
>     return; // 또는 isEmpty 체크를 switch 앞으로 이동
> }
> ```

---

## 2. 새로 발견된 문제

### 2-1. `Flow` — `ConnectionRoute` record가 `static`이지만 접근 제어가 `package-private`

**파일**: `core/Flow.java`

```java
static record ConnectionRoute(Connection connection, AbstractNode targetNode, String targetPort) {
}
```

`static record`이면서 접근 제어자가 없어 같은 패키지(`core`)에서만 사용 가능합니다.  
`FlowEngine`과 `Flow`가 같은 패키지이므로 실제 동작에는 문제가 없지만, `record`가 왜 `static`인지, 왜 `package-private`인지 의도가 코드만 봐서는 불명확합니다.

> **제안**: `private static` (외부에 노출할 필요가 없다면), 또는 `public static`으로 명시. 현재처럼 암묵적 접근 제어는 지양.

---

### 2-2. `FlowEngine` — `FlowRuntime` 내부 클래스의 필드가 직접 노출됨

**파일**: `core/FlowEngine.java`

```java
private static class FlowRuntime {
    private final AtomicBoolean running;
    private final List<Thread> workers;
    ...
}
// 사용처:
runtime.running.set(false);
for (Thread worker : runtime.workers) { ... }
```

`FlowRuntime`의 `running`과 `workers`에 접근 제어자가 없어서 (`private` 제어자가 없음) 같은 최상위 클래스(`FlowEngine`) 내에서 직접 필드 접근을 합니다.  
내부 클래스이므로 `private` 필드도 외부 클래스에서 접근 가능하지만, `stop()` 같은 동작을 `FlowRuntime` 자체 메서드로 캡슐화하면 더 명확합니다.

> **제안**: `FlowRuntime`에 `stop()` 메서드 추가:
> ```java
> void stop() {
>     running.set(false);
>     workers.forEach(Thread::interrupt);
> }
> ```

---

### 2-3. `FlowRunner` (demo) — `RunnerSupport`이 있음에도 사용하지 않음

**파일**: `demo/runner/FlowRunner.java`

`RunnerSupport.startWorker()`를 사용하면 각 노드 처리 스레드를 3줄로 줄일 수 있지만, `FlowRunner`는 여전히 수동으로 Thread를 생성합니다.  
`RunnerSupport`를 사용하지 않는다면 `RunnerSupport`가 데모 패키지에 존재해야 할 이유가 없습니다.  
또한 `FlowRunner`는 이미 `FlowEngine`에서 스레드를 자동으로 관리하게 됐으므로, 이 Runner 자체가 **이전 방식의 학습 예제**로서만 의미가 있습니다.

> **제안**: `FlowRunner`를 `FlowEngineRunner`처럼 `FlowEngine`을 사용하도록 업데이트하거나, "Flow 직접 배선 예제"임을 주석으로 명시.

---

### 2-4. `MqttPathDemo` — `src/main` 경로에 남아 있음

**파일**: `protocol/MqttPathDemo.java`

Demo 패키지 정리 시 `BlockingQueueDemo`, `SynchronizedBufferDemo`, `UnSafeBufferDemo`는 `src/test`로 이동됐지만, `MqttPathDemo`는 그대로 `src/main/java/com/fbp/engine/protocol/`에 남아 있습니다.

> **제안**: `src/test` 또는 `demo` 패키지로 이동.

---

### 2-5. `ModbusTcpClient` — `transactionId` 가 멀티스레드에서 안전하지 않음

**파일**: `protocol/ModbusTcpClient.java`

```java
private int transactionId = 0;

private int nextTransactionId() {
    transactionId++;
    return transactionId;
}
```

`readHoldingRegisters()`와 `writeSingleRegister()` 모두 `nextTransactionId()`를 호출합니다.  
현재 `ModbusReaderNode`/`ModbusWriterNode`에서 각 노드가 별도 클라이언트를 가지므로 단일 스레드에서 호출되는 경우가 대부분이지만, 동일 클라이언트가 여러 스레드에서 사용된다면 `transactionId`가 충돌할 수 있습니다.

> **제안**: `AtomicInteger transactionId = new AtomicInteger(0)`으로 변경.

---

### 2-6. `Message` — `@Data` 어노테이션이 불변 객체에 과도

**파일**: `message/Message.java`

`@Data`는 `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, `@RequiredArgsConstructor`를 모두 생성합니다.  
그런데 `Message`는 `private final` 필드만 있고 setter가 필요 없으며, 생성자도 직접 정의되어 있습니다. 즉 `@Data`가 생성하는 `Setter`와 `@RequiredArgsConstructor`는 실제로 사용되지 않습니다.

> **제안**: `@Data` → `@Getter` + `@EqualsAndHashCode` + `@ToString`으로 교체 (불필요한 setter 생성 방지).

---

### 2-7. `ProtocolNode` — `reconnectIntervalMs`가 변경 불가능하게 설계됐지만 `private`이 아닌 접근 가능

**파일**: `core/ProtocolNode.java`

```java
private long reconnectIntervalMs = 5000;
```

생성자 파라미터로 받지 않으며, 변경할 수단도 없고, 설정 맵(`config`)에서도 읽지 않습니다. 항상 5000ms로 고정입니다.

> **제안**: 생성자나 `config`에서 `reconnectIntervalMs`를 읽도록 개선하거나, 상수(`private static final long RECONNECT_INTERVAL_MS = 5000`)로 명시.

---

### 2-8. `ThreeNodeRunner` — `System.out.println()` 여전히 사용

**파일**: `demo/runner/ThreeNodeRunner.java`

```java
System.out.println("[생산자] 메시지 전송: " + message.getPayload());
```

다른 노드들은 `@Slf4j`로 통일됐지만, `ThreeNodeRunner`는 아직 `System.out`을 사용합니다.

> **제안**: `@Slf4j` + `log.info()` 로 변경.

---

### 2-9. `FlowEngine` — `listFlows()`가 `log.info()`를 사용하지만 CLI에서는 `System.out`이 더 적합

**파일**: `core/FlowEngine.java`, `demo/runner/FlowEngineCliRunner.java`

```java
public void listFlows(){
    for(String flowId : flows.keySet()){
        log.info(flowId + " - " + flowStates.get(flowId));
    }
}
```

`FlowEngineCliRunner`에서는 사용자가 `list` 명령을 입력하면 응답을 기대합니다. 하지만 `log.info()`는 로그 설정에 따라 콘솔에 보이지 않을 수 있습니다.  
또한 로그 포맷(`[main] INFO FlowEngine - monitoring - STOPPED`)은 CLI 사용자에게 불필요한 정보를 포함합니다.

> **참고**: 이것은 단순 데모라면 큰 문제는 아니지만, CLI로 사용하려면 `listFlows()`가 문자열 list를 반환하거나 별도 CLI 어댑터가 `System.out`으로 출력하는 구조가 더 적합합니다.

---

## 3. 개선 제안 요약

### 🔴 기능/정확성 문제

| 파일 | 문제 |
|------|------|
| `FlowEngine.java` | `stopWorkers()`에서 `InterruptedException` 시 나머지 워커 미정리 |
| `MqttPublisherNode.java` | `onProcess()`가 `RuntimeException` re-throw → 워커 스레드 종료 위험 |
| `CompositeRuleNode.java` | `isEmpty()` 체크가 switch 이후에 위치 (코드 의도 불명확) |

### 🟡 잠재적 버그 / 스레드 안전

| 파일 | 문제 |
|------|------|
| `MqttPublisherNode.java` | `errorCount`가 `int` — 스레드 안전하지 않음 |
| `ModbusTcpClient.java` | `transactionId`가 `int` — 멀티스레드 위험 |
| `FlowEngine.java` | `getFlows()`가 내부 맵을 직접 반환 |

### 🟢 코드 품질 / 정리

| 파일 | 문제 |
|------|------|
| `Connection.java` | 주석 처리된 코드 잔존, `null` id 생성자 |
| `Flow.java` | `ConnectionRoute` record 접근 제어 미명시 |
| `FlowEngine.java` | `FlowRuntime` 필드 직접 접근 (캡슐화 부족) |
| `MqttPathDemo.java` | `src/main` 경로에 데모 코드 잔존 |
| `ThresholdFilterNode.java` | `filedName` 오타 |
| `LogNode.java` | 타임스탬프 이중 출력 |
| `Message.java` | `@Data`가 과도 (setter 불필요) |
| `ProtocolNode.java` | `reconnectIntervalMs` 하드코딩 |
| `demo/runner/FlowRunner.java` | `RunnerSupport` 미사용, 용도 불명확 |
| `demo/runner/ThreeNodeRunner.java` | `System.out.println()` 잔존 |
