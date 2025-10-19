package com.techhub.app.commonservice.kafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
@Slf4j
public class KafkaConfig {

    private static final String DEFAULT_TRUSTED_PACKAGES = "*";

    private final KafkaProperties kafkaProperties;
    private final Environment environment;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, determineBootstrapServers());
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.putIfAbsent(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, determineBootstrapServers());
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                environment.getProperty("kafka.consumer.auto-offset-reset", "latest"));
        props.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.addTrustedPackages(resolveTrustedPackages());
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.setRemoveTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(resolveConcurrency());
        factory.getContainerProperties().setAckMode(resolveAckMode());
        factory.getContainerProperties().setMissingTopicsFatal(false);
        factory.setAutoStartup(resolveAutoStartup());
        return factory;
    }

    private String determineBootstrapServers() {
        List<String> servers = kafkaProperties.getBootstrapServers();
        if (!CollectionUtils.isEmpty(servers)) {
            return String.join(",", servers);
        }

        String configuredList = environment.getProperty("kafka.bootstrap-servers");
        if (StringUtils.hasText(configuredList)) {
            return configuredList;
        }

        String host = environment.getProperty("kafka.hostname");
        String port = environment.getProperty("kafka.port");
        if (StringUtils.hasText(host) && StringUtils.hasText(port)) {
            return host + ":" + port;
        }

        throw new IllegalStateException(
                "Kafka bootstrap servers are not configured. " +
                "Provide 'spring.kafka.bootstrap-servers', 'kafka.bootstrap-servers', or 'kafka.hostname'/'kafka.port'.");
    }

    private String[] resolveTrustedPackages() {
        String configured = environment.getProperty("kafka.consumer.trusted-packages");
        if (!StringUtils.hasText(configured)) {
            return new String[]{DEFAULT_TRUSTED_PACKAGES};
        }
        return StringUtils.commaDelimitedListToStringArray(configured);
    }

    private int resolveConcurrency() {
        return environment.getProperty("kafka.consumer.concurrency", Integer.class, 1);
    }

    private boolean resolveAutoStartup() {
        return environment.getProperty("kafka.consumer.auto-startup", Boolean.class, true);
    }

    private ContainerProperties.AckMode resolveAckMode() {
        String configured = environment.getProperty("kafka.consumer.ack-mode");
        if (!StringUtils.hasText(configured)) {
            return ContainerProperties.AckMode.MANUAL_IMMEDIATE;
        }
        try {
            return ContainerProperties.AckMode.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid Kafka ack mode '{}', falling back to MANUAL_IMMEDIATE", configured);
            return ContainerProperties.AckMode.MANUAL_IMMEDIATE;
        }
    }
}
