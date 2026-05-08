package com.fbp.engine.core;

/**
 * 에러 전용 출력 포트임을 타입으로 표시하기 위한 클래스다.
 * 현재 동작은 DefaultOutputPort와 같고, Stage 9 에러 플로우 확장에서 의미가 커진다.
 */
public class ErrorPort extends DefaultOutputPort {
    public ErrorPort(String name) {
        super(name);
    }
}
