package com.example.distributed.quest.service;

public class QuestRedisKeys {
    private static final String PREFIX = "quest:";
    private static final String QUESTION_OPTIONS = PREFIX + "question:%d:options";
    private static final String QUESTION_COUNTS = PREFIX + "question:%d:counts";

    public static String questionOptions(Long questionId) {
        return String.format(QUESTION_OPTIONS, questionId);
    }

    public static String questionCounts(Long questionId) {
        return String.format(QUESTION_COUNTS, questionId);
    }
}