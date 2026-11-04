package com.smarttraveler.chat;

/** 외부 요청을 분리해 실제 키 없이도 조건 추출을 검증한다. */
public interface OpenAiTransport {
    String request(String body);
}
