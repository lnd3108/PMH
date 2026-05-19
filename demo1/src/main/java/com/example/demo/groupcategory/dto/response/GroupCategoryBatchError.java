package com.example.demo.groupcategory.dto.response;

public record GroupCategoryBatchError(
        Long id,
        String code,
        String message
) {
}
