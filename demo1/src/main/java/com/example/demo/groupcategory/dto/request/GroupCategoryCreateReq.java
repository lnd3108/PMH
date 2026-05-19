package com.example.demo.groupcategory.dto.request;

import java.time.LocalDate;

public record GroupCategoryCreateReq(
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
        LocalDate endEffectiveDate){}




