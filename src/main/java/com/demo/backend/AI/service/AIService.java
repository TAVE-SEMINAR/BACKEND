package com.demo.backend.AI.service;

import com.demo.backend.AI.dto.response.AIPredictResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.ByteArrayResource;

@Service
@RequiredArgsConstructor
public class AIService {

    private final WebClient webClient;

    public AIPredictResponse predict(MultipartFile file) {

        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("file", resource);

            return webClient.post()
                    .uri("http://localhost:8000/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(AIPredictResponse.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("FastAPI 호출 실패", e);
        }
    }
}