package com.example.demo.groupcategory.service;

import com.example.demo.common.paging.PageResponse;
import com.example.demo.groupcategory.dto.request.GroupCategoryActionReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryCreateReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpdateReq;
import com.example.demo.groupcategory.entity.GroupCategory;

public interface GroupCategoryPrcService {
    Long create(GroupCategoryCreateReq req);
    PageResponse<GroupCategory> getAll(int page, int size);
    GroupCategory getById(Long id);
    Long update(Long id, GroupCategoryUpdateReq req);
    void delete(Long id);
    Long submit(Long id, GroupCategoryActionReq req);
    Long approve(Long id, GroupCategoryActionReq req);
    Long reject(Long id, GroupCategoryActionReq req);
    PageResponse<GroupCategory> search(GroupCategorySearchReq req);



}
