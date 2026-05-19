package com.example.demo.groupcategory.service.Impl;

import com.example.demo.groupcategory.constant.GroupCategoryConstant;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.common.paging.PageResponse;
import com.example.demo.groupcategory.dto.request.GroupCategoryActionReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryCreateReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpdateReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import com.example.demo.groupcategory.mapper.GroupCategoryMapper;
import com.example.demo.groupcategory.repository.GroupCategoryNativeRepository;
import com.example.demo.groupcategory.service.GroupCategoryNativeService;
import com.example.demo.groupcategory.service.helper.GroupCategoryNewDataHelper;
import com.example.demo.groupcategory.service.validator.GroupCategoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class ImplGroupCategoryNativeService implements GroupCategoryNativeService {

    private final GroupCategoryNativeRepository repository;
    private final GroupCategoryValidator validator;
    private final GroupCategoryMapper mapper;
    private final GroupCategoryNewDataHelper newDataHelper;

    public Long create(GroupCategoryCreateReq req) {
        validateCreateRequest(req);
        return repository.create(req);
    }

    public PageResponse<GroupCategory> getAll(int page, int size) {
        return repository.getAll(page, size);
    }

    public GroupCategory getById(Long id) {
        validateId(id);
        return repository.getById(id);
    }

    public Long update(Long id, GroupCategoryUpdateReq req) {
        validateId(id);
        validateUpdateRequest(req);
        return repository.update(id, req);
    }

    public void delete(Long id) {
        validateId(id);
        repository.delete(id);
    }

    public PageResponse<GroupCategory> search(GroupCategorySearchReq req, int page, int size) {
        return repository.search(req, page, size);
    }

    public Long submit(Long id, GroupCategoryActionReq req) {
        validateId(id);
        validateActionRequest(req);

        GroupCategory current = repository.getById(id);
        if (current.isPending()) {
            throw new BusinessException(ErrorCode.GC_ALREADY_PENDING);
        }

        if (current.isPublished()) {
            if (!newDataHelper.hasMeaningfulNewData(current.getNewData())) {
                throw new BusinessException(ErrorCode.GC_NO_CHANGES, "Khong co du lieu thay doi de gui duyet");
            }

            GroupCategory preview = mapper.cloneEntity(current);
            newDataHelper.applyPatchJson(preview, current.getNewData());
            validator.validateRequiredEntity(preview);
            validator.validateDuplicateForEntity(preview, current.getId());

            repository.updateWorkflowState(
                    id,
                    GroupCategoryConstant.STATUS_PENDING,
                    current.getIsDisplay(),
                    current.getNewData()
            );
            return id;
        }

        validator.validateRequiredEntity(current);
        validator.validateDuplicateForEntity(current, current.getId());

        repository.updateWorkflowState(
                id,
                GroupCategoryConstant.STATUS_PENDING,
                GroupCategoryConstant.DISPLAY_HIDDEN,
                current.getNewData()
        );
        return id;
    }

    public Long approve(Long id, GroupCategoryActionReq req) {
        validateId(id);
        validateActionRequest(req);

        GroupCategory current = repository.getById(id);
        if (!current.isPending()) {
            throw new BusinessException(ErrorCode.GC_ONLY_PENDING_CAN_APPROVE);
        }

        if (newDataHelper.hasMeaningfulNewData(current.getNewData())) {
            GroupCategory approved = mapper.cloneEntity(current);
            newDataHelper.applyPatchJson(approved, current.getNewData());
            validator.validateRequiredEntity(approved);
            validator.validateDuplicateForEntity(approved, current.getId());

            approved.markAsApproved();
            approved.show();
            approved.clearNewData();
            repository.applyApprovedData(approved);
            return id;
        }

        repository.updateWorkflowState(
                id,
                GroupCategoryConstant.STATUS_APPROVED,
                GroupCategoryConstant.DISPLAY_VISIBLE,
                null
        );
        return id;
    }

    public Long reject(Long id, GroupCategoryActionReq req) {
        validateId(id);
        validateActionRequest(req);

        GroupCategory current = repository.getById(id);
        if (!current.isPending()) {
            throw new BusinessException(ErrorCode.GC_ONLY_PENDING_CAN_REJECT);
        }

        if (newDataHelper.hasMeaningfulNewData(current.getNewData())) {
            repository.updateWorkflowState(
                    id,
                    GroupCategoryConstant.STATUS_APPROVED,
                    GroupCategoryConstant.DISPLAY_VISIBLE,
                    null
            );
            return id;
        }

        repository.updateWorkflowState(
                id,
                GroupCategoryConstant.STATUS_REJECTED,
                GroupCategoryConstant.DISPLAY_HIDDEN,
                current.getNewData()
        );
        return id;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.GC_INVALID_ID);
        }
    }

    private void validateActionRequest(GroupCategoryActionReq req) {
        if (req == null || req.actor() == null || req.actor().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "actor khong duoc de trong");
        }
    }

    private void validateCreateRequest(GroupCategoryCreateReq req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        validateRequiredFields(req.paramName(), req.paramValue(), req.paramType(), req.effectiveDate(), req.endEffectiveDate());
    }

    private void validateUpdateRequest(GroupCategoryUpdateReq req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        validateRequiredFields(req.paramName(), req.paramValue(), req.paramType(), req.effectiveDate(), req.endEffectiveDate());
    }

    private void validateRequiredFields(
            String paramName,
            String paramValue,
            String paramType,
            LocalDate effectiveDate,
            LocalDate endEffectiveDate
    ) {
        if (paramName == null || paramName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramName khong duoc de trong");
        }
        if (paramValue == null || paramValue.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramValue khong duoc de trong");
        }
        if (paramType == null || paramType.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramType khong duoc de trong");
        }
        if (effectiveDate == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "effectiveDate khong duoc de trong");
        }
        if (endEffectiveDate != null && endEffectiveDate.isBefore(effectiveDate)) {
            throw new BusinessException(ErrorCode.GC_INVALID_DATE_RANGE);
        }
    }

}
