package com.example.demo.groupcategory.dto.excel;

import java.time.LocalDate;

public record GroupCategoryExcelRowReq(
        int rowNumber,
        String paramName,
        String paramValue,
        String paramType,
        String description,
        String componentCode,
        Integer isActive,
        Integer isDisplay,
        LocalDate effectiveDate,
        LocalDate endEffectiveDate
) {}