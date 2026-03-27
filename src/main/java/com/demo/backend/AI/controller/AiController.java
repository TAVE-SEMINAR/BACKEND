package com.demo.backend.AI.controller;

import com.demo.backend.AI.dto.response.AIPredictResponse;
import com.demo.backend.AI.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI", description = "AI 예측 API")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AIService aiService;

    @Operation(summary = "이미지 예측", description = "이미지 파일을 업로드하면 AI 모델의 예측 결과를 반환합니다.")
    @PostMapping(value = "/predict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AIPredictResponse predict(
            @Parameter(description = "예측할 이미지 파일", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file) {
        return aiService.predict(file);
    }
}
