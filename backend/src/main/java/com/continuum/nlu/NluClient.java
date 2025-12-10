package com.continuum.nlu;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NluClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;

    public static class IntentRequest {
        public String text;
    }

    public static class IntentResponse {
        public String type;
    }

    public NluClient(@Value("${nlu.base-url:http://localhost:8090}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String classifyIntent(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            IntentRequest req = new IntentRequest();
            req.text = text;

            IntentResponse resp = restTemplate.postForObject(
                    baseUrl + "/classify-intent",
                    req,
                    IntentResponse.class);

            if (resp == null || resp.type == null || resp.type.isBlank()) {
                return null;
            }
            return resp.type;
        } catch (Exception e) {
            // For now, fail soft and let the caller fall back to a default type.
            return null;
        }
    }
}
