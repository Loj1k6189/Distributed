package com.example.distributed.quest.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question_options")
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", length = 20, nullable = false)
    private OptionType optionType;

    @Column(name = "option_key", length = 100, nullable = false)
    private String optionKey;

    @Column(name = "option_value", columnDefinition = "TEXT")
    private String optionValue;

    @Column(name = "placeholder", columnDefinition = "TEXT")
    private String placeholder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rule", columnDefinition = "json")
    private Map<String, Object> validationRule;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_required")
    private Boolean required;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum OptionType {
        TEXT("文本输入框"),
        TEXTAREA("文本域"),
        RADIO("单选按钮"),
        CHECKBOX("复选框"),
        SELECT("下拉选择"),
        IMAGE("图片上传"),
        VIDEO("视频上传"),
        FILE("文件上传"),
        RATING("评分"),
        BOOLEAN("布尔值"),
        DATE("日期"),
        TIME("时间"),
        DATETIME("日期时间"),
        EMAIL("邮箱输入"),
        PHONE("电话输入"),
        NUMBER("数字输入");

        private final String description;

        OptionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}