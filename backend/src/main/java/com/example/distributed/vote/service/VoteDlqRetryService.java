package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import tools.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.vote", name = "mq-enabled", havingValue = "true", matchIfMissing = true)
public class VoteDlqRetryService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final VoteProperties voteProperties;

    public int retryFromDlq(int maxRetry) {
        int retried = 0;
        for (int i = 0; i < maxRetry; i++) {
            Object payload = rabbitTemplate.receiveAndConvert(voteProperties.getMqDlqQueue());
            if (payload == null) {
                break;
            }
            VoteEventMessage message = toMessage(payload);
            rabbitTemplate.convertAndSend(
                    voteProperties.getMqExchange(),
                    voteProperties.getMqRoutingKey(),
                    message,
                    msg -> {
                        msg.getMessageProperties().setMessageId(message.eventId());
                        msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return msg;
                    }
            );
            retried++;
        }
        return retried;
    }

    private VoteEventMessage toMessage(Object payload) {
        if (payload instanceof VoteEventMessage voteEventMessage) {
            return voteEventMessage;
        }
        if (payload instanceof LinkedHashMap<?, ?> map) {
            return objectMapper.convertValue(map, VoteEventMessage.class);
        }
        throw new VoteBusinessException("DLQ_MESSAGE_FORMAT_ERROR", "死信消息格式不正确", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
