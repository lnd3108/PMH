package com.example.demo.common.paging;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PagingUtils {

    private PagingUtils(){
    }

    public static Pageable toPageable(PagingRequest req){
        return PageRequest.of(
                req.getSafePage(),
                req.getSafeSize(),
                Sort.by(req.getSafeSortDir(), req.getSafeSortBy())
        );
    }
}
