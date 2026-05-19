package com.example.demo.groupcategory.controller;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.paging.PageResponse;
import com.example.demo.groupcategory.dto.request.GroupCategoryActionReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryCreateReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpdateReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import com.example.demo.groupcategory.service.GroupCategoryPrcService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/prc/categories")
@RequiredArgsConstructor
public class GroupCategoryPRCController {

    private final GroupCategoryPrcService service;

    @PreAuthorize("hasAuthority('GC_CREATE') or hasAuthority('GC_ADMIN')")
    @PostMapping
    public ApiResponse<Long> create(@RequestBody GroupCategoryCreateReq req){
        return ApiResponse.success(service.create(req));
    }

    @PreAuthorize("hasAuthority('GC_LIST') or hasAuthority('GC_ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<GroupCategory>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return ApiResponse.success(service.getAll(page, size));
    }

    @PreAuthorize("hasAuthority('GC_LIST') or hasAuthority('GC_ADMIN')")
    @GetMapping("/{id}")
    public ApiResponse<GroupCategory> getById(@PathVariable Long id){
        return ApiResponse.success(service.getById(id));
    }

    @PreAuthorize("hasAuthority('GC_UPDATE') or hasAuthority('GC_ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<Long> update(@PathVariable Long id, @RequestBody GroupCategoryUpdateReq req) {
        return ApiResponse.success(service.update(id, req));
    }

    @PreAuthorize("hasAuthority('GC_DELETE') or hasAuthority('GC_ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success("Deleted successfully", "OK");
    }

    @PreAuthorize("hasAuthority('GC_SEARCH') or hasAuthority('GC_ADMIN')")
    @PostMapping("/search")
    public ApiResponse<PageResponse<GroupCategory>> search(@RequestBody GroupCategorySearchReq req) {
        return ApiResponse.success(service.search(req));
    }

    @PreAuthorize("hasAuthority('GC_SUBMIT') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/submit")
    public ApiResponse<Long> submit(@PathVariable Long id, @RequestBody GroupCategoryActionReq req){
        return ApiResponse.success("Submit success", service.submit(id, req));
    }


    @PreAuthorize("hasAuthority('GC_APPROVE') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/approve")
    public ApiResponse<Long> approve(@PathVariable Long id, @RequestBody GroupCategoryActionReq req){
        return ApiResponse.success("Approve success", service.approve(id, req));
    }

    @PreAuthorize("hasAuthority('GC_REJECT') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/reject")
    public ApiResponse<Long> reject(@PathVariable Long id, @RequestBody GroupCategoryActionReq req){
        return ApiResponse.success("Reject success", service.reject(id, req));
    }
}
