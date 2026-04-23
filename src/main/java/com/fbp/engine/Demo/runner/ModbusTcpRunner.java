// 과제 3-5: ModbusTcpClient와 ModbusTcpSimulator 독립 동작 확인 runner
package com.fbp.engine.demo.runner;

import com.fbp.engine.exception.ModbusException;
import com.fbp.engine.protocol.ModbusTcpClient;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class ModbusTcpRunner {
    public static void main(String[] args) {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        ModbusTcpClient client = new ModbusTcpClient("localhost", 5020);

        try {
            simulator.setRegister(0, 250);
            simulator.setRegister(1, 600);
            simulator.setRegister(2, 1);
            simulator.start();

            Thread.sleep(200);

            client.connect();

            int[] firstRead = client.readHoldingRegisters(1, 0, 3);
            log.info("첫 번째 읽기: {}", Arrays.toString(firstRead));

            client.writeSingleRegister(1, 2, 100);

            int[] secondRead = client.readHoldingRegisters(1, 0, 3);
            log.info("두 번째 읽기: {}", Arrays.toString(secondRead));
        } catch (IOException | ModbusException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                client.disconnect();
            } catch (IOException ignored) {
            }
            try {
                simulator.stop();
            } catch (IOException ignored) {
            }
        }
    }
}
