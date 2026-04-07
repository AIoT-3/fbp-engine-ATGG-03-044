## 과제 1-2. FBP 엔진 핵심 클래스 목록

| 이름 | 종류 | 패키지 | 역할 |
|---|---|---|---|
| Node | 인터페이스 | com.fbp.engine.core | 모든 노드가 공통으로 따라야 하는 기본 동작을 정의한다. |
| InPort | 클래스 | com.fbp.engine.core | 다른 노드로부터 메시지를 입력받는 포트이다. |
| OutPort | 클래스 | com.fbp.engine.core | 처리한 메시지를 다음 노드로 전달하는 출력 포트이다. |
| Connection | 클래스 | com.fbp.engine.core | OutPort와 InPort를 연결하여 메시지 이동 경로를 만든다. |
| Flow | 클래스 | com.fbp.engine.core | 여러 노드와 연결 정보를 묶어 하나의 FBP 네트워크를 표현한다. |
| FlowEngine | 클래스 | com.fbp.engine.core | Flow를 등록하고 실행·중지하는 엔진의 관리자 역할을 한다. |
| Message | 클래스 | com.fbp.engine.message | 노드 사이를 이동하는 데이터 객체이다. |
| SensorNode | 클래스 | com.fbp.engine.node | 센서 데이터를 생성하여 다음 노드로 보내는 노드이다. |
| FilterNode | 클래스 | com.fbp.engine.node | 입력된 메시지를 조건에 따라 필터링하거나 분기하는 노드이다. |
| AlertNode | 클래스 | com.fbp.engine.node | 조건을 만족한 메시지에 대해 알림을 출력하거나 전송하는 노드이다. |
| MainRunner | 클래스 | com.fbp.engine.runner | FlowEngine을 실행하는 프로그램 시작점이다. |