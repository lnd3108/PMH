package com.example.demo.groupcategory.repository;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.common.paging.PageResponse;
import com.example.demo.groupcategory.dto.request.GroupCategoryCreateReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpdateReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Repository
@Transactional
public class GroupCategoryNativeRepository {

    @PersistenceContext
    private EntityManager em;

    private static final String BASE_SELECT = """
            SELECT
                ID,
                PARAM_NAME,
                PARAM_VALUE,
                PARAM_TYPE,
                DESCRIPTION,
                COMPONENT_CODE,
                STATUS,
                IS_ACTIVE,
                IS_DISPLAY,
                NEW_DATA,
                EFFECTIVE_DATE,
                END_EFFECTIVE_DATE
            FROM PMH_GROUP_CATEGORY
            """;

    private static final String BASE_COUNT = """
            SELECT COUNT(1)
            FROM PMH_GROUP_CATEGORY
            """;

    public Long create(GroupCategoryCreateReq req) {
        Long id = getNextId();
        String sql = """
                INSERT INTO PMH_GROUP_CATEGORY (
                    ID,
                    PARAM_NAME,
                    PARAM_VALUE,
                    PARAM_TYPE,
                    DESCRIPTION,
                    COMPONENT_CODE,
                    STATUS,
                    IS_ACTIVE,
                    IS_DISPLAY,
                    NEW_DATA,
                    EFFECTIVE_DATE,
                    END_EFFECTIVE_DATE
                ) VALUES (
                    :id,
                    :paramName,
                    :paramValue,
                    :paramType,
                    :description,
                    :componentCode,
                    :status,
                    :isActive,
                    :isDisplay,
                    :newData,
                    :effectiveDate,
                    :endEffectiveDate
                )
                """;

        em.createNativeQuery(sql)
                .setParameter("id", id)
                .setParameter("paramName", req.paramName())
                .setParameter("paramValue", req.paramValue())
                .setParameter("paramType", req.paramType())
                .setParameter("description", req.description())
                .setParameter("componentCode", req.componentCode())
                .setParameter("status", req.status())
                .setParameter("isActive", req.isActive())
                .setParameter("isDisplay", req.isDisplay())
                .setParameter("newData", req.newData())
                .setParameter("effectiveDate", req.effectiveDate())
                .setParameter("endEffectiveDate", req.endEffectiveDate())
                .executeUpdate();

        return id;
    }

    public PageResponse<GroupCategory> getAll(int page, int size) {
        //lấy danh sách tất cả bản ghi có phân trang
        String sql = BASE_SELECT + " ORDER BY ID DESC";
        Query dataQuery = em.createNativeQuery(sql); // tạo querry lấy dữ liệu
        dataQuery.setFirstResult(page * size); //bỏ qua số dòng của trang trước
        dataQuery.setMaxResults(size); // lấy tối đa size cho trang hiện tại

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();

        long total = ((Number) em.createNativeQuery(BASE_COUNT).getSingleResult()).longValue();
        return buildPageResponse(rows, page, size, total, "id", "desc");
    }

