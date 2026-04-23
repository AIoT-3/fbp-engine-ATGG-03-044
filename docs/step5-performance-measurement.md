# Step 5 성능 측정 정리

## 목적

이 문서는 과제 5-3의 측정 항목과 실행 방법을 정리한 문서다.

## 현재 제공 코드

- runner: `src/main/java/com/fbp/engine/demo/runner/Step5PerformanceRunner.java`

이 runner는 기본적으로 `RuleNode` 단일 파이프라인의 처리량과 평균 처리 시간을 측정한다.

## 기본 측정 항목

- throughput
  - 초당 처리 메시지 수
- latency
  - 메시지 한 건 처리 평균 시간

## 실행 대상 메시지 수

- 100
- 500
- 1000

## 실행 방법

```bash
mvn -q -DskipTests compile
java -cp target/classes com.fbp.engine.demo.runner.Step5PerformanceRunner
```

## 출력 예시

```text
messages=100
match=69, mismatch=31
throughput=...
avgLatency=...
```

## 기록 템플릿

| 메시지 수 | throughput(msg/s) | avg latency(us) | 비고 |
| --- | --- | --- | --- |
| 100 |  |  |  |
| 500 |  |  |  |
| 1000 |  |  |  |

## 해석 시 주의

- 현재 runner는 **기본 Rule 파이프라인 성능**을 보는 용도다.
- MQTT Broker, MODBUS 시뮬레이터, 네트워크 지연을 포함한 완전한 end-to-end 성능과는 다르다.
- 외부 시스템을 포함한 성능 측정이 필요하면 Step 5 시나리오 runner를 기준으로 별도 측정해야 한다.
