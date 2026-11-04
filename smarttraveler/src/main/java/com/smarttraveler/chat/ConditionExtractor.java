package com.smarttraveler.chat;

import tools.jackson.databind.node.ObjectNode;

/** 대화에서 확인할 수 있는 조건만 반영한다. 나중에 AI 추출기로 교체할 수 있다. */
public interface ConditionExtractor {
    void extract(String message, String previousQuestion, ObjectNode conditions);
}
