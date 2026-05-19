package com.example.demo.common.paging;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class PagingRequest {
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private  String sortDir = "desc";

    public int getSafePage(){
        return page == null || page < 0 ? 0 : page;
    }

    public int getSafeSize(){
        if(size == null || size <= 0) return 10;
        return Math.min(size, 100);
    }

    public String getSafeSortBy(){
        return (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
    }

    public Sort.Direction getSafeSortDir(){
        return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}
