package com.example.demo.groupcategory.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GroupCategoryBatchReq(
        @NotEmpty(message = "ids không được để trống")
        List<Long> ids
) {
}
