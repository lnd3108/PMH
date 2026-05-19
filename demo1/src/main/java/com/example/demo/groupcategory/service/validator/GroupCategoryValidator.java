package com.example.demo.groupcategory.service.validator;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpsertReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import com.example.demo.groupcategory.repository.GroupCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.example.demo.common.util.StringUtil.isBlank;
import static com.example.demo.common.util.StringUtil.normalizeRequired;

@Component
@RequiredArgsConstructor
public class GroupCategoryValidator {
    private final GroupCategoryRepository repository;

    public void validateRequired(GroupCategoryUpsertReq req) {
        if (isBlank(req.paramName())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramName không được để trống");
        }
        if (isBlank(req.paramValue())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramValue không được để trống");
        }
        if (isBlank(req.paramType())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramType không được để trống");
        }
        if (req.effectiveDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "effectiveDate không được để trống");
        }
        //nếu có ngày kết thúc, ngày này không được sớm hơn ngày hiệu lực
        if (req.endEffectiveDate() != null && req.endEffectiveDate().isBefore(req.effectiveDate())) {
            //nếu ngày hết hiệu lưcj không hợp lệ thì ném lỗi businesss
            throw new BusinessException(ErrorCode.GC_INVALID_DATE_RANGE);
        }
    }

    //kiểm tra dữ liệu sau khi map
    public void validateRequiredEntity(GroupCategory entity) {
        if (isBlank(entity.getParamName())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramName không được để trống");
        }
        if (isBlank(entity.getParamValue())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramValue không được để trống");
        }
        if (isBlank(entity.getParamType())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "paramType không được để trống");
        }
        if (entity.getEffectiveDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "effectiveDate không được để trống");
        }

        if (entity.getEndEffectiveDate() != null
                && entity.getEndEffectiveDate().isBefore(entity.getEffectiveDate())) {
            throw new BusinessException(ErrorCode.GC_INVALID_DATE_RANGE);
        }
    }

    public void validateDuplicateForUpsert(GroupCategoryUpsertReq req, Long excludeId) {
        //check các field khóa để tránh trùng do khác biệt khoảng trắng/ chuẩn hóa
        String paramName = normalizeRequired(req.paramName());
        String paramValue = normalizeRequired(req.paramValue());
        String paramType = normalizeRequired(req.paramType());

        //Nếu id null thì kiểm tra duplicate cho create, nếu không bỏ qua bản ghi
        boolean exists = excludeId == null ? repository.existsByParamNameAndParamValueAndParamType(paramName, paramValue, paramType)
                : repository.existsByParamNameAndParamValueAndParamTypeAndIdNot(paramName, paramValue, paramType, excludeId);
        //nếu đã có bản ghi thì chặn upsert
        if (exists) {
            throw new BusinessException(
                    ErrorCode.GC_DUPLICATE,
                    "Bản ghi đã tồn tại với bộ paramName + paramValue + paramType"
            );
        }
    }

    public void validateDuplicateForEntity(GroupCategory entity, Long excludeId) {
       //kiểm tra duplicate trên entity đã có sẵn dữ liệu, dùng sau khi map/patch
        boolean exists = excludeId == null
                ? repository.existsByParamNameAndParamValueAndParamType(
                normalizeRequired(entity.getParamName()),
                normalizeRequired(entity.getParamValue()),
                normalizeRequired(entity.getParamType())
        )
                : repository.existsByParamNameAndParamValueAndParamTypeAndIdNot(
                normalizeRequired(entity.getParamName()),
                normalizeRequired(entity.getParamValue()),
                normalizeRequired(entity.getParamType()),
                excludeId
        );
        //nếu tồn tại thì trả lỗi
        if (exists) {
            throw new BusinessException(
                    ErrorCode.GC_DUPLICATE,
                    "Bản ghi đã tồn tại với bộ paramName + paramValue + paramType"
            );
        }
    }
}