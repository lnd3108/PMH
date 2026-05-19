package com.example.demo.groupcategory.dto.request;

import java.util.List;

public record GroupCategorySearchReq(
        String paramName,
        String paramValue,
        String paramType,
        List<Integer> status,
        List<Integer> isActive,
        Integer page,
        Integer size,
        String sortBy,
        String sortDir
){
}
