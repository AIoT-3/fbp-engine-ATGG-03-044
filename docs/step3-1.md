# Step 3-1 MODBUS TCP 프레임 수작업 정리

## 1. FC 03 요청 프레임
조건: 슬레이브 1, 주소 10번부터 Holding Register 5개 읽기

```text
[00 01]  Transaction ID = 1
[00 00]  Protocol ID = 0
[00 06]  Length = 6
[01]     Unit ID = 1
[03]     Function Code = FC 03 (Read Holding Registers)
[00 0A]  Start Address = 10
[00 05]  Quantity = 5
```

바이트 배열 형태:

```java
byte[] readRequest = new byte[] {
        0x00, 0x01,       // Transaction ID = 1
        0x00, 0x00,       // Protocol ID = 0
        0x00, 0x06,       // Length = 6
        0x01,             // Unit ID = 1
        0x03,             // Function Code = FC 03
        0x00, 0x0A,       // Start Address = 10
        0x00, 0x05        // Quantity = 5
};
```

## 2. FC 06 요청 프레임
조건: 슬레이브 1, 주소 5번에 값 1234 쓰기

1234의 16진수 값은 `0x04D2` 이다.

```text
[00 02]  Transaction ID = 2
[00 00]  Protocol ID = 0
[00 06]  Length = 6
[01]     Unit ID = 1
[06]     Function Code = FC 06 (Write Single Register)
[00 05]  Register Address = 5
[04 D2]  Value = 1234
```

바이트 배열 형태:

```java
byte[] writeRequest = new byte[] {
        0x00, 0x02,       // Transaction ID = 2
        0x00, 0x00,       // Protocol ID = 0
        0x00, 0x06,       // Length = 6
        0x01,             // Unit ID = 1
        0x06,             // Function Code = FC 06
        0x00, 0x05,       // Register Address = 5
        0x04, (byte) 0xD2 // Value = 1234
};
```

## 3. FC 03 읽기 응답 프레임
조건: 레지스터 5개 값이 `[100, 200, 300, 400, 500]`

각 값의 16진수:

- 100 = `00 64`
- 200 = `00 C8`
- 300 = `01 2C`
- 400 = `01 90`
- 500 = `01 F4`

응답 프레임:

```text
[00 01]  Transaction ID = 1
[00 00]  Protocol ID = 0
[00 0D]  Length = 13
[01]     Unit ID = 1
[03]     Function Code = FC 03
[0A]     Byte Count = 10
[00 64]  Register 10 = 100
[00 C8]  Register 11 = 200
[01 2C]  Register 12 = 300
[01 90]  Register 13 = 400
[01 F4]  Register 14 = 500
```

바이트 배열 형태:

```java
byte[] readResponse = new byte[] {
        0x00, 0x01,       // Transaction ID = 1
        0x00, 0x00,       // Protocol ID = 0
        0x00, 0x0D,       // Length = 13
        0x01,             // Unit ID = 1
        0x03,             // Function Code = FC 03
        0x0A,             // Byte Count = 10
        0x00, 0x64,       // Register 10 = 100
        0x00, (byte) 0xC8,// Register 11 = 200
        0x01, 0x2C,       // Register 12 = 300
        0x01, (byte) 0x90,// Register 13 = 400
        0x01, (byte) 0xF4 // Register 14 = 500
};
```
