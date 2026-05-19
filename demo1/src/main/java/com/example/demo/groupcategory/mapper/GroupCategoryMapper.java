package com.example.demo.groupcategory.mapper;

import com.example.demo.groupcategory.dto.request.GroupCategoryUpsertReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import org.springframework.stereotype.Component;

import static com.example.demo.common.util.StringUtil.normalizeNullable;
import static com.example.demo.common.util.StringUtil.normalizeRequired;

@Component
public class GroupCategoryMapper {

    public void applyBaseFields(GroupCategory entity, GroupCategoryUpsertReq req) {
        // Gan paramName da duoc normalize bat buoc vao entity.
        entity.setParamName(normalizeRequired(req.paramName()));
        entity.setParamValue(normalizeRequired(req.paramValue()));
        entity.setParamType(normalizeRequired(req.paramType()));
        entity.setDescription(normalizeNullable(req.description()));
        entity.setComponentCode(normalizeNullable(req.componentCode()));
        // Copy ngay hieu luc tu request sang entity.
        entity.setEffectiveDate(req.effectiveDate());
        entity.setEndEffectiveDate(req.endEffectiveDate());
    }

    public GroupCategory buildPreviewEntity(GroupCategory current, GroupCategoryUpsertReq req) {
        // Clone entity hien tai de tao ban xem truoc, tranh sua truc tiep object goc.
        GroupCategory preview = cloneEntity(current);

        // Cap nhat cac field co ban tren ban preview theo du lieu request.
        preview.setParamName(normalizeRequired(req.paramName()));
        preview.setParamValue(normalizeRequired(req.paramValue()));
        preview.setParamType(normalizeRequired(req.paramType()));
        preview.setDescription(normalizeNullable(req.description()));
        preview.setComponentCode(normalizeNullable(req.componentCode()));
        preview.setEffectiveDate(req.effectiveDate());
        preview.setEndEffectiveDate(req.endEffectiveDate());

        // Chi ghi de isActive neu request co gui gia tri moi.
        if (req.isActive() != null) {
            // Cap nhat isActive tren ban preview.
            preview.setIsActive(req.isActive());
        }

        // Chi ghi de isDisplay neu request co gui gia tri moi.
        if (req.isDisplay() != null) {
            preview.setIsDisplay(req.isDisplay());
        }

        // Tra ve entity preview de cac lop khac validate/so sanh truoc khi luu that.
        return preview;
    }

    public GroupCategory cloneEntity(GroupCategory source) {
        // Tao entity moi de copy du lieu tu entity nguon.
        GroupCategory target = new GroupCategory();
        // Copy id de ban clone van dai dien cho cung mot ban ghi.
        target.setId(source.getId());
        // Copy paramName hien tai.
        target.setParamName(source.getParamName());
        target.setParamValue(source.getParamValue());
        target.setParamType(source.getParamType());
        target.setDescription(source.getDescription());
        target.setComponentCode(source.getComponentCode());
        target.setStatus(source.getStatus());
        target.setIsActive(source.getIsActive());
        target.setIsDisplay(source.getIsDisplay());
        target.setEffectiveDate(source.getEffectiveDate());
        target.setEndEffectiveDate(source.getEndEffectiveDate());
        // Copy NEW_DATA hien tai de ban clone giu nguyen boi canh thay doi.
        target.setNewData(source.getNewData());
        // Tra ve ban clone de su dung o cac luong xu ly tiep theo.
        return target;
    }
}
