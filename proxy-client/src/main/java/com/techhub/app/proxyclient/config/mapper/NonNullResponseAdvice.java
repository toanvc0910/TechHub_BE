package com.techhub.app.proxyclient.config.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class NonNullResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(SkipNulls.class)
                && MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
//        return returnType.getContainingClass().equals(PromotionController.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (MappingJackson2HttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            ObjectMapper nonNullMapper = new JsonMapper()
                    .registerModule(new JavaTimeModule())
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);

            // Option 1:
            // Convert your original `body` into a plain Map/List structure with all nulls already removed:
            // Return that Map/List — Spring’s normal converter will serialize it to real JSON.
            return nonNullMapper.convertValue(body, Object.class);

//            // Option 2:
//            // Write raw JSON bytes and then tell Spring “I've handled it”:
//            try {
//                ServletServerHttpResponse servletResp = (ServletServerHttpResponse) response;
//                nonNullMapper.writeValue(servletResp.getServletResponse().getOutputStream(), body);
//                return null;  // Spring won’t try to serialize again
//            } catch (IOException e) {
//                throw new HttpMessageNotWritableException("JSON serialization error", e);
//            }

        }
        return body;
    }

}