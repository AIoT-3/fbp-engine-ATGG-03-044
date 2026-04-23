# Step 5 에러 시뮬레이션 가이드

## 목적

이 문서는 과제 5-2를 수행할 때 어떤 에러 상황을 재현하고 무엇을 기록해야 하는지 정리한 문서다.

## 권장 시뮬레이션 1. MQTT Broker 연결 끊김

### 방법

1. `MqttSubscriberNode` 또는 `MqttPublisherNode`를 사용하는 runner 실행
2. Mosquitto Broker 중지
3. 잠시 후 Broker 재시작

### 확인 항목

- 자동 재연결 시도 여부
- 재연결 후 구독 복원 여부
- 재연결 전후 예외 로그
- 메시지 유실 여부

### 개선 포인트

- 재연결 후 재구독이 확실히 되는지
- 버퍼 누적 없이 정상 복구되는지

## 권장 시뮬레이션 2. MODBUS 장비 응답 없음

### 방법

1. `ModbusReaderNode` 또는 `ModbusWriterNode` 초기화
2. `ModbusTcpSimulator` 중지
3. 읽기/쓰기 요청 발생

### 확인 항목

- 타임아웃 또는 예외 발생 여부
- `ModbusReaderNode`의 `error` 포트 메시지 여부
- Writer 로그 출력 여부

### 개선 포인트

- Reader/Writer 모두 에러 경로가 일관적인지
- 재시도 정책이 필요한지

## 권장 시뮬레이션 3. 잘못된 JSON 수신

### 방법

1. `MqttSubscriberNode`를 실행
2. `mosquitto_pub`로 잘못된 JSON 발행

예:

```bash
mosquitto_pub -h localhost -p 1883 -t "sensor/temp" -m '{invalid json}'
```

### 확인 항목

- `rawPayload`가 들어간 메시지 생성 여부
- 플로우가 중단되지 않는지

### 개선 포인트

- rawPayload를 받은 뒤 downstream에서 어떻게 처리할지
- 파싱 실패 메시지를 별도 에러 경로로 보낼지

## 권장 시뮬레이션 4. 높은 메시지 빈도

### 방법

1. MQTT runner 실행
2. 짧은 간격으로 메시지 반복 발행

### 확인 항목

- 메시지 누락 여부
- `Connection` buffer 적체
- 처리 지연 증가 여부

### 개선 포인트

- 큐 용량 조정
- backpressure 또는 drop 정책 필요성

## 기록 템플릿

### 에러 상황

- 이름:
- 재현 방법:

### 관찰 결과

- 로그:
- 메시지 흐름:
- 복구 여부:

### 개선 필요 사항

- 1.
- 2.
