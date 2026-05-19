package com.example.demo.groupcategory.dto.response;

import com.example.demo.groupcategory.entity.GroupCategory;

public record GroupCategoryStatusOnlyResponse(
        Long id,
        Integer status,
        Integer isDisplay,
        String newData
) {
    public static GroupCategoryStatusOnlyResponse from(GroupCategory entity) {
        return new GroupCategoryStatusOnlyResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getIsDisplay(),
                entity.getNewData()
        );
    }
}