package com.fbp.engine.plugin;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.RuleExpression;
import com.fbp.engine.node.AlertNode;
import com.fbp.engine.node.CollectorNode;
import com.fbp.engine.node.CompositeRuleNode;
import com.fbp.engine.node.CounterNode;
import com.fbp.engine.node.DelayNode;
import com.fbp.engine.node.DeadLetterNode;
import com.fbp.engine.node.DynamicRouterNode;
import com.fbp.engine.node.ErrorHandlerNode;
import com.fbp.engine.node.FileWriterNode;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.HumiditySensorNode;
import com.fbp.engine.node.LogNode;
import com.fbp.engine.node.MergeNode;
import com.fbp.engine.node.ModbusReaderNode;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.node.RoutingRule;
import com.fbp.engine.node.SplitNode;
import com.fbp.engine.node.TemperatureSensorNode;
import com.fbp.engine.node.ThresholdFilterNode;
import com.fbp.engine.node.TimeWindowRuleNode;
import com.fbp.engine.node.TimerNode;
import com.fbp.engine.node.TransformNode;
import com.fbp.engine.registry.NodeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuiltInNodeProvider implements NodeProvider {
    @Override
    public List<NodeDescriptor> getNodeDescriptors() {
        return List.of(
                descriptor("Generator", "메시지를 수동 생성하는 노드", GeneratorNode.class, (id, config) -> new GeneratorNode(id)),
                descriptor("Timer", "주기적으로 tick 메시지를 생성하는 노드", TimerNode.class,
                        (id, config) -> new TimerNode(id, longValue(config, "intervalMs", 1000L))),
                descriptor("Print", "메시지를 콘솔에 출력하는 노드", PrintNode.class, (id, config) -> new PrintNode(id)),
                descriptor("Log", "메시지를 로그로 남기는 노드", LogNode.class, (id, config) -> new LogNode(id)),
                descriptor("Alert", "알림 메시지를 처리하는 노드", AlertNode.class, (id, config) -> new AlertNode(id)),
                descriptor("Collector", "테스트/검증용 메시지 수집 노드", CollectorNode.class, (id, config) -> new CollectorNode(id)),
                descriptor("Counter", "메시지 수를 세는 노드", CounterNode.class, (id, config) -> new CounterNode(id)),
                descriptor("Merge", "두 입력 메시지를 병합하는 노드", MergeNode.class, (id, config) -> new MergeNode(id)),
                descriptor("Delay", "메시지 처리를 지연시키는 노드", DelayNode.class,
                        (id, config) -> new DelayNode(id, longValue(config, "delayMs", 0L))),
                descriptor("Filter", "숫자 필드 기준 필터 노드", FilterNode.class,
                        (id, config) -> new FilterNode(id, firstStringValue(config, "field", "key"), doubleValue(config, "threshold", 0.0))),
                descriptor("ThresholdFilter", "임계값 기준 alert/normal 분기 노드", ThresholdFilterNode.class,
                        (id, config) -> new ThresholdFilterNode(id, firstStringValue(config, "field", "fieldName"), doubleValue(config, "threshold", 0.0))),
                descriptor("Split", "임계값 기준 match/mismatch 분기 노드", SplitNode.class,
                        (id, config) -> new SplitNode(id, firstStringValue(config, "field", "key"), doubleValue(config, "threshold", 0.0))),
                descriptor("Rule", "표현식 기반 match/mismatch 분기 노드", RuleNode.class,
                        (id, config) -> new RuleNode(id, stringValue(config, "expression", null))),
                descriptor("CompositeRule", "여러 조건을 AND/OR로 평가하는 노드", CompositeRuleNode.class, BuiltInNodeProvider::compositeRule),
                descriptor("TimeWindowRule", "시간 창 안의 조건 발생 횟수를 평가하는 노드", TimeWindowRuleNode.class,
                        (id, config) -> new TimeWindowRuleNode(
                                id,
                                RuleExpression.parse(stringValue(config, "expression", null))::evaluate,
                                longValue(config, "windowMs", 1000L),
                                intValue(config, "threshold", 1)
                        )),
                descriptor("Transform", "기본 identity transform 노드", TransformNode.class,
                        (id, config) -> new TransformNode(id, message -> message)),
                descriptor("TemperatureSensor", "온도 센서 노드", TemperatureSensorNode.class,
                        (id, config) -> new TemperatureSensorNode(id, doubleValue(config, "min", 0.0), doubleValue(config, "max", 100.0))),
                descriptor("HumiditySensor", "습도 센서 노드", HumiditySensorNode.class,
                        (id, config) -> new HumiditySensorNode(id, doubleValue(config, "min", 0.0), doubleValue(config, "max", 100.0))),
                descriptor("FileWriter", "메시지를 파일에 쓰는 노드", FileWriterNode.class,
                        (id, config) -> new FileWriterNode(id, stringValue(config, "filePath", null))),
                descriptor("MqttSubscriber", "MQTT 구독 노드", MqttSubscriberNode.class, MqttSubscriberNode::new),
                descriptor("MqttPublisher", "MQTT 발행 노드", MqttPublisherNode.class, MqttPublisherNode::new),
                descriptor("ModbusReader", "MODBUS TCP 읽기 노드", ModbusReaderNode.class, ModbusReaderNode::new),
                descriptor("ModbusWriter", "MODBUS TCP 쓰기 노드", ModbusWriterNode.class, ModbusWriterNode::new),
                descriptor("DynamicRouter", "메시지 내용에 따라 출력 포트를 선택하는 노드", DynamicRouterNode.class, BuiltInNodeProvider::dynamicRouter),
                descriptor("ErrorHandler", "에러 메시지를 retry/deadLetter로 분기하는 노드", ErrorHandlerNode.class,
                        (id, config) -> new ErrorHandlerNode(id, intValue(config, "maxRetries", 0))),
                descriptor("DeadLetter", "처리 불가 메시지를 보관하는 노드", DeadLetterNode.class,
                        (id, config) -> new DeadLetterNode(id))
        );
    }

    private static <T extends AbstractNode> NodeDescriptor descriptor(String typeName, String description, Class<T> nodeClass, NodeFactory factory) {
        NodeDescriptor descriptor = new NodeDescriptor(typeName, description, nodeClass, factory);
        return descriptor;
    }

    private static CompositeRuleNode compositeRule(String id, Map<String, Object> config) {
        String operatorText = stringValue(config, "operator", "AND");
        CompositeRuleNode node = new CompositeRuleNode(id, CompositeRuleNode.Operator.valueOf(operatorText.toUpperCase()));
        Object rawConditions = config.get("conditions");
        if (rawConditions instanceof List<?> conditions) {
            for (Object rawCondition : conditions) {
                if (rawCondition instanceof Map<?, ?> condition) {
                    String field = objectAsString(condition.get("field"), "field");
                    String operator = objectAsString(condition.get("operator"), "operator");
                    Object value = condition.get("value");
                    node.addCondition(field, operator, value);
                }
            }
        }
        return node;
    }

    private static DynamicRouterNode dynamicRouter(String id, Map<String, Object> config) {
        List<RoutingRule> rules = new ArrayList<>();
        Object rawRules = config.get("rules");
        if (rawRules instanceof List<?> ruleDefinitions) {
            for (Object rawRule : ruleDefinitions) {
                if (rawRule instanceof Map<?, ?> rule) {
                    rules.add(new RoutingRule(
                            objectAsString(rule.get("field"), "field"),
                            objectAsString(rule.get("operator"), "operator"),
                            rule.get("value"),
                            objectAsString(rule.get("outputPort"), "outputPort")
                    ));
                }
            }
        }
        String defaultPort = stringValue(config, "defaultPort", "default");
        DynamicRouterNode node = new DynamicRouterNode(id, rules, defaultPort);
        return node;
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            if (defaultValue == null) {
                throw new PluginException("필수 config가 없습니다: " + key);
            }
            return defaultValue;
        }
        if (value instanceof String stringValue && !stringValue.trim().isEmpty()) {
            return stringValue;
        }
        throw new PluginException("config는 문자열이어야 합니다: " + key);
    }

    private static String firstStringValue(Map<String, Object> config, String firstKey, String secondKey) {
        Object firstValue = config.get(firstKey);
        if (firstValue instanceof String stringValue && !stringValue.trim().isEmpty()) {
            return stringValue;
        }
        Object secondValue = config.get(secondKey);
        if (secondValue instanceof String stringValue && !stringValue.trim().isEmpty()) {
            return stringValue;
        }
        throw new PluginException("필수 config가 없습니다: " + firstKey + " 또는 " + secondKey);
    }

    private static String objectAsString(Object value, String key) {
        if (value instanceof String stringValue && !stringValue.trim().isEmpty()) {
            return stringValue;
        }
        throw new PluginException("condition " + key + "는 문자열이어야 합니다");
    }

    private static int intValue(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new PluginException("config는 숫자여야 합니다: " + key);
    }

    private static long longValue(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new PluginException("config는 숫자여야 합니다: " + key);
    }

    private static double doubleValue(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new PluginException("config는 숫자여야 합니다: " + key);
    }
}
