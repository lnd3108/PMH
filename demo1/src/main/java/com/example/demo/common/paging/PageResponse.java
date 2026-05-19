package com.example.demo.common.paging;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Builder
public class PageResponse<T> {
    private List<T> content;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    private boolean first;
    private boolean last;
    private boolean empty;

    private String sortBy;
    private String sortDir;

    public  static <E, R> PageResponse<R> from(Page<E> pageData, Function<E, R> mapper){
        String sortBy = null;
        String sortDir = null;


        if(pageData.getSort().isSorted()){
            Sort.Order order = pageData.getSort().iterator().next();
            sortBy = order.getProperty();
            sortDir = order.getDirection().name().toLowerCase();
        }

        return PageResponse.<R> builder()
                .content(pageData.getContent().stream().map(mapper).collect(Collectors.toList()))
                .page(pageData.getNumber())
                .size(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .first(pageData.isFirst())
                .last(pageData.isLast())
                .empty(pageData.isEmpty())
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
    }

    public static <E, R> PageResponse<R> fromNative(
            List<E> data,
            int page,
            int size,
            long totalElements,
            String sortBy,
            String sortDir,
            Function<E, R> mapper
    ){
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        return PageResponse.<R>builder()
                .content(data.stream().map(mapper).collect(Collectors.toList()))
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(totalPages == 0 || page >= totalPages - 1)
                .empty(data == null || data.isEmpty())
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
    }
}
