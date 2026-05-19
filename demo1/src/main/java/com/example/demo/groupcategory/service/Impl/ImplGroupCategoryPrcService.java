package com.example.demo.groupcategory.service.Impl;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.common.paging.PageResponse;
import com.example.demo.groupcategory.dto.request.GroupCategoryActionReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryCreateReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpdateReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import com.example.demo.groupcategory.repository.GroupCategoryProcedureRepo;
import com.example.demo.groupcategory.service.GroupCategoryPrcService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ImplGroupCategoryPrcService implements GroupCategoryPrcService {

    private final GroupCategoryProcedureRepo repository;

    public Long create(GroupCategoryCreateReq req){
        validateCreate(req);
        return repository.create(req);
    }

    public PageResponse<GroupCategory> getAll(int page, int size){
        return repository.getAll(page, size);
    }

    public GroupCategory getById(Long id){
        validateId(id);
        return repository.getById(id);
    }

    public Long update(Long id, GroupCategoryUpdateReq req){
        validateId(id);
        validateUpdate(req);
        return repository.update(id, req);
    }

    public void delete(Long id){
        validateId(id);
        repository.delete(id);
    }

    public Long submit(Long id, GroupCategoryActionReq req){
        validateId(id);
        validateActionRequest(req);
        return repository.action(id, "SUBMIT", req);
    }

    public Long approve(Long id, GroupCategoryActionReq req){
        validateId(id);
        validateActionRequest(req);
        return repository.action(id, "APPROVE", req);
    }

    public Long reject(Long id, GroupCategoryActionReq req){
        validateId(id);
        validateActionRequest(req);
        return repository.action(id, "REJECT", req);
    }

    public PageResponse<GroupCategory> search(GroupCategorySearchReq req) {
        return repository.search(req);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Id không hợp lệ");
        }
    }

    private void validateCreate(GroupCategoryCreateReq req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (req.effectiveDate() != null && req.endEffectiveDate() != null
                && req.endEffectiveDate().isBefore(req.effectiveDate())) {
            throw new BusinessException(ErrorCode.GC_INVALID_DATE_RANGE);
        }
    }

    private void validateUpdate(GroupCategoryUpdateReq req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (req.effectiveDate() != null && req.endEffectiveDate() != null
                && req.endEffectiveDate().isBefore(req.effectiveDate())) {
            throw new BusinessException(ErrorCode.GC_INVALID_DATE_RANGE);
        }
    }

    private void validateActionRequest(GroupCategoryActionReq req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (req.actor() == null || req.actor().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "actor không được để trống");
        }
    }
}