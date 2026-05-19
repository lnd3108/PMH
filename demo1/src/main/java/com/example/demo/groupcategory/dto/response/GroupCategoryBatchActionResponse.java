package com.example.demo.groupcategory.dto.response;

import java.util.List;

public record GroupCategoryBatchActionResponse(
        int total,
        int successCount,
        int failedCount,
        List<GroupCategoryStatusOnlyResponse> updated,
        List<GroupCategoryBatchError> failed
) {
}