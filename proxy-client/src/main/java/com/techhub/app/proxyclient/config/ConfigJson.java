package com.techhub.app.proxyclient.config;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigJson {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer filterCustomizer() {
        return builder -> {
            builder.filters(new SimpleFilterProvider()
                    .addFilter("employeeDto", SimpleBeanPropertyFilter.serializeAll())); // serialize tất cả nếu không filter
        };
    }
}