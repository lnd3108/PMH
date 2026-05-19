package com.example.demo.groupcategory.repository;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.common.paging.PageResponse;
import com.example.demo.groupcategory.dto.request.GroupCategoryActionReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryCreateReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.dto.request.GroupCategoryUpdateReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Repository
// Logic xử lý nằm trong đảm baor tính toàn vẹn dữ liệu cho một luồng xử lý
@Transactional
public class GroupCategoryProcedureRepo {

    //inject Entitymanager do JPA quản lý vào class
    @PersistenceContext
    private EntityManager em;

    public Long create(GroupCategoryCreateReq req){
        try {
            //Tạo prc query gọi từ DB
            StoredProcedureQuery sp = em.createStoredProcedureQuery("LND_PRC_GC_CREATE");

            //Mapping dữ liệu khai báo prc nhận tham số nào kiểu gì
            sp.registerStoredProcedureParameter("P_PARAM_NAME", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_PARAM_VALUE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_PARAM_TYPE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_DESCRIPTION", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_COMPONENT_CODE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_STATUS", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_IS_ACTIVE", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_IS_DISPLAY", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_EFFECTIVE_DATE", Date.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_END_EFFECTIVE_DATE", Date.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_ID", Long.class, ParameterMode.OUT);

            //gán dữ liệu từ request
            sp.setParameter("P_PARAM_NAME", req.paramName());
            sp.setParameter("P_PARAM_VALUE", req.paramValue());
            sp.setParameter("P_PARAM_TYPE", req.paramType());
            sp.setParameter("P_DESCRIPTION", req.description());
            sp.setParameter("P_COMPONENT_CODE", req.componentCode());
            sp.setParameter("P_STATUS", req.status());
            sp.setParameter("P_IS_ACTIVE", req.isActive());
            sp.setParameter("P_IS_DISPLAY", req.isDisplay());
            sp.setParameter("P_EFFECTIVE_DATE", req.effectiveDate() == null ? null : Date.valueOf(req.effectiveDate()));
            sp.setParameter("P_END_EFFECTIVE_DATE", req.endEffectiveDate() == null ? null : Date.valueOf(req.endEffectiveDate()));

            //chạy prc trong db
            sp.execute();

            //đọc output
            Object outId = sp.getOutputParameterValue("P_ID");
            //trả ra id
            return outId == null ? null : ((Number) outId).longValue();
        } catch (Exception e) {
            throw mapProcedureException(e, ErrorCode.GC_CREATE_FAILED);
        }
    }

    //tắt cảnh báo khi ép kiểu generic mà compiler không kiểm tra chắc chắn được
    @SuppressWarnings("unchecked")
    public PageResponse<GroupCategory> getAll(int page, int size) {
        try {
            int safePage = Math.max(page, 0);
            int safeSize = size <= 0 ? 10 : Math.min(size, 100);

            StoredProcedureQuery sp= em.createStoredProcedureQuery("LND_PRC_GC_GET_ALL_PAGE");

            sp.registerStoredProcedureParameter("P_PAGE", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_SIZE", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_TOTAL", Long.class, ParameterMode.OUT);
            sp.registerStoredProcedureParameter("P_CURSOR", void.class, ParameterMode.REF_CURSOR);

            sp.setParameter("P_PAGE", safePage);
            sp.setParameter("P_SIZE", safeSize);

            sp.execute();

            List<Object[]> rows = sp.getResultList();
            List<GroupCategory> result = new ArrayList<>();
            for(Object[] row : rows){
                result.add(mapRow(row));
            }

            long total = ((Number) sp.getOutputParameterValue("P_TOTAL")).longValue();

            return PageResponse.fromNative(result, safePage, safeSize, total, "effectiveDate", "desc", item -> item);
        } catch (Exception e) {
            throw mapProcedureException(e, ErrorCode.GC_GET_ALL_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    public GroupCategory getById(Long id){
        try {
            StoredProcedureQuery sp = em.createStoredProcedureQuery("LND_PRC_GC_GET_BY_ID");

            sp.registerStoredProcedureParameter("P_ID", Long.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_CURSOR", void.class, ParameterMode.REF_CURSOR);

            sp.setParameter("P_ID", id);
            sp.execute();

            List<Object[]> rows = sp.getResultList();
            if(rows == null || rows.isEmpty()){
                throw new BusinessException(ErrorCode.GC_NOT_FOUND);
            }

            return mapRow(rows.get(0));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw mapProcedureException(e, ErrorCode.INTERNAL_ERROR);
        }
    }

    public Long update (Long id, GroupCategoryUpdateReq req){
        try {
            StoredProcedureQuery sp = em.createStoredProcedureQuery("LND_PRC_GC_UPDATE");

            sp.registerStoredProcedureParameter("P_ID", Long.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_PARAM_NAME", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_PARAM_VALUE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_PARAM_TYPE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_DESCRIPTION", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_COMPONENT_CODE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_STATUS", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_IS_ACTIVE", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_IS_DISPLAY", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_EFFECTIVE_DATE", Date.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_END_EFFECTIVE_DATE", Date.class, ParameterMode.IN);

            sp.setParameter("P_ID", id);
            sp.setParameter("P_PARAM_NAME", req.paramName());
            sp.setParameter("P_PARAM_VALUE", req.paramValue());
            sp.setParameter("P_PARAM_TYPE", req.paramType());
            sp.setParameter("P_DESCRIPTION", req.description());
            sp.setParameter("P_COMPONENT_CODE", req.componentCode());
            sp.setParameter("P_STATUS", req.status());
            sp.setParameter("P_IS_ACTIVE", req.isActive());
            sp.setParameter("P_IS_DISPLAY", req.isDisplay());
            sp.setParameter("P_EFFECTIVE_DATE", req.effectiveDate() == null ? null : Date.valueOf(req.effectiveDate()));
            sp.setParameter("P_END_EFFECTIVE_DATE", req.endEffectiveDate() == null ? null : Date.valueOf(req.endEffectiveDate()));

            sp.execute();
            return id;
        } catch (Exception e) {
            throw mapProcedureException(e, ErrorCode.GC_UPDATE_FAILED);
        }
    }

    public void delete(Long id){
        try {
            StoredProcedureQuery sp = em.createStoredProcedureQuery("LND_PRC_GC_DELETE");
            //truyền id
            sp.registerStoredProcedureParameter("P_ID", Long.class, ParameterMode.IN);
            sp.setParameter("P_ID", id);
            //chạy prc thưcj từ db
            sp.execute();

        } catch (Exception e) {
            throw mapProcedureException(e, ErrorCode.GC_DELETE_FAILED);
        }
    }

    public Long action(Long id, String action, GroupCategoryActionReq req){
        try {
            StoredProcedureQuery sp = em.createStoredProcedureQuery("LND_PRC_GC_WORKFLOW");

            sp.registerStoredProcedureParameter("P_ID", Long.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_ACTION", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_ACTOR", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_NOTE", String.class, ParameterMode.IN);

            sp.setParameter("P_ID", id);
            sp.setParameter("P_ACTION", action);
            sp.setParameter("P_ACTOR", req.actor() == null ? null : req.actor().trim());
            sp.setParameter("P_NOTE", emptyToNull(req.note()));

            sp.execute();
            return id;
        } catch (Exception e) {
            throw mapWorkflowException(e, action);
        }
    }

    private BusinessException mapWorkflowException(Exception e, String action) {
        String message = getDeepMessage(e);

        if (message.contains("Bản ghi đang ở trạng thái chờ duyệt")) {
            return new BusinessException(ErrorCode.GC_ALREADY_PENDING);
        }
        if (message.contains("Chỉ bản ghi chờ duyệt mới được approve")) {
            return new BusinessException(ErrorCode.GC_ONLY_PENDING_CAN_APPROVE);
        }
        if (message.contains("Chỉ bản ghi chờ duyệt mới được reject")) {
            return new BusinessException(ErrorCode.GC_ONLY_PENDING_CAN_REJECT);
        }
        if (message.contains("Không có NEW_DATA để gửi duyệt")
                || message.contains("NEW_DATA rỗng, không có dữ liệu để approve")) {
            return new BusinessException(ErrorCode.GC_INVALID_NEW_DATA);
        }
        if (message.contains("Không tìm thấy bản ghi cần xử lý workflow")) {
            return new BusinessException(ErrorCode.GC_NOT_FOUND);
        }
        if (message.contains("P_ACTION chỉ được là SUBMIT, APPROVE hoặc REJECT")
                || message.contains("P_ID không được để trống")
                || message.contains("effectiveDate phải có dạng YYYY-MM-DD")
                || message.contains("endEffectiveDate phải có dạng YYYY-MM-DD")) {
            return new BusinessException(ErrorCode.INVALID_REQUEST, message);
        }

        ErrorCode fallbackCode = switch (action) {
            case "SUBMIT" -> ErrorCode.GC_ALREADY_PENDING;
            case "APPROVE" -> ErrorCode.GC_ONLY_PENDING_CAN_APPROVE;
            case "REJECT" -> ErrorCode.GC_ONLY_PENDING_CAN_REJECT;
            default -> ErrorCode.INTERNAL_ERROR;
        };
        return new BusinessException(fallbackCode);
    }

    public PageResponse<GroupCategory> search(GroupCategorySearchReq req) {
        try {

            //chặn input xấu như là giá trị âm hoặc giá trị quá lơn
            int safePage = req.page() == null || req.page() < 0 ? 0 : req.page();
            int safeSize = req.size() == null || req.size() <= 0 ? 10 : Math.min(req.size(), 100);

            //Tạo prc querry gọi từ DB
            StoredProcedureQuery sp = em.createStoredProcedureQuery("LND_PRC_GC_SEARCH_PAGE_2");

            //TODO:Mapping mức khai báo tham số
            //cho biết prc nhận những trường nào, kiểu gì
            //mapping contract giữa java và prc
            sp.registerStoredProcedureParameter("P_PARAM_NAME", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_PARAM_VALUE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_PARAM_TYPE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_IS_ACTIVE", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_PAGE", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_SIZE", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("P_TOTAL", Long.class, ParameterMode.OUT);
            sp.registerStoredProcedureParameter("P_CURSOR", void.class, ParameterMode.REF_CURSOR);

            //TODO: Mapping dữ liệu input thưcj tế
            //setParam đẩy dữ liệu từ java sang DB
            sp.setParameter("P_PARAM_NAME", emptyToNull(req.paramName()));
            sp.setParameter("P_PARAM_VALUE", emptyToNull(req.paramValue()));
            sp.setParameter("P_PARAM_TYPE", emptyToNull(req.paramType()));
            sp.setParameter("P_STATUS", joinIntegerList(req.status()));
            sp.setParameter("P_IS_ACTIVE", joinIntegerList(req.isActive()));
            sp.setParameter("P_PAGE", safePage);
            sp.setParameter("P_SIZE", safeSize);

            sp.execute();

            List<Object[]> rows = sp.getResultList();
            List<GroupCategory> result = new ArrayList<>();
            for (Object[] row : rows) {
                result.add(mapRow(row));
            }

            //map dữ liệu output của DB về java
            long total = ((Number) sp.getOutputParameterValue("P_TOTAL")).longValue();

            //đóng gói kết quả theo format và trả ra API
            return PageResponse.fromNative(result, safePage, safeSize, total, "effectiveDate", "desc", item -> item);
        } catch (Exception e) {
            throw mapProcedureException(e, ErrorCode.GC_SEARCH_FAILED);
        }
    }


    //chuyển đổi lỗi  DB/PRC thành lỗi nghiệp vụ của ứng dụng để dễ fig
    private BusinessException mapProcedureException(Exception e, ErrorCode defaultCode) {
        String message = getDeepMessage(e);

        if (message.contains("GC_DUPLICATE")) {
            return new BusinessException(ErrorCode.GC_DUPLICATE);
        }
        if (message.contains("GC_NOT_FOUND")) {
            return new BusinessException(ErrorCode.GC_NOT_FOUND);
        }
        if (message.contains("GC_PENDING_CANNOT_DELETE")) {
            return new BusinessException(ErrorCode.GC_PENDING_CANNOT_DELETE);
        }
        if (message.contains("GC_APPROVED_CANNOT_DELETE")) {
            return new BusinessException(ErrorCode.GC_APPROVED_CANNOT_DELETE);
        }
        if (message.contains("GC_INVALID_DATE_RANGE")) {
            return new BusinessException(ErrorCode.GC_INVALID_DATE_RANGE);
        }

        return new BusinessException(defaultCode);
    }

    //duyệt toàn bộ lỗi của exception bằng while (e != null)  rồi nối message lại thành một string
    private String getDeepMessage(Throwable e) {
        StringBuilder sb = new StringBuilder();
        while (e != null) {
            if (e.getMessage() != null) {
                sb.append(e.getMessage()).append(" | ");
            }
            e = e.getCause();
        }
        return sb.toString();
    }

    //TODO: Mapping dữ liệu output thật sự từ DB về java Object
    //chuyển một dòng dữ liệu thô từ cursor thành Object
    private GroupCategory mapRow(Object[] row){
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
        entity.setEffectiveDate(toLocalDate(row[9]));
        entity.setEndEffectiveDate(toLocalDate(row[10]));
        return entity;
    }

    //3 hàm convert kiểu dữ liệu an toàn
    private Long toLong(Object value){
        if(value == null) return null;
        return ((Number) value).longValue();
    }

    private Integer toInteger(Object value){
        if(value == null) return null;
        return ((Number) value).intValue();
    }

    private java.time.LocalDate toLocalDate(Object value){
        if (value == null) return null;
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        return null;
    }

    //hàm đổi list thành chuỗi để prc tách và lọc theo in
    private String joinIntegerList(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    //nếu chuỗi rỗng hoặc chủi toàn khoẳng trắng thì đổi thành null
    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

//Tạo storedProcedureQuerry
//đăng ký tham số IN/out/ref_cursor
//set dữ liệu từ request
//execute
//lấy kết quả trả ra
//map dữ liệu thô thành entity/response
//nếu lỗi thì map về bussinessException chuẩn