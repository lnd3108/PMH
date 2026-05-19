package com.example.demo.groupcategory.service.helper;

import com.example.demo.groupcategory.dto.request.GroupCategoryUpsertReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;

import static com.example.demo.common.util.StringUtil.isBlank;
import static com.example.demo.common.util.StringUtil.normalizeNullable;
import static com.example.demo.common.util.StringUtil.normalizeRequired;

@Component
@RequiredArgsConstructor
public class GroupCategoryNewDataHelper {

    // ObjectMapper duoc Spring inject de tao va doc JSON.
    private final ObjectMapper objectMapper;

    public String buildPatchJson(GroupCategory current, GroupCategoryUpsertReq req) {
        // Tao JSON object rong, chi chua cac field thay doi.
        ObjectNode patch = objectMapper.createObjectNode();

        // So sanh paramName hien tai voi gia tri moi sau khi normalize bat buoc.
        putIfChanged(patch, "paramName", current.getParamName(), normalizeRequired(req.paramName()));
        // So sanh paramValue hien tai voi gia tri moi sau khi normalize bat buoc.
        putIfChanged(patch, "paramValue", current.getParamValue(), normalizeRequired(req.paramValue()));
        // So sanh paramType hien tai voi gia tri moi sau khi normalize bat buoc.
        putIfChanged(patch, "paramType", current.getParamType(), normalizeRequired(req.paramType()));
        // So sanh description hien tai voi gia tri moi sau khi normalize cho phep null.
        putIfChanged(patch, "description", current.getDescription(), normalizeNullable(req.description()));
        // So sanh componentCode hien tai voi gia tri moi sau khi normalize cho phep null.
        putIfChanged(patch, "componentCode", current.getComponentCode(), normalizeNullable(req.componentCode()));
        // Chuyen effectiveDate sang String de so sanh dong nhat giua entity va request.
        putIfChanged(patch, "effectiveDate", toString(current.getEffectiveDate()), toString(req.effectiveDate()));
        // Chuyen endEffectiveDate sang String de so sanh dong nhat giua entity va request.
        putIfChanged(patch, "endEffectiveDate", toString(current.getEndEffectiveDate()), toString(req.endEffectiveDate()));

        // Neu request co gui isActive va khac gia tri hien tai thi dua vao patch.
        if (req.isActive() != null && !Objects.equals(current.getIsActive(), req.isActive())) {
            // Ghi gia tri isActive moi vao JSON patch.
            patch.put("isActive", req.isActive());
        }

        // Neu request co gui isDisplay va khac gia tri hien tai thi dua vao patch.
        if (req.isDisplay() != null && !Objects.equals(current.getIsDisplay(), req.isDisplay())) {
            // Ghi gia tri isDisplay moi vao JSON patch.
            patch.put("isDisplay", req.isDisplay());
        }

        try {
            // Serialize ObjectNode thanh chuoi JSON de luu vao NEW_DATA.
            return objectMapper.writeValueAsString(patch);
        } catch (JsonProcessingException e) {
            // Nem loi runtime neu khong the chuyen patch thanh JSON.
            throw new RuntimeException("Khong serialize duoc NEW_DATA", e);
        }
    }

    public void applyPatchJson(GroupCategory target, String patchJson) {
        try {
            // Parse chuoi patch JSON thanh JsonNode de doc tung field.
            JsonNode node = objectMapper.readTree(patchJson);

            // Neu patch co paramName thi cap nhat vao entity dich.
            if (node.has("paramName")) {
                // Doc text, trim khoang trang, rong thi tra ve null.
                target.setParamName(asTextOrNull(node.get("paramName")));
            }
            // Neu patch co paramValue thi cap nhat vao entity dich.
            if (node.has("paramValue")) {
                // Doc text, trim khoang trang, rong thi tra ve null.
                target.setParamValue(asTextOrNull(node.get("paramValue")));
            }
            // Neu patch co paramType thi cap nhat vao entity dich.
            if (node.has("paramType")) {
                // Doc text, trim khoang trang, rong thi tra ve null.
                target.setParamType(asTextOrNull(node.get("paramType")));
            }
            // Neu patch co description thi cap nhat vao entity dich.
            if (node.has("description")) {
                // Doc text, trim khoang trang, rong thi tra ve null.
                target.setDescription(asTextOrNull(node.get("description")));
            }
            // Neu patch co componentCode thi cap nhat vao entity dich.
            if (node.has("componentCode")) {
                // Doc text, trim khoang trang, rong thi tra ve null.
                target.setComponentCode(asTextOrNull(node.get("componentCode")));
            }
            // Neu patch co effectiveDate thi parse sang LocalDate roi gan vao entity dich.
            if (node.has("effectiveDate")) {
                // Chuyen JsonNode sang LocalDate.
                target.setEffectiveDate(parseLocalDate(node.get("effectiveDate")));
            }
            // Neu patch co endEffectiveDate thi parse sang LocalDate roi gan vao entity dich.
            if (node.has("endEffectiveDate")) {
                // Chuyen JsonNode sang LocalDate.
                target.setEndEffectiveDate(parseLocalDate(node.get("endEffectiveDate")));
            }
            // Neu patch co isActive thi cap nhat vao entity dich.
            if (node.has("isActive")) {
                // JSON null thi set null, nguoc lai doc so nguyen.
                target.setIsActive(node.get("isActive").isNull() ? null : node.get("isActive").asInt());
            }
            // Neu patch co isDisplay thi cap nhat vao entity dich.
            if (node.has("isDisplay")) {
                // JSON null thi set null, nguoc lai doc so nguyen.
                target.setIsDisplay(node.get("isDisplay").isNull() ? null : node.get("isDisplay").asInt());
            }
        } catch (Exception e) {
            // Nem loi runtime neu patchJson khong dung dinh dang JSON.
            throw new RuntimeException("NEW_DATA khong dung dinh dang JSON", e);
        }
    }

    public boolean hasMeaningfulNewData(String newData) {
        // Neu chuoi null, rong hoac chi co khoang trang thi khong co du lieu moi.
        if (isBlank(newData)) {
            return false;
        }

        // Trim de loai bo khoang trang dau/cuoi truoc khi danh gia.
        String trimmed = newData.trim();
        // "{}" va "null" deu duoc xem la khong co thong tin thay doi co y nghia.
        if ("{}".equals(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return false;
        }

        try {
            // Parse chuoi da trim thanh JsonNode de xac dinh chinh xac noi dung.
            JsonNode node = objectMapper.readTree(trimmed);
            // Neu parse ra null JSON thi xem la khong co du lieu.
            if (node == null || node.isNull()) {
                return false;
            }
            // Primitive la co y nghia; object/array chi co y nghia khi co phan tu.
            return !(node.isObject() || node.isArray()) || node.size() > 0;
        } catch (Exception e) {
            // Neu du lieu khong parse duoc JSON thi van xem la co du lieu de tranh bo sot.
            return true;
        }
    }

    private void putIfChanged(ObjectNode patch, String field, String oldValue, String newValue) {
        // Chi dua field vao patch khi gia tri cu va moi khac nhau.
        if (!Objects.equals(oldValue, newValue)) {
            // Neu gia tri moi la null thi luu JSON null.
            if (newValue == null) {
                patch.putNull(field);
            } else {
                // Neu gia tri moi khac null thi luu chuoi moi vao JSON patch.
                patch.put(field, newValue);
            }
        }
    }

    private String asTextOrNull(JsonNode node) {
        // Neu node khong ton tai hoac la JSON null thi tra ve null.
        if (node == null || node.isNull()) {
            return null;
        }
        // Lay noi dung text tu JsonNode.
        String value = node.asText();
        // Chuan hoa ve null neu text rong, nguoc lai trim roi tra ve.
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private LocalDate parseLocalDate(JsonNode node) {
        // Neu node khong ton tai hoac la JSON null thi khong co ngay de parse.
        if (node == null || node.isNull()) {
            return null;
        }
        // Parse chuoi ngay theo dinh dang ISO-8601 thanh LocalDate.
        return LocalDate.parse(node.asText());
    }

    private String toString(LocalDate date) {
        // Chuyen LocalDate thanh String, neu null thi giu nguyen null.
        return date == null ? null : date.toString();
    }
}
