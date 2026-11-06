package com.example.appbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearningProjectCompletionRequest {

    @NotBlank(message = "请填写完成说明")
    @Size(max = 500, message = "完成说明最多500字符")
    private String evidence;
}
