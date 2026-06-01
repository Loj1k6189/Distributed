package com.example.distributed.quest.enums;

import lombok.Getter;

/**
 * 题目类型枚举
 */
@Getter
public enum QuestionType {
    
    /**
     * 单选题
     */
    SINGLE_CHOICE("单选题", true),
    
    /**
     * 多选题
     */
    MULTIPLE_CHOICE("多选题", true),
    
    /**
     * 问答题（文本）
     */
    TEXT_ANSWER("问答题", false),
    
    /**
     * 评分题
     */
    RATING("评分题", false),
    
    /**
     * 日期题
     */
    DATE("日期题", false);
    
    private final String description;
    private final boolean hasOptions;
    
    QuestionType(String description, boolean hasOptions) {
        this.description = description;
        this.hasOptions = hasOptions;
    }
}
