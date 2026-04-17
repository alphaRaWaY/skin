package org.skinAI.client;

import org.skinAI.pojo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.ai.base-url:http://127.0.0.1:8082}")
    private String aiBaseUrl;

    public AiServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAdvice(String prompt) {
        Result<?> result = restTemplate.postForObject(
                aiBaseUrl + "/api/deepseek/advice",
                prompt,
                Result.class
        );
        if (result == null || result.getCode() == null || result.getCode() != 0 || result.getResult() == null) {
            throw new RuntimeException("AI service response invalid");
        }
        return String.valueOf(result.getResult());
    }
}
