package com.example.demo.groupcategory.service;

import com.example.demo.common.paging.PageResponse;
import com.example.demo.common.paging.PagingRequest;
import com.example.demo.groupcategory.dto.request.GroupCategoryBatchReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpsertReq;
import com.example.demo.groupcategory.dto.response.GroupCategoryBatchActionResponse;
import com.example.demo.groupcategory.dto.response.GroupCategoryResponse;
import com.example.demo.groupcategory.entity.GroupCategory;

import java.util.List;

public interface GroupCategoryService {
    GroupCategory create(GroupCategoryUpsertReq req);
    GroupCategory createAndSubmit(GroupCategoryUpsertReq req);
    GroupCategory update(Long id, GroupCategoryUpsertReq req);
    GroupCategory submit(Long id);
    GroupCategory approve(Long id);
    GroupCategory reject(Long id, String reason);
    GroupCategory cancelApprove(Long id);
    void delete(Long id);
    PageResponse<GroupCategoryResponse> getCategory(PagingRequest pagingRequest);
    PageResponse<GroupCategoryResponse> search(GroupCategorySearchReq req, PagingRequest pagingRequest);
    GroupCategoryBatchActionResponse submitBatch(GroupCategoryBatchReq req);
    GroupCategoryBatchActionResponse approveBatch(GroupCategoryBatchReq req);
    GroupCategoryBatchActionResponse batchCancelApprove(GroupCategoryBatchReq req);
    GroupCategoryBatchActionResponse deleteBatch(GroupCategoryBatchReq req);
    GroupCategory getCategoryById(Long id);
    List<String> getComponentCodes(Integer isActive);
}
