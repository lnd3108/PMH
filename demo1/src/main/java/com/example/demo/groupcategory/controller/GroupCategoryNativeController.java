package com.example.demo.groupcategory.controller;

import com.example.demo.common.paging.PageResponse;
import com.example.demo.groupcategory.dto.request.GroupCategoryActionReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryCreateReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpdateReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import com.example.demo.groupcategory.service.GroupCategoryNativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/native/categories")
public class GroupCategoryNativeController {

    private final GroupCategoryNativeService service;

    @PreAuthorize("hasAuthority('GC_CREATE') or hasAuthority('GC_ADMIN')")
    @PostMapping
    public Long create(@RequestBody GroupCategoryCreateReq req) {
        return service.create(req);
    }

    @PreAuthorize("hasAuthority('GC_LIST') or hasAuthority('GC_ADMIN')")
    @GetMapping
    public PageResponse<GroupCategory> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getAll(page, size);
    }

    @PreAuthorize("hasAuthority('GC_LIST') or hasAuthority('GC_ADMIN')")
    @GetMapping("/{id}")
    public GroupCategory getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PreAuthorize("hasAuthority('GC_UPDATE') or hasAuthority('GC_ADMIN')")
    @PutMapping("/{id}")
    public Long update(@PathVariable Long id, @RequestBody GroupCategoryUpdateReq req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasAuthority('GC_DELETE') or hasAuthority('GC_ADMIN')")
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Deleted successfully";
    }

    @PreAuthorize("hasAuthority('GC_SEARCH') or hasAuthority('GC_ADMIN')")
    @PostMapping("/search")
    public PageResponse<GroupCategory> search(
            @RequestBody GroupCategorySearchReq req,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.search(req, page, size);
    }

    @PreAuthorize("hasAuthority('GC_SUBMIT') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/submit")
    public Long submit(@PathVariable Long id, @RequestBody GroupCategoryActionReq req) {
        return service.submit(id, req);
    }

    @PreAuthorize("hasAuthority('GC_APPROVE') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/approve")
    public Long approve(@PathVariable Long id, @RequestBody GroupCategoryActionReq req) {
        return service.approve(id, req);
    }

    @PreAuthorize("hasAuthority('GC_REJECT') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/reject")
    public Long reject(@PathVariable Long id, @RequestBody GroupCategoryActionReq req) {
        return service.reject(id, req);
    }
}
