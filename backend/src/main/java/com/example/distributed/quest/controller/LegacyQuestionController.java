package com.example.distributed.quest.controller;

import com.example.distributed.quest.dto.AnswerSubmissionRequest;
import com.example.distributed.quest.dto.LegacyQuestionDto;
import com.example.distributed.quest.dto.QuestionnaireCreateRequest;
import com.example.distributed.quest.entity.Question;
import com.example.distributed.quest.entity.QuestionOption;
import com.example.distributed.quest.entity.Questionnaire;
import com.example.distributed.quest.entity.QuestionnaireAnswer;
import com.example.distributed.quest.enums.QuestionType;
import com.example.distributed.quest.exception.QuestionnaireException;
import com.example.distributed.quest.repository.QuestionRepository;
import com.example.distributed.quest.repository.QuestionnaireAnswerRepository;
import com.example.distributed.quest.repository.QuestionnaireRepository;
import com.example.distributed.quest.service.QuestionnaireService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 兼容旧版前端的问卷接口（/api/v1/question）
 */
@RestController
@RequestMapping("/api/v1/question")
@RequiredArgsConstructor
@Slf4j
public class LegacyQuestionController {

    private final QuestionnaireService questionnaireService;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final QuestionnaireAnswerRepository answerRepository;

    @PostMapping
    public ResponseEntity<LegacyQuestionDto.CreateResponse> createQuestion(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @Valid @RequestBody LegacyQuestionDto.CreateRequest request) {

        QuestionnaireCreateRequest createRequest = new QuestionnaireCreateRequest();
        createRequest.setTitle(request.getTitle());
        createRequest.setDescription(request.getDescription());
        createRequest.setAllowAnonymous(true);

        QuestionnaireCreateRequest.QuestionDTO question = new QuestionnaireCreateRequest.QuestionDTO();
        question.setContent(request.getTitle());
        question.setQuestionType(Boolean.TRUE.equals(request.getAllowMultiple())
                ? QuestionType.MULTIPLE_CHOICE
                : QuestionType.SINGLE_CHOICE);
        question.setSortOrder(0);
        question.setIsRequired(true);

        List<QuestionnaireCreateRequest.OptionDTO> options = new ArrayList<>();
        int sortOrder = 0;
        for (String optionValue : request.getOptions()) {
            QuestionnaireCreateRequest.OptionDTO option = new QuestionnaireCreateRequest.OptionDTO();
            option.setContent(optionValue);
            option.setSortOrder(sortOrder++);
            options.add(option);
        }
        question.setOptions(options);
        createRequest.setQuestions(List.of(question));

        Questionnaire questionnaire = questionnaireService.createQuestionnaire(userId, createRequest);

        LegacyQuestionDto.CreateResponse response = new LegacyQuestionDto.CreateResponse();
        response.setId(questionnaire.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<LegacyQuestionDto.ListItem> listQuestions() {
        return questionnaireRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Questionnaire::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public LegacyQuestionDto.DetailResponse getQuestion(@PathVariable Long id) {
        Questionnaire questionnaire = questionnaireRepository.findByIdBasic(id)
                .orElseThrow(() -> new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_NOT_FOUND));

        Question primaryQuestion = loadPrimaryQuestion(id);

        LegacyQuestionDto.DetailResponse response = new LegacyQuestionDto.DetailResponse();
        response.setId(questionnaire.getId());
        response.setTitle(questionnaire.getTitle());
        response.setDescription(questionnaire.getDescription());
        response.setAllowMultiple(primaryQuestion.getQuestionType() == QuestionType.MULTIPLE_CHOICE);
        response.setOptions(primaryQuestion.getOptions().stream()
                .sorted(Comparator.comparing(QuestionOption::getSortOrder))
                .map(this::toOptionResponse)
                .collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/submitted")
    public LegacyQuestionDto.SubmittedResponse checkSubmitted(
            @RequestParam String userId,
            @RequestParam Long questionId) {
        boolean submitted = answerRepository.existsByUserIdAndQuestionnaireId(userId, questionId);
        LegacyQuestionDto.SubmittedResponse response = new LegacyQuestionDto.SubmittedResponse();
        response.setSubmitted(submitted);
        return response;
    }

    @PostMapping("/submit")
    public ResponseEntity<LegacyQuestionDto.SubmitResponse> submitAnswer(
            @RequestHeader(value = "X-Forwarded-For", required = false) String userIp,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @Valid @RequestBody LegacyQuestionDto.SubmitRequest request) {

        if (userIp == null) {
            userIp = "127.0.0.1";
        }
        if (userAgent == null) {
            userAgent = "unknown";
        }

        Question primaryQuestion = loadPrimaryQuestion(request.getQuestionId());

        AnswerSubmissionRequest submissionRequest = new AnswerSubmissionRequest();
        submissionRequest.setQuestionnaireId(request.getQuestionId());
        submissionRequest.setIsAnonymous(false);

        AnswerSubmissionRequest.AnswerDTO answerDTO = new AnswerSubmissionRequest.AnswerDTO();
        answerDTO.setQuestionId(primaryQuestion.getId());
        answerDTO.setSelectedOptionIds(request.getOptionIds());
        submissionRequest.setAnswers(List.of(answerDTO));

        Long answerId = questionnaireService.submitAnswer(
                request.getUserId(),
                userIp,
                userAgent,
                submissionRequest);

        LegacyQuestionDto.SubmitResponse response = new LegacyQuestionDto.SubmitResponse();
        response.setSuccess(true);
        response.setAnswerId(answerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/stats/{id}")
    @Transactional
    public LegacyQuestionDto.StatsResponse getStats(@PathVariable Long id) {
        Questionnaire questionnaire = questionnaireRepository.findByIdBasic(id)
                .orElseThrow(() -> new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_NOT_FOUND));

        Question primaryQuestion = loadPrimaryQuestion(id);

        Map<Long, LegacyQuestionDto.OptionStats> statsMap = new LinkedHashMap<>();
        primaryQuestion.getOptions().stream()
                .sorted(Comparator.comparing(QuestionOption::getSortOrder))
                .forEach(option -> {
                    LegacyQuestionDto.OptionStats stat = new LegacyQuestionDto.OptionStats();
                    stat.setOptionId(option.getId());
                    stat.setOptionText(option.getContent());
                    stat.setVotes(0L);
                    stat.setPercentage(0.0);
                    stat.setRank(0);
                    statsMap.put(option.getId(), stat);
                });

        List<QuestionnaireAnswer> answers = answerRepository.findWithAnswersByQuestionnaireId(id);
        for (QuestionnaireAnswer answer : answers) {
            if (answer.getQuestionAnswers() == null) {
                continue;
            }
            answer.getQuestionAnswers().forEach(questionAnswer -> {
                if (questionAnswer.getQuestion() == null
                        || !questionAnswer.getQuestion().getId().equals(primaryQuestion.getId())) {
                    return;
                }
                String selected = questionAnswer.getSelectedOptionIds();
                if (selected == null || selected.isBlank()) {
                    return;
                }
                for (String part : selected.split(",")) {
                    String trimmed = part.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (!trimmed.chars().allMatch(Character::isDigit)) {
                        log.warn("统计数据包含无效选项ID: questionnaireId={}, rawValue={}", id, trimmed);
                        continue;
                    }
                    Long optionId = Long.parseLong(trimmed);
                    LegacyQuestionDto.OptionStats stat = statsMap.get(optionId);
                    if (stat != null) {
                        stat.setVotes(stat.getVotes() + 1);
                    }
                }
            });
        }

        long totalSubmissions = answers.size();
        if (totalSubmissions > 0) {
            for (LegacyQuestionDto.OptionStats stat : statsMap.values()) {
                stat.setPercentage(stat.getVotes() / (double) totalSubmissions);
            }
        }

        List<LegacyQuestionDto.OptionStats> optionStats = new ArrayList<>(statsMap.values());
        optionStats.sort(Comparator.comparing(LegacyQuestionDto.OptionStats::getVotes, Comparator.reverseOrder()));
        int rank = 1;
        for (LegacyQuestionDto.OptionStats stat : optionStats) {
            stat.setRank(rank++);
        }

        LegacyQuestionDto.StatsResponse response = new LegacyQuestionDto.StatsResponse();
        response.setQuestionTitle(questionnaire.getTitle());
        response.setTotalSubmissions(totalSubmissions);
        response.setLastUpdated(resolveLastUpdated(answers));
        response.setOptions(optionStats);
        return response;
    }

    private LegacyQuestionDto.ListItem toListItem(Questionnaire questionnaire) {
        LegacyQuestionDto.ListItem item = new LegacyQuestionDto.ListItem();
        item.setId(questionnaire.getId());
        item.setTitle(questionnaire.getTitle());
        item.setDescription(questionnaire.getDescription());
        item.setIsActive(questionnaire.getIsActive());
        item.setCreatedAt(questionnaire.getCreatedAt() != null ? questionnaire.getCreatedAt().toString() : null);
        item.setExpiresAt(questionnaire.getEndTime() != null ? questionnaire.getEndTime().toString() : null);
        return item;
    }

    private LegacyQuestionDto.OptionResponse toOptionResponse(QuestionOption option) {
        LegacyQuestionDto.OptionResponse response = new LegacyQuestionDto.OptionResponse();
        response.setId(option.getId());
        response.setOptionValue(option.getContent());
        return response;
    }

    private Question loadPrimaryQuestion(Long questionnaireId) {
        List<Question> questions = questionRepository.findByQuestionnaireIdWithOptions(questionnaireId);
        if (questions == null || questions.isEmpty()) {
            throw new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTION_NOT_FOUND);
        }
        return questions.get(0);
    }

    private String resolveLastUpdated(List<QuestionnaireAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        LocalDateTime submittedAt = answers.get(0).getSubmittedAt();
        return submittedAt != null ? submittedAt.toString() : null;
    }
}
