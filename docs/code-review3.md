# FBP Engine 코드 리뷰 3차

> 2차 리뷰 이후 변경된 코드를 기준으로 정리합니다.
> 교육 과제 특성상 의도적으로 유지하는 코드가 있으므로 그 부분은 별도로 구분합니다.
> 코드는 **일절 수정하지 않고** 문서로만 기록합니다.

---

## ✅ 2차 리뷰 이후 잘 개선된 사항

| 항목 | 내용 |
|------|------|
| `Connection` | 주석 처리된 코드 제거, `null` id 문제를 `AtomicLong SEQUENCE`로 자동 생성 해결 |
| `FlowEngine.getFlows()` | `Collections.unmodifiableMap()` 반환으로 캡슐화 |
| `FlowEngine.getFlowStates()` | 마찬가지로 `unmodifiableMap()` 반환 |
| `FlowEngine.stopWorkers()` | `interrupted` flag 방식으로 모든 워커를 join한 뒤 재interrupt |
| `FlowEngine` 워커 | `RuntimeException` catch 추가 → 워커 스레드가 예외로 종료되지 않음 |
| `FlowRuntime` | `stop()`, `workers()` 메서드 추가로 캡슐화 |
| `MqttPublisherNode` | `errorCount` → `AtomicInteger`, re-throw 제거 |
| `ModbusTcpClient` | `transactionId` → `AtomicInteger` |
| `ThresholdFilterNode` | `filedName` 오타 → `fieldName` 수정 |
| `LogNode` | `DateTimeFormatter` / `LocalTime.now()` 이중 타임스탬프 제거 |
| `CompositeRuleNode` | `isEmpty()` 체크를 switch 앞으로 이동, 로직 명확화 |
| `Message` | `@Data` → `@Getter` + `@EqualsAndHashCode` + `@ToString`으로 교체 |
| `ProtocolNode` | `reconnectIntervalMs`를 config에서 읽도록 변경 |

---

## 목차

1. [교육 목적으로 의도적으로 유지하는 항목](#1-교육-목적으로-의도적으로-유지하는-항목)
2. [아직 남아 있는 개선 가능 항목](#2-아직-남아-있는-개선-가능-항목)
3. [새로 발견된 미세 사항](#3-새로-발견된-미세-사항)
4. [전체 현황 요약](#4-전체-현황-요약)

---

## 1. 교육 목적으로 의도적으로 유지하는 항목

아래 항목들은 강의 과제 구조상 일부러 남겨두는 것으로 보이므로 **반드시 바꿔야 하는 것은 아닙니다**. 다만 코드를 처음 보는 사람이 혼동할 수 있으므로 한 줄 주석을 달아두는 것을 권장합니다.

---

### A. `ThreeNodeRunner` — `System.out.println()` 유지

**파일**: `demo/runner/ThreeNodeRunner.java`

```java
System.out.println("[생산자] 메시지 전송: " + message.getPayload());
```

이것은 과제 4-5의 "생산자 역할 스레드 동작 확인"을 위한 출력으로, 교육 과정 상 명시적으로 콘솔을 보여주기 위한 것으로 보입니다. 교육 목적이면 그대로 두어도 됩니다.

> **권장 (선택 사항)**: 주석으로 의도 명시
> ```java
> // 과제: 생산자 동작을 직접 콘솔에서 확인하기 위해 유지
> System.out.println("[생산자] 메시지 전송: " + message.getPayload());
> ```

---

### B. `FlowRunner` — `RunnerSupport` 대신 직접 Thread 관리

**파일**: `demo/runner/FlowRunner.java`

과제 7-2의 요구사항이 "Flow를 사용하되 스레드를 직접 배선하는 방식"이라면, `RunnerSupport`를 쓰지 않고 직접 Thread를 생성하는 것이 맞습니다. 현재 주석(`// 과제 7-2`)이 있어 의도가 명확합니다.

> **현 상태 유지 가능**: 의도가 주석으로 표시되어 있으므로 문제 없음.

---

### C. `MqttPathDemo` — `src/main` 경로에 존재

**파일**: `protocol/MqttPathDemo.java`

강의에서 MQTT 연결 흐름을 직접 보여주기 위한 데모 코드로, 과제 경로 제약상 `src/main`에 두는 경우 어쩔 수 없습니다.

> **현 상태 유지 가능**: 과제 요구 사항이라면 허용됨. 다만 클래스 상단에 `// 교육용 데모 코드` 주석 한 줄 추가 권장.

---

### D. `AbstractNode.getOutputPort()` / `getInputPort()` — `public` 유지

**파일**: `core/AbstractNode.java`

`MainRunner`, `ThreeNodeRunner` 등에서 포트에 직접 접근하는 교육 예제가 있기 때문에 `public`을 유지해야 합니다. `FlowEngine`이 스레드를 자동 관리하게 된 현 시점에서는 직접 접근이 과거 방식임을 주석으로 알리면 충분합니다.

> **현 상태 유지 가능**: 교육 목적 접근 방식 허용.

---

### E. `Flow.ConnectionRoute` — `static record` 접근 제어 미명시

**파일**: `core/Flow.java`

```java
static record ConnectionRoute(Connection connection, AbstractNode targetNode, String targetPort) {
}
```

`package-private` 접근 제어입니다. `FlowEngine`과 `Flow`가 같은 패키지라 실제로는 문제없이 동작합니다. 접근 제어를 명시적으로 `private static`으로 바꾸면 `FlowEngine`에서 사용할 수 없게 되므로, 현재 구조에서는 `package-private`이 오히려 올바른 선택입니다.

> **현 상태 유지 가능**: 이미 올바른 접근 제어.

---

## 2. 아직 남아 있는 개선 가능 항목

아래는 기능에 직접 영향을 주거나, 개선하면 코드 품질이 눈에 띄게 좋아지는 항목들입니다.

---

### 2-1. `Connection` — `LinkedBlockingQueue` 생성자 중복

**파일**: `core/Connection.java`

현재 5가지 생성자가 있습니다:

```java
public Connection()                                 // id 자동, capacity=100
public Connection(int capacity)                     // id 자동, capacity 지정
public Connection(String id)                        // id 지정, capacity=100
public Connection(String id, int capacity)          // id 지정, capacity 지정 ← 실제 생성
public Connection(String id, LinkedBlockingQueue<Message> buffer) // 테스트용
```

`Connection(String id, LinkedBlockingQueue<Message> buffer)` 생성자는 오직 테스트 코드에서만 사용합니다. production 클래스에 테스트 전용 생성자가 있으면 API 의도가 모호해집니다.

> **제안**: 이 생성자를 `@VisibleForTesting` 어노테이션(Guava) 또는 주석으로 표시하거나, 테스트용 팩토리 메서드로 분리.
> ```java
> // 테스트 전용 — 실제 운영 코드에서는 호출하지 말 것
> public Connection(String id, LinkedBlockingQueue<Message> buffer) { ... }
> ```

---

### 2-2. `FlowEngine` — `log.info()` 문자열 연결(concatenation) 방식 혼재

**파일**: `core/FlowEngine.java`

```java
log.info("[Engine] 플로우 '" + flow.getId() + "' 등록됨");  // 문자열 연결 방식
log.info("[Engine] 플로우 '{}' 시작됨", flowId);            // SLF4J 파라미터 방식
```

SLF4J는 `log.info("... {}", value)` 형태의 파라미터 방식을 사용해야 로그 레벨이 비활성화됐을 때 불필요한 문자열 생성을 피할 수 있습니다. 일부 메서드는 문자열 연결(`+`)을 그대로 사용하고 있습니다.

> **제안**: 모든 `log.*` 호출을 파라미터 방식으로 통일.
> ```java
> log.info("[Engine] 플로우 '{}' 등록됨", flow.getId());
> log.info("[Engine] 플로우 '{}' - {}", flowId, flowStates.get(flowId));
> ```

---

### 2-3. `ProtocolNode` — `reconnect()` 스케줄러가 `ScheduledFuture`를 관리하지 않음

**파일**: `core/ProtocolNode.java`

```java
reconnectScheduler.scheduleAtFixedRate(() -> {
    if (retryCount[0] >= maxRetries || ...) {
        reconnectScheduler.shutdown();  // 스케줄러 내부에서 자신을 종료
        return;
    }
    ...
    stopReconnectScheduler();  // 또 다른 종료 경로
}, ...);
```

현재 종료 경로가 두 곳입니다:
1. `reconnectScheduler.shutdown()` (태스크 내부)
2. `stopReconnectScheduler()` (태스크 내부)

`ScheduledExecutorService.shutdown()`을 호출해도 이미 실행 중인 태스크는 완료됩니다. 두 경로가 혼재하면 `reconnectScheduler`가 null이 됐는데 태스크는 아직 돌고 있는 상황이 생길 수 있습니다. `scheduleAtFixedRate()`의 반환값인 `ScheduledFuture`를 저장하고 `future.cancel(false)`로 취소하는 것이 더 명확합니다.

> **제안**:
> ```java
> private ScheduledFuture<?> reconnectFuture;
>
> protected void reconnect() {
>     stopReconnectScheduler();
>     reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
>     reconnectFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
>         if (retryCount[0] >= maxRetries || connectionState == ConnectionState.CONNECTED) {
>             reconnectFuture.cancel(false);
>             return;
>         }
>         ...
>     }, reconnectIntervalMs, reconnectIntervalMs, TimeUnit.MILLISECONDS);
> }
> ```

---

### 2-4. `MergeNode` — `pending1` / `pending2` 필드에 대한 동시성 주석 부재

**파일**: `node/MergeNode.java`

`onProcess()`는 `synchronized`로 보호되고, `MergeInputPort.receive()`가 `owner.process()`를 직접 호출하므로 실제로는 안전합니다. 그러나 코드를 처음 보는 사람은 `pending1 = cleanMessage;`가 왜 안전한지 즉각 파악하기 어렵습니다.

> **제안**: 상단에 한 줄 주석 추가
> ```java
> // receive()는 항상 process()를 통해 호출되며 onProcess()는 synchronized이므로 thread-safe
> @Override
> protected synchronized void onProcess(Message message) {
> ```

---

### 2-5. `MqttPublisherNode` / `MqttSubscriberNode` — `DisconnectedBufferOptions` 여전히 불필요

**파일**: `node/MqttPublisherNode.java`, `node/MqttSubscriberNode.java`, `protocol/MqttPathDemo.java`

2차 리뷰에서 지적했으나 아직 남아 있습니다:

```java
DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
bufferOptions.setBufferEnabled(false);  // false가 기본값이므로 생성 자체가 불필요
client.setBufferOpts(bufferOptions);
```

`DisconnectedBufferOptions`의 기본 `bufferEnabled`는 `false`입니다. `false`로 명시하려고 객체를 생성하면 오히려 "이게 true인 상태가 있나?"라는 의구심을 불러일으킵니다.

> **제안**: 세 줄 모두 제거. 불필요한 객체 생성 없앰.  
> 만약 미래에 버퍼를 켤 가능성을 염두에 두고 남긴 것이라면 주석으로 이유 명시.

---

### 2-6. `MqttSubscriberNode` — `onProcess()` 미구현 상태

**파일**: `node/MqttSubscriberNode.java`

`MqttSubscriberNode`는 `ProtocolNode`를 상속하는데, `ProtocolNode.onProcess()`는 비어 있습니다. `MqttSubscriberNode` 자체도 `onProcess()`를 오버라이드하지 않습니다. 즉 누군가 `FlowEngine`이 이 노드 앞 Connection에서 메시지를 꺼내 `targetNode.getInputPort(...).receive(message)` 해도 아무 일도 일어나지 않습니다.

`MqttSubscriberNode`는 MQTT 콜백(푸시 방식)으로 메시지를 받아 직접 `send()`하는 구조이므로 입력 포트(`InputPort`)가 애초에 없어야 자연스럽습니다.

> **제안**: `addInputPort()`를 제거하거나, Javadoc으로 "이 노드는 외부 MQTT 콜백으로만 메시지를 수신하므로 onProcess()를 사용하지 않는다"고 명시.

---

### 2-7. `FlowEngine` — `listFlows()`가 CLI 출력 용도임에도 `log.info()` 사용

**파일**: `core/FlowEngine.java`, `demo/runner/FlowEngineCliRunner.java`

2차 리뷰에서도 지적한 내용입니다. `FlowEngineCliRunner`에서 사용자가 `list`를 입력하면 응답을 기대하지만, `log.info()`는 로그 설정에 따라 콘솔에 나타나지 않을 수 있습니다(특히 logback.xml에서 레벨을 높이면). 교육 목적으로 유지한다면 괜찮지만, 만약 CLI가 실제로 동작해야 하는 기능이라면 문제가 됩니다.

> **제안**: `listFlows()` 시그니처를 `List<String> listFlows()`로 변경하고, 호출부(`FlowEngineCliRunner`)에서 `System.out.println()`으로 출력하여 엔진 로직과 UI를 분리.

---

## 3. 새로 발견된 미세 사항

---

### 3-1. `Connection` — `id`가 `final`로 바뀌었지만 `@Getter`가 있어 setter는 없음 ← 정상

이전엔 `private String id` (non-final)이었는데 `private final String id`로 바뀌었습니다. `@Getter`만 있어 외부에서 변경 불가. 올바른 방향입니다. (기록용)

---

### 3-2. `FlowEngine` — `FlowRuntime.workers()` 메서드가 리스트를 직접 반환

**파일**: `core/FlowEngine.java`

```java
private List<Thread> workers() {
    return workers;  // 내부 리스트 직접 반환
}
```

`FlowRuntime`은 `private static class`이고 `workers()`도 `private`입니다. `FlowEngine` 내부에서만 사용하므로 실제 문제는 없습니다. 다만 일관성을 위해 `Collections.unmodifiableList(workers)`를 반환하거나, 현재처럼 직접 반환해도 됩니다. (미세 사항)

---

### 3-3. `Message` — `@EqualsAndHashCode`가 `id`(UUID)를 포함하여 동등성 비교

**파일**: `message/Message.java`

`@EqualsAndHashCode`를 붙이면 `id`, `payload`, `timestamp` 세 필드를 모두 비교합니다.  
`id`는 `UUID.randomUUID()`로 생성되므로 동일한 payload를 가진 두 `Message`는 절대 `equals()`가 true가 되지 않습니다. 테스트에서 `assertEquals(expected, actual)`로 메시지를 비교하면 항상 실패합니다.

현재 테스트 코드에서 `Message` 동등성을 어떻게 검증하는지 확인이 필요합니다. 만약 payload만 비교해야 한다면 `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@EqualsAndHashCode.Include`를 payload에 추가하는 것이 의미에 맞습니다.

> **제안**: 테스트에서 `message.getPayload().equals(expected.getPayload())`로 payload만 비교하거나, equals 대상 필드를 payload로 한정.

---

### 3-4. `Flow` — `getConnections()`가 여전히 남아 있지만 내부에서 사용되지 않음

**파일**: `core/Flow.java`

```java
public List<Connection> getConnections() { ... }
```

`getConnectionRoutes()`가 내부적으로 사용되고, `getConnection(id)`가 외부 조회에 쓰입니다. `getConnections()`는 인덱스 기반 접근을 위해 남겨진 것으로 보이지만, 현재 코드에서 이를 사용하는 곳이 없습니다(FlowRunner도 `getConnection(id)`로 교체됨).

> **제안**: 실제 사용처가 없다면 제거. 혹은 `@Deprecated` 표시로 "getConnection(id)를 사용하라"고 유도.

---

## 4. 전체 현황 요약

### 현재 코드 상태 평가

| 영역 | 1차 | 2차 | 3차 |
|------|-----|-----|-----|
| 스레드 안전 | ⚠️ 다수 미흡 | ✅ 핵심 수정 | ✅ 거의 완료 |
| FlowEngine 구조 | ❌ 스레드 미관리 | ✅ 관리 추가 | ✅ 견고해짐 |
| 로그 일관성 | ⚠️ 혼재 | ✅ 통일 | ⚠️ 일부 + 연결 잔존 |
| 노드 완성도 | ⚠️ 미구현 다수 | ✅ 대부분 완성 | ⚠️ MqttSubscriber 구조 모호 |
| 코드 중복 | ⚠️ Runner 중복 | ✅ 분리 완료 | ✅ 양호 |
| API 명확성 | ⚠️ | ✅ 개선 | ⚠️ 일부 미흡 |

### 남은 이슈 우선순위

| 우선순위 | 파일 | 문제 |
|---------|------|------|
| 🟡 권장 | `FlowEngine.java` | log 문자열 연결 → 파라미터 방식 통일 |
| 🟡 권장 | `ProtocolNode.java` | `ScheduledFuture` 로 재연결 취소 명확화 |
| 🟡 권장 | `MqttSubscriberNode.java` | `onProcess()` 없음 명시 또는 InputPort 제거 |
| 🟢 선택 | `Connection.java` | 테스트 전용 생성자 표시 |
| 🟢 선택 | `MqttPublisherNode/SubscriberNode` | 불필요한 `DisconnectedBufferOptions` 제거 |
| 🟢 선택 | `Flow.java` | `getConnections()` 미사용 → 제거 또는 Deprecated |
| 🟢 선택 | `Message.java` | `@EqualsAndHashCode` 범위 검토 |
| 🟢 선택 | `MergeNode.java` | 동시성 의도 주석 추가 |

> 🟡 = 개선 권장 / 🟢 = 선택 사항

---

**전체적으로 1, 2차 리뷰를 통해 코드 품질이 크게 향상됐습니다.**  
핵심 기능(FlowEngine 스레드 관리, MergeNode, 스레드 안전, 로깅)은 모두 올바르게 동작하는 수준입니다.  
남은 항목들은 대부분 "더 좋은 코드"를 위한 선택적 개선이며, 교육 과제 맥락에서는 현재 상태로도 충분합니다.
