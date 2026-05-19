package com.example.demo.groupcategory.dto.response;

import com.example.demo.groupcategory.entity.GroupCategory;

import java.time.LocalDate;

public record GroupCategoryResponse(
        Long id,
        String paramName,
        String paramValue,
        String paramType,
        String description,
        String componentCode,
        Integer status,
        Integer isActive,
        Integer isDisplay,
        String newData,
        LocalDate effectiveDate,
        LocalDate endEffectiveDate
) {
    public static GroupCategoryResponse from(GroupCategory e) {
        return new GroupCategoryResponse(
                e.getId(),
                e.getParamName(),
                e.getParamValue(),
                e.getParamType(),
                e.getDescription(),
                e.getComponentCode(),
                e.getStatus(),
                e.getIsActive(),
                e.getIsDisplay(),
                e.getNewData(),
                e.getEffectiveDate(),
                e.getEndEffectiveDate()
        );
    }
}