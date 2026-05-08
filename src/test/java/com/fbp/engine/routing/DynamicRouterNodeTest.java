package com.fbp.engine.routing;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.DynamicRouterNode;
import com.fbp.engine.node.RoutingRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DynamicRouterNode - 메시지 내용 기준 동적 라우팅")
class DynamicRouterNodeTest {
    @Test
    @DisplayName("조건이 일치하면 해당 규칙의 출력 포트로 메시지를 보낸다")
    void routesToFirstMatchingPort() throws InterruptedException {
        DynamicRouterNode router = new DynamicRouterNode(
                "router",
                List.of(new RoutingRule("type", "==", "temp", "temperature")),
                "default"
        );
        Connection temperature = new LocalConnection("temperature");
        Connection fallback = new LocalConnection("fallback");
        router.getOutputPort("temperature").connect(temperature);
        router.getOutputPort("default").connect(fallback);

        router.process(new Message(Map.of("type", "temp")));

        assertEquals("temp", temperature.poll().get("type"));
        assertEquals(0, fallback.getBufferSize());
    }

    @Test
    @DisplayName("어떤 규칙도 맞지 않으면 default 포트로 메시지를 보낸다")
    void routesToDefaultWhenNoRuleMatches() throws InterruptedException {
        DynamicRouterNode router = new DynamicRouterNode(
                "router",
                List.of(new RoutingRule("type", "==", "temp", "temperature")),
                "default"
        );
        Connection fallback = new LocalConnection("fallback");
        router.getOutputPort("default").connect(fallback);

        router.process(new Message(Map.of("type", "humidity")));

        assertEquals("humidity", fallback.poll().get("type"));
    }

    @Test
    @DisplayName("여러 규칙이 맞으면 먼저 등록된 규칙이 우선한다")
    void firstMatchingRuleWins() throws InterruptedException {
        DynamicRouterNode router = new DynamicRouterNode(
                "router",
                List.of(
                        new RoutingRule("value", ">", 10, "first"),
                        new RoutingRule("value", ">", 5, "second")
                ),
                "default"
        );
        Connection first = new LocalConnection("first");
        Connection second = new LocalConnection("second");
        router.getOutputPort("first").connect(first);
        router.getOutputPort("second").connect(second);

        router.process(new Message(Map.of("value", 20)));

        assertEquals(20, ((Number) first.poll().get("value")).intValue());
        assertEquals(0, second.getBufferSize());
    }

    @Test
    @DisplayName("규칙 목록이 비어 있으면 모든 메시지가 default 포트로 이동한다")
    void routesToDefaultWhenRulesAreEmpty() throws InterruptedException {
        DynamicRouterNode router = new DynamicRouterNode("router", List.of(), "default");
        Connection fallback = new LocalConnection("fallback");
        router.getOutputPort("default").connect(fallback);

        router.process(new Message(Map.of("type", "temp")));

        assertEquals("temp", fallback.poll().get("type"));
    }

    @Test
    @DisplayName("라우팅 필드가 메시지에 없으면 default 포트로 이동한다")
    void routesToDefaultWhenFieldIsMissing() throws InterruptedException {
        DynamicRouterNode router = new DynamicRouterNode(
                "router",
                List.of(new RoutingRule("type", "==", "temp", "temperature")),
                "default"
        );
        Connection fallback = new LocalConnection("fallback");
        router.getOutputPort("default").connect(fallback);

        router.process(new Message(Map.of("value", 10)));

        assertEquals(10, ((Number) fallback.poll().get("value")).intValue());
    }

    @Test
    @DisplayName("실행 중 규칙을 추가하면 이후 메시지 라우팅에 반영된다")
    void canAddRuleAtRuntime() throws InterruptedException {
        DynamicRouterNode router = new DynamicRouterNode("router", List.of(), "default");
        RoutingRule rule = new RoutingRule("type", "==", "temp", "temperature");
        router.addRule(rule);
        Connection temperature = new LocalConnection("temperature");
        router.getOutputPort("temperature").connect(temperature);

        router.process(new Message(Map.of("type", "temp")));

        assertEquals("temp", temperature.poll().get("type"));
    }

    @Test
    @DisplayName("실행 중 규칙을 제거하면 이후 메시지는 default 포트로 이동할 수 있다")
    void canRemoveRuleAtRuntime() throws InterruptedException {
        RoutingRule rule = new RoutingRule("type", "==", "temp", "temperature");
        DynamicRouterNode router = new DynamicRouterNode("router", List.of(rule), "default");
        Connection temperature = new LocalConnection("temperature");
        Connection fallback = new LocalConnection("fallback");
        router.getOutputPort("temperature").connect(temperature);
        router.getOutputPort("default").connect(fallback);

        assertTrue(router.removeRule(rule));
        router.process(new Message(Map.of("type", "temp")));

        assertEquals(0, temperature.getBufferSize());
        assertEquals("temp", fallback.poll().get("type"));
    }

    @Test
    @DisplayName("100개 규칙 중 마지막 규칙 매칭도 제한 시간 안에 처리된다")
    void evaluatesHundredRulesWithinReasonableTime() {
        List<RoutingRule> rules = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            rules.add(new RoutingRule("type", "==", "type-" + i, "port-" + i));
        }
        DynamicRouterNode router = new DynamicRouterNode("router", rules, "default");
        Connection matched = new LocalConnection("matched");
        router.getOutputPort("port-99").connect(matched);

        assertTimeoutPreemptively(java.time.Duration.ofMillis(100), () ->
                router.process(new Message(Map.of("type", "type-99"))));

        assertEquals(1, matched.getBufferSize());
    }
}
