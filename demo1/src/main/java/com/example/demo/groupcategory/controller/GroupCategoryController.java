package com.example.demo.groupcategory.controller;

import com.example.demo.common.paging.PageResponse;
import com.example.demo.common.paging.PagingRequest;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.groupcategory.dto.request.GroupCategoryBatchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryRejectReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpsertReq;
import com.example.demo.groupcategory.dto.excel.GroupCatExcelImportResultRes;
import com.example.demo.groupcategory.dto.response.GroupCategoryBatchActionResponse;
import com.example.demo.groupcategory.dto.response.GroupCategoryResponse;
import com.example.demo.groupcategory.service.GroupCategoryExcelService;
import com.example.demo.groupcategory.service.GroupCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jpa/categories")
public class GroupCategoryController {

    private final GroupCategoryService service;
    private final GroupCategoryExcelService groupCategoryExcelService;

    @PreAuthorize("hasAuthority('GC_CREATE') or hasAuthority('GC_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupCategoryResponse> create(@Valid @RequestBody GroupCategoryUpsertReq req) {
        return ApiResponse.success("Create success", GroupCategoryResponse.from(service.create(req)));
    }

    @PreAuthorize("hasAuthority('GC_CREATE') or hasAuthority('GC_SUBMIT') or hasAuthority('GC_ADMIN')")
    @PostMapping("/submit-create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupCategoryResponse> createAndSubmit(@Valid @RequestBody GroupCategoryUpsertReq req) {
        return ApiResponse.success("Create and submit success", GroupCategoryResponse.from(service.createAndSubmit(req)));
    }

    @PreAuthorize("hasAuthority('GC_LIST') or hasAuthority('GC_ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<GroupCategoryResponse>> getAll(PagingRequest pagingRequest) {
        return ApiResponse.success(service.getCategory(pagingRequest));
    }

    @PreAuthorize("hasAuthority('GC_LIST') or hasAuthority('GC_ADMIN')")
    @GetMapping("/{id}")
    public ApiResponse<GroupCategoryResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(GroupCategoryResponse.from(service.getCategoryById(id)));
    }

    @PreAuthorize("hasAuthority('GC_LIST') or hasAuthority('GC_ADMIN')")
    @GetMapping("/component-codes")
    public ApiResponse<List<String>> getComponentCodes(@RequestParam(defaultValue = "1") Integer isActive) {
        return ApiResponse.success("Get component codes success", service.getComponentCodes(isActive));
    }

    @PreAuthorize("hasAuthority('GC_UPDATE') or hasAuthority('GC_ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<GroupCategoryResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody GroupCategoryUpsertReq req) {
        return ApiResponse.success("Update success", GroupCategoryResponse.from(service.update(id, req)));
    }

    @PreAuthorize("hasAuthority('GC_SUBMIT') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/submit")
    public ApiResponse<GroupCategoryResponse> submit(@PathVariable Long id) {
        return ApiResponse.success("Submit success", GroupCategoryResponse.from(service.submit(id)));
    }

    @PreAuthorize("hasAuthority('GC_APPROVE') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/approve")
    public ApiResponse<GroupCategoryResponse> approve(@PathVariable Long id) {
        return ApiResponse.success("Approve success", GroupCategoryResponse.from(service.approve(id)));
    }

    @PreAuthorize("hasAuthority('GC_REJECT') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/reject")
    public ApiResponse<GroupCategoryResponse> reject(@PathVariable Long id,
                                                     @Valid @RequestBody GroupCategoryRejectReq req) {
        return ApiResponse.success("Reject success", GroupCategoryResponse.from(service.reject(id, req.reason())));
    }

    @PreAuthorize("hasAuthority('GC_CANCEL_APPROVE') or hasAuthority('GC_ADMIN')")
    @PostMapping("/{id}/cancel-approve")
    public ApiResponse<GroupCategoryResponse> cancelApprove(@PathVariable Long id) {
        return ApiResponse.success("Cancel approve success", GroupCategoryResponse.from(service.cancelApprove(id)));
    }

    @PreAuthorize("hasAuthority('GC_DELETE') or hasAuthority('GC_ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success("Delete success", "Deleted successfully");
    }

    @PreAuthorize("hasAuthority('GC_DELETE') or hasAuthority('GC_ADMIN')")
    @DeleteMapping("/batch")
    public ApiResponse<GroupCategoryBatchActionResponse> deleteBatch(
            @Valid @RequestBody GroupCategoryBatchReq req
    ) {
        return ApiResponse.success("Delete batch success", service.deleteBatch(req));
    }

    @PreAuthorize("hasAuthority('GC_SEARCH') or hasAuthority('GC_ADMIN')")
    @PostMapping("/search")
    public ApiResponse<PageResponse<GroupCategoryResponse>> search(@RequestBody GroupCategorySearchReq req,
                                                                   PagingRequest pagingRequest) {
        return ApiResponse.success(service.search(req, pagingRequest));
    }

    @PreAuthorize("hasAuthority('GC_LIST') or hasAuthority('GC_SEARCH') or hasAuthority('GC_ADMIN')")
    @PostMapping("/export")
    public ResponseEntity<ByteArrayResource> exportExcel(@RequestBody(required = false) GroupCategorySearchReq req) {
        byte[] content = groupCategoryExcelService.exportExcel(req);
        String filename = "group-category-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    @PreAuthorize("hasAuthority('GC_IMPORT') or hasAuthority('GC_CREATE') or hasAuthority('GC_SUBMIT') or hasAuthority('GC_ADMIN')")
    @GetMapping("/download-template")
    public ResponseEntity<Resource> downloadTemplate() {

        ByteArrayResource resource = groupCategoryExcelService.dowloadTemplate();
        long contentLength = resource == null ? 0 : resource.contentLength();

        if (contentLength <= 0) {
            throw new BusinessException(ErrorCode.GC_CREATE_FAILED, "Template category trả về rỗng");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_category.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(contentLength)
                .body(resource);
    }

    @PreAuthorize("hasAuthority('GC_CREATE') or hasAuthority('GC_SUBMIT') or hasAuthority('GC_ADMIN')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GroupCatExcelImportResultRes> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean submitAfterImport
    ) {
        return ApiResponse.success(
                "Import excel success",
                groupCategoryExcelService.importExcel(file, submitAfterImport)
        );
    }

    @PreAuthorize("hasAuthority('GC_SUBMIT') or hasAuthority('GC_ADMIN')")
    @PostMapping("/submit-batch")
    public ApiResponse<GroupCategoryBatchActionResponse> submitBatch(
            @Valid @RequestBody GroupCategoryBatchReq req
    ) {
        return ApiResponse.success("Submit batch success", service.submitBatch(req));
    }

    @PreAuthorize("hasAuthority('GC_APPROVE') or hasAuthority('GC_ADMIN')")
    @PostMapping("/approve-batch")
    public ApiResponse<GroupCategoryBatchActionResponse> approveBatch(
            @Valid @RequestBody GroupCategoryBatchReq req
    ) {
        return ApiResponse.success("Approve batch success", service.approveBatch(req));
    }

    @PreAuthorize("hasAuthority('GC_CANCEL_APPROVE') or hasAuthority('GC_ADMIN')")
    @PostMapping("/batch-cancel-approve")
    public ApiResponse<GroupCategoryBatchActionResponse> batchCancelApprove(
            @Valid @RequestBody GroupCategoryBatchReq req
    ) {
        return ApiResponse.success("Batch cancel approve success", service.batchCancelApprove(req));
    }
}
