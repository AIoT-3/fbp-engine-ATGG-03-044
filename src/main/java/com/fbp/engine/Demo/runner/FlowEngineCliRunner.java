// 과제 8-5: FlowEngine CLI 실행 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

//과제 8-5: FlowEngine에 간단한 CLI를 추가하라. Scanner로 사용자 입력을 받아 처리한다.
@Slf4j
public class FlowEngineCliRunner {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        FlowEngine engine = new FlowEngine();
        TimerNode timerNode = new TimerNode("timer-1", 1000);
        PrintNode printNode = new PrintNode("printer-1");

        Flow monitoring = new Flow("monitoring")
                .addNode(timerNode)
                .addNode(printNode)
                .connect("timer-1", "out", "printer-1", "in");

        engine.register(monitoring);

        while (true) {
            log.info("fbp>");
            String line = scanner.nextLine();

            if (line.equals("list")) {
                engine.listFlows();
            } else if (line.startsWith("start ")) {
                String id = line.substring(6);
                engine.startFlow(id);
            } else if (line.startsWith("stop ")) {
                String id = line.substring(5);
                engine.stopFlow(id);
            } else if (line.equals("exit")) {
                engine.shutdown();
                log.info("[Engine] 엔진 종료됨");
                break;
            }
        }
    }
}
