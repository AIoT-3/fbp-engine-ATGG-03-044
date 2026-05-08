// 과제 3-10: GeneratorNode -> Connection -> FilterNode(threshold=30) -> Connection -> PrintNode runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.PrintNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneratorFilterPrintRunner {
    public static void main(String[] args) throws InterruptedException {
        GeneratorNode generatorNode = new GeneratorNode("generator-1");
        FilterNode filterNode = new FilterNode("filter-1", "temperature", 30.0);
        PrintNode printNode = new PrintNode("printer-1");

        Connection generatorToFilter = new LocalConnection();
        Connection filterToPrint = new LocalConnection();

        generatorNode.getOutputPort().connect(generatorToFilter);
        filterNode.getOutputPort().connect(filterToPrint);

        runCase(generatorNode, filterNode, printNode, generatorToFilter, filterToPrint, 25.0);
        runCase(generatorNode, filterNode, printNode, generatorToFilter, filterToPrint, 35.0);
    }

    private static void runCase(GeneratorNode generatorNode, FilterNode filterNode, PrintNode printNode,
                                Connection generatorToFilter, Connection filterToPrint,
                                double temperature) throws InterruptedException {
        generatorNode.generate("temperature", temperature);

        Message generated = generatorToFilter.poll();
        filterNode.process(generated);

        if (filterToPrint.getBufferSize() == 0) {
            log.info("temperature={} 메시지는 threshold 미만이라 PrintNode에 도달하지 않음", temperature);
            return;
        }

        Message filtered = filterToPrint.poll();
        printNode.process(filtered);
    }
}
