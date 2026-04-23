package com.fbp.engine.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModbusExceptionTest {

    @Test
    @DisplayName("getMessage 포맷")
    void MessageTest() {
        ModbusException exception = new ModbusException(0x03, 0x02);

        assertTrue(exception.getMessage().contains("FC: 0x03"));
        assertTrue(exception.getMessage().contains("Exception: 0x02"));
        assertTrue(exception.getMessage().contains("Illegal Data Address"));
    }

    @Test
    @DisplayName("getExceptionCode")
    void ExceptionCodeTest() {
        ModbusException exception = new ModbusException(0x06, 0x04);

        assertEquals(0x04, exception.getExceptionCode());
    }

    @Test
    @DisplayName("상수 값")
    void ConstantTest() {
        assertEquals(0x01, ModbusException.ILLEGAL_FUNCTION);
        assertEquals(0x02, ModbusException.ILLEGAL_DATA_ADDRESS);
        assertEquals(0x03, ModbusException.ILLEGAL_DATA_VALUE);
        assertEquals(0x04, ModbusException.SLAVE_DEVICE_FAILURE);
    }
}
