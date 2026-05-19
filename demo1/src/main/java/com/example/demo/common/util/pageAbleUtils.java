package com.example.demo.common.util;

import com.example.demo.common.response.ApiResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.Set;

public class pageAbleUtils {
    public static Pageable sanitize (Pageable pageable, Set<String> allowedSortFields, String defaultSortField){
        if(pageable.getSort().isUnsorted()){
            return org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, defaultSortField)
            );
        }

        Sort.Order firstOrder = pageable.getSort().iterator().next();
        String property = firstOrder.getProperty();

        if(!allowedSortFields.contains(property)){
            return org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, defaultSortField)
            );
        }

        return pageable;
    }public static <T> ResponseEntity<ApiResponse<T>> createResponseEntity(ApiResponse<T> apiResponse) {
        return new ResponseEntity<>(apiResponse, apiResponse.getStatus());
    }
}