    public GroupCategory getById(Long id) {
        String sql = BASE_SELECT + " WHERE ID = :id";

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("id", id)
                .getResultList();//tránh lỗi nếu không có dữ liệu
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.GC_NOT_FOUND, "Khong tim thay ban ghi voi id = " + id);
        }
        return mapRow(rows.get(0));
    }

    public Long update(Long id, GroupCategoryUpdateReq req) {
        String sql = """
                UPDATE PMH_GROUP_CATEGORY
                SET
                    PARAM_NAME = :paramName,
                    PARAM_VALUE = :paramValue,
                    PARAM_TYPE = :paramType,
                    DESCRIPTION = :description,
                    COMPONENT_CODE = :componentCode,
                    STATUS = :status,
                    IS_ACTIVE = :isActive,
                    IS_DISPLAY = :isDisplay,
                    NEW_DATA = :newData,
                    EFFECTIVE_DATE = :effectiveDate,
                    END_EFFECTIVE_DATE = :endEffectiveDate
                WHERE ID = :id
                """;

        int updated = em.createNativeQuery(sql)
                .setParameter("id", id)
                .setParameter("paramName", req.paramName())
                .setParameter("paramValue", req.paramValue())
                .setParameter("paramType", req.paramType())
                .setParameter("description", req.description())
                .setParameter("componentCode", req.componentCode())
                .setParameter("status", req.status())
                .setParameter("isActive", req.isActive())
                .setParameter("isDisplay", req.isDisplay())
                .setParameter("newData", req.newData())
                .setParameter("effectiveDate", req.effectiveDate())
                .setParameter("endEffectiveDate", req.endEffectiveDate())
                .executeUpdate();

        if (updated == 0) {
            throw new BusinessException(ErrorCode.GC_NOT_FOUND, "Khong tim thay ban ghi de update, id = " + id);
        }

        return id;
    }

    public void delete(Long id) {
        String sql = """
                DELETE FROM PMH_GROUP_CATEGORY
                WHERE ID = :id
                """;

        int deleted = em.createNativeQuery(sql)
                .setParameter("id", id)
                .executeUpdate();

        if (deleted == 0) {
            throw new BusinessException(ErrorCode.GC_NOT_FOUND, "Khong tim thay ban ghi de xoa, id = " + id);
        }
    }

    public PageResponse<GroupCategory> search(GroupCategorySearchReq req, int page, int size) {
        //tạo where 1 = 1 đẻr nối thêm điều kiện and cho dễ
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        appendLikeCondition(where, req.paramName(), "PARAM_NAME", "paramName");
        appendLikeCondition(where, req.paramValue(), "PARAM_VALUE", "paramValue");
        appendLikeCondition(where, req.paramType(), "PARAM_TYPE", "paramType");
        appendInCondition(where, req.status(), "STATUS", "status");
        appendInCondition(where, req.isActive(), "IS_ACTIVE", "isActive");

        String sql = BASE_SELECT + where + " ORDER BY ID DESC";
        String countSql = BASE_COUNT + where;

        Query dataQuery = em.createNativeQuery(sql);
        Query countQuery = em.createNativeQuery(countSql);
        setSearchParams(dataQuery, req);
        setSearchParams(countQuery, req);
        dataQuery.setFirstResult(page * size);
        dataQuery.setMaxResults(size); // count không cần phân trang vì chỉ trả ra 1 giá trị

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();

        long total = ((Number) countQuery.getSingleResult()).longValue();
        return buildPageResponse(rows, page, size, total, "id", "desc");
    }

    public void updateWorkflowState(Long id, Integer status, Integer isDisplay, String newData) {
        String sql = """
                UPDATE PMH_GROUP_CATEGORY
                SET
                    STATUS = :status,
                    IS_DISPLAY = :isDisplay,
                    NEW_DATA = :newData
                WHERE ID = :id
                """;

        int updated = em.createNativeQuery(sql)
                .setParameter("id", id)
                .setParameter("status", status)
                .setParameter("isDisplay", isDisplay)
                .setParameter("newData", newData)
                .executeUpdate();
        if (updated == 0) {
            throw new BusinessException(ErrorCode.GC_NOT_FOUND, "Khong tim thay ban ghi voi id = " + id);
        }
    }

    //hàm duyêt thành công ghi dữ liệu đã được approve vào DB
    public void applyApprovedData(GroupCategory entity) {
        String sql = """
                UPDATE PMH_GROUP_CATEGORY
                SET
                    PARAM_NAME = :paramName,
                    PARAM_VALUE = :paramValue,
                    PARAM_TYPE = :paramType,
                    DESCRIPTION = :description,
                    COMPONENT_CODE = :componentCode,
                    STATUS = :status,
                    IS_ACTIVE = :isActive,
                    IS_DISPLAY = :isDisplay,
                    NEW_DATA = :newData,
                    EFFECTIVE_DATE = :effectiveDate,
                    END_EFFECTIVE_DATE = :endEffectiveDate
                WHERE ID = :id
                """;

        int updated = em.createNativeQuery(sql)
                .setParameter("id", entity.getId())
                .setParameter("paramName", entity.getParamName())
                .setParameter("paramValue", entity.getParamValue())
                .setParameter("paramType", entity.getParamType())
                .setParameter("description", entity.getDescription())
                .setParameter("componentCode", entity.getComponentCode())
                .setParameter("status", entity.getStatus())
                .setParameter("isActive", entity.getIsActive())
                .setParameter("isDisplay", entity.getIsDisplay())
                .setParameter("newData", entity.getNewData())
                .setParameter("effectiveDate", entity.getEffectiveDate())
                .setParameter("endEffectiveDate", entity.getEndEffectiveDate())
                .executeUpdate();

        if (updated == 0) {
            throw new BusinessException(ErrorCode.GC_NOT_FOUND, "Khong tim thay ban ghi voi id = " + entity.getId());
        }
    }

    /*
    Query sequence Oracle để lấy id tiếp theo.
    dual là bảng giả trong Oracle.
    NEXTVAL tăng sequence lên 1 đơn vị.
    Ép kết quả sang long.
     */
    private Long getNextId() {
        String sql = "SELECT PMH_GROUP_CATEGORY_SEQ.NEXTVAL FROM dual";
        Number number = (Number) em.createNativeQuery(sql).getSingleResult();
        return number.longValue();
    }

    private PageResponse<GroupCategory> buildPageResponse(
            @NonNull List<Object[]> rows,
            int page,
            int size,
            long total,
            String sortBy,
            String sortDir
    ) {
        List<GroupCategory> content = rows.stream()
                .map(this::mapRow)
                .toList();

        int totalPages = (int) Math.ceil((double) total / size);

        return PageResponse.<GroupCategory>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page + 1 >= totalPages)
                .empty(content.isEmpty())
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
    }

    private void setSearchParams(Query query, GroupCategorySearchReq req) {
        setLikeParameter(query, req.paramName(), "paramName");
        setLikeParameter(query, req.paramValue(), "paramValue");
        setLikeParameter(query, req.paramType(), "paramType");
        setInParameter(query, req.status(), "status");
        setInParameter(query, req.isActive(), "isActive");
    }

    private GroupCategory mapRow(Object[] row) {
        GroupCategory entity = new GroupCategory();
        entity.setId(toLong(row[0]));
        entity.setParamName((String) row[1]);
        entity.setParamValue((String) row[2]);
        entity.setParamType((String) row[3]);
        entity.setDescription((String) row[4]);
        entity.setComponentCode((String) row[5]);
        entity.setStatus(toInteger(row[6]));
        entity.setIsActive(toInteger(row[7]));
        entity.setIsDisplay(toInteger(row[8]));
        entity.setNewData((String) row[9]);
        entity.setEffectiveDate(toLocalDate(row[10]));
        entity.setEndEffectiveDate(toLocalDate(row[11]));
        return entity;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void appendLikeCondition(StringBuilder sql, String value, String column, String param) {
        if (hasText(value)) {
            sql.append(" AND UPPER(")
                    .append(column)
                    .append(") LIKE UPPER(:")
                    .append(param)
                    .append(")");
        }
    }

    private void appendInCondition(StringBuilder sql, List<?> values, String column, String param) {
        if (values != null && !values.isEmpty()) {
            sql.append(" AND ")
                    .append(column)
                    .append(" IN (:")
                    .append(param)
                    .append(")");
        }
    }

    private void setLikeParameter(Query query, String value, String param) {
        if (hasText(value)) {
            query.setParameter(param, "%" + value.trim() + "%");
        }
    }

    private void setInParameter(Query query, List<?> values, String param) {
        if (values != null && !values.isEmpty()) {
            query.setParameter(param, values);
        }
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private java.time.LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return null;
    }
}
