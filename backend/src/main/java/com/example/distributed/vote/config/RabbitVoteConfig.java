package com.example.distributed.vote.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.vote", name = "mq-enabled", havingValue = "true", matchIfMissing = true)
public class RabbitVoteConfig {

    private final VoteProperties voteProperties;

    @Bean
    public DirectExchange voteEventExchange() {
        return new DirectExchange(voteProperties.getMqExchange(), true, false);
    }

    @Bean
    public DirectExchange voteDlxExchange() {
        return new DirectExchange(voteProperties.getMqDlxExchange(), true, false);
    }

    @Bean
    public Queue voteEventQueue() {
        return QueueBuilder.durable(voteProperties.getMqQueue())
                .withArgument("x-dead-letter-exchange", voteProperties.getMqDlxExchange())
                .withArgument("x-dead-letter-routing-key", voteProperties.getMqDlxRoutingKey())
                .build();
    }

    @Bean
    public Queue voteDlqQueue() {
        return QueueBuilder.durable(voteProperties.getMqDlqQueue()).build();
    }

    @Bean
    public Binding voteEventBinding() {
        return BindingBuilder.bind(voteEventQueue())
                .to(voteEventExchange())
                .with(voteProperties.getMqRoutingKey());
    }

    @Bean
    public Binding voteDlqBinding() {
        return BindingBuilder.bind(voteDlqQueue())
                .to(voteDlxExchange())
                .with(voteProperties.getMqDlxRoutingKey());
    }

    @Bean
    public MessageConverter voteMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter voteMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(voteMessageConverter);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory voteBatchListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter voteMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(voteMessageConverter);
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(voteProperties.getMqBatchSize());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
