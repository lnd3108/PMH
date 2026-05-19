package com.example.demo.groupcategory.dto.excel;

import java.util.List;

public record GroupCatExcelImportResultRes(
        int totalRows,
        int successRows,
        int failedRows,
        List<GroupCatExcelImportErrorRes> errors
) {
}
