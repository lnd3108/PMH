package com.example.demo.groupcategory.service.Impl;

import com.example.demo.groupcategory.constant.GroupCategoryConstant;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.common.paging.PageResponse;
import com.example.demo.common.paging.PagingRequest;
import com.example.demo.common.paging.PagingUtils;
import com.example.demo.groupcategory.dto.request.GroupCategoryBatchReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpsertReq;
import com.example.demo.groupcategory.dto.response.GroupCategoryBatchActionResponse;
import com.example.demo.groupcategory.dto.response.GroupCategoryBatchError;
import com.example.demo.groupcategory.dto.response.GroupCategoryResponse;
import com.example.demo.groupcategory.dto.response.GroupCategoryStatusOnlyResponse;
import com.example.demo.groupcategory.entity.GroupCategory;
import com.example.demo.groupcategory.mapper.GroupCategoryMapper;
import com.example.demo.groupcategory.repository.GroupCategoryRepository;
import com.example.demo.groupcategory.service.GroupCategoryService;
import com.example.demo.groupcategory.service.helper.GroupCategoryNewDataHelper;
import com.example.demo.groupcategory.service.validator.GroupCategoryValidator;
import com.example.demo.groupcategory.repository.specification.GroupCategorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ImplGroupCategoryService implements GroupCategoryService {

    private final GroupCategoryRepository repository;
    private final GroupCategoryValidator validator;
    private final GroupCategoryMapper mapper;
    private final GroupCategoryNewDataHelper newDataHelper;

    public GroupCategory create(GroupCategoryUpsertReq req) {

        validator.validateRequired(req);
        validator.validateDuplicateForUpsert(req, null);

        GroupCategory entity = new GroupCategory();
        //gắn trường đã chuẩn hóa vào new
        mapper.applyBaseFields(entity, req);

        entity.markAsDraft();
        entity.applyDefaultFlags(req.isActive(), GroupCategoryConstant.DISPLAY_HIDDEN);
        //để display về trạng thái chưa duyệt có thể xóa
        entity.replaceNewData("{}");
        return repository.save(entity);
    }

    public GroupCategory createAndSubmit(GroupCategoryUpsertReq req) {
        validator.validateRequired(req);
        validator.validateDuplicateForUpsert(req, null);

        GroupCategory entity = new GroupCategory();
        mapper.applyBaseFields(entity, req);

        entity.markAsPending();
        entity.applyDefaultFlags(req.isActive(), GroupCategoryConstant.DISPLAY_HIDDEN);
        entity.replaceNewData("{}");

        return repository.save(entity);
    }

    public GroupCategory update(Long id, GroupCategoryUpsertReq req) {
        validator.validateRequired(req);

        GroupCategory current = getRequired(id);

        Integer status = current.getStatus();

        if (current.isPending()) {
            throw new BusinessException(ErrorCode.GC_ALREADY_PENDING);
        }

        boolean canStoreChangeInNewData =
                Objects.equals(status, GroupCategoryConstant.STATUS_DRAFT)
                        || Objects.equals(status, GroupCategoryConstant.STATUS_REJECTED)
                        || Objects.equals(status, GroupCategoryConstant.STATUS_CANCEL_APPROVE);

        if (canStoreChangeInNewData) {
            GroupCategory preview = mapper.buildPreviewEntity(current, req);
            validator.validateDuplicateForEntity(preview, current.getId());

            String patchJson = newDataHelper.buildPatchJson(current, req);

            if (!newDataHelper.hasMeaningfulNewData(patchJson)) {
                throw new BusinessException(ErrorCode.GC_NO_CHANGES);
            }

            current.replaceNewData(patchJson);

            return repository.save(current);
        }

        throw new BusinessException(ErrorCode.INVALID_STATE, "Trạng thái hiện tại không cho phép sửa");
    }

    public GroupCategory submit(Long id) {
        GroupCategory current = getRequired(id);

        if (current.isPending()) {
            throw new BusinessException(ErrorCode.GC_ALREADY_PENDING);
        }

        if (newDataHelper.hasMeaningfulNewData(current.getNewData())) {
            GroupCategory preview = mapper.cloneEntity(current);
            newDataHelper.applyPatchJson(preview, current.getNewData());

            validator.validateRequiredEntity(preview);
            validator.validateDuplicateForEntity(preview, current.getId());

            current.markAsPending();

            return repository.save(current);
        }

        validator.validateRequiredEntity(current);
        validator.validateDuplicateForEntity(current, current.getId());

        current.markAsPending();

        return repository.save(current);
    }

    public GroupCategory approve(Long id) {
        GroupCategory current = getRequired(id);

        if (!current.isPending()) {
            throw new BusinessException(ErrorCode.GC_ONLY_PENDING_CAN_APPROVE);
        }

        if (newDataHelper.hasMeaningfulNewData(current.getNewData())) {
            newDataHelper.applyPatchJson(current, current.getNewData());
        }

        validator.validateRequiredEntity(current);
        validator.validateDuplicateForEntity(current, current.getId());

        current.markAsApproved();
        current.show();
        current.clearNewData();

        return repository.save(current);
    }

    public GroupCategory reject(Long id, String reason) {
        GroupCategory current = getRequired(id);

        if (!current.isPending()) {
            throw new BusinessException(ErrorCode.GC_ONLY_PENDING_CAN_REJECT);
        }

        current.markAsRejected();
        current.clearNewData();

        // Nếu rejected vẫn cần hiển thị trên màn quản lý thì không gọi hide()
        // Nếu rejected cần ẩn khỏi màn nghiệp vụ khác thì mới gọi hide()
        current.hide();

        return repository.save(current);
    }

    public GroupCategory cancelApprove(Long id) {
        GroupCategory current = getRequired(id);

        if (!Objects.equals(current.getStatus(), GroupCategoryConstant.STATUS_APPROVED)) {
            throw new BusinessException(ErrorCode.GC_ONLY_APPROVED_CAN_CANCEL);
        }

        current.markAsCancelApproved();
        current.hide();
        current.clearNewData();

        return repository.save(current);
    }

    public void delete(Long id) {
        GroupCategory current = getRequired(id);

        Integer status = current.getStatus();

        if (Objects.equals(status, GroupCategoryConstant.STATUS_PENDING)) {
            throw new BusinessException(ErrorCode.GC_PENDING_CANNOT_DELETE);
        }

        if (Objects.equals(status, GroupCategoryConstant.STATUS_APPROVED)
                || Objects.equals(status, GroupCategoryConstant.STATUS_CANCEL_APPROVE)) {
            throw new BusinessException(ErrorCode.GC_APPROVED_CANNOT_DELETE);
        }

        if (!Objects.equals(status, GroupCategoryConstant.STATUS_DRAFT)
                && !Objects.equals(status, GroupCategoryConstant.STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Trạng thái hiện tại không cho phép xóa");
        }

        if (!Objects.equals(current.getIsDisplay(), GroupCategoryConstant.DISPLAY_HIDDEN)) {
            throw new BusinessException(ErrorCode.GC_APPROVED_CANNOT_DELETE);
        }

        if (newDataHelper.hasMeaningfulNewData(current.getNewData())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Bản ghi đã có dữ liệu thay đổi, không được xóa");
        }

        repository.delete(current);
    }

    public PageResponse<GroupCategoryResponse> getCategory(PagingRequest pagingRequest) {
        Pageable pageable = PagingUtils.toPageable(pagingRequest);
        Page<GroupCategory> pageData = repository.findAll(pageable);
        return PageResponse.from(pageData, GroupCategoryResponse::from);
    }

    public PageResponse<GroupCategoryResponse> search(GroupCategorySearchReq req, PagingRequest pagingRequest) {
        Pageable pageable = PagingUtils.toPageable(pagingRequest);
        Page<GroupCategory> pageData = repository.findAll(GroupCategorySpecification.search(req), pageable);
        return PageResponse.from(pageData, GroupCategoryResponse::from);
    }

    public GroupCategoryBatchActionResponse submitBatch(GroupCategoryBatchReq req) {
        return processBatch(req, this::submit);
    }

    public GroupCategoryBatchActionResponse approveBatch(GroupCategoryBatchReq req) {
        return processBatch(req, this::approve);
    }

    public GroupCategoryBatchActionResponse batchCancelApprove(GroupCategoryBatchReq req) {
        return processBatch(req, this::cancelApprove);
    }

    public GroupCategoryBatchActionResponse deleteBatch(GroupCategoryBatchReq req) {
        return processBatch(req, this::deleteOneForBatch);
    }

    private GroupCategoryBatchActionResponse processBatch(
            GroupCategoryBatchReq req,
            BatchActionExecutor executor
    ) {
        Set<Long> uniqueIds = new LinkedHashSet<>(req.ids());
        if (uniqueIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "ids không được để trống");
        }

        List<GroupCategoryStatusOnlyResponse> updated = new ArrayList<>();
        List<GroupCategoryBatchError> failed = new ArrayList<>();

        for (Long id : uniqueIds) {
            if (id == null) {
                failed.add(new GroupCategoryBatchError(null, ErrorCode.INVALID_REQUEST.getCode(), "id không hợp lệ"));
                continue;
            }

            try {
                GroupCategory entity = executor.execute(id);
                updated.add(GroupCategoryStatusOnlyResponse.from(entity));
            } catch (BusinessException ex) {
                failed.add(new GroupCategoryBatchError(id, ex.getCode(), ex.getMessage()));
            } catch (Exception ex) {
                failed.add(new GroupCategoryBatchError(id, ErrorCode.INTERNAL_ERROR.getCode(), "Lỗi hệ thống"));
            }
        }

        return new GroupCategoryBatchActionResponse(
                uniqueIds.size(),
                updated.size(),
                failed.size(),
                updated,
                failed
        );
    }

    @FunctionalInterface
    private interface BatchActionExecutor {
        GroupCategory execute(Long id);
    }

    private GroupCategory deleteOneForBatch(Long id) {
        GroupCategory current = getRequired(id);
        GroupCategory deletedSnapshot = new GroupCategory(
                current.getId(),
                current.getParamName(),
                current.getParamValue(),
                current.getParamType(),
                current.getDescription(),
                current.getComponentCode(),
                current.getStatus(),
                current.getIsActive(),
                current.getIsDisplay(),
                current.getNewData(),
                current.getEffectiveDate(),
                current.getEndEffectiveDate()
        );
        delete(id);
        return deletedSnapshot;
    }

    public GroupCategory getCategoryById(Long id) {
        return getRequired(id);
    }

    public List<String> getComponentCodes(Integer isActive) {
        Integer effectiveIsActive = isActive == null ? GroupCategoryConstant.ACTIVE_DEFAULT : isActive;
        return repository.findDistinctComponentCodesByIsActive(effectiveIsActive);
    }

    private GroupCategory getRequired(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.GC_NOT_FOUND,
                        "Không tìm thấy bản ghi với id = " + id
                ));
    }

}
