package com.example.demo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("CM_400", HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    UNAUTHORIZED("CM_401", HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu"),
    FORBIDDEN("CM_403", HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    NOT_FOUND("CM_404", HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    CONFLICT("CM_409", HttpStatus.CONFLICT, "Dữ liệu đã tồn tại"),
    INVALID_STATE("CM_422", HttpStatus.UNPROCESSABLE_ENTITY, "Trạng thái dữ liệu không hợp lệ"),
    INTERNAL_ERROR("CM_500", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống"),

    GC_INVALID_ID("GC_400_01", HttpStatus.BAD_REQUEST, "Id không hợp lệ"),
    GC_INVALID_PAGE("GC_400_02", HttpStatus.BAD_REQUEST, "Page phải >= 0 và size phải trong khoảng 1–100"),
    GC_PARAM_NAME_REQUIRED("GC_400_03", HttpStatus.BAD_REQUEST, "paramName không được để trống"),
    GC_PARAM_VALUE_REQUIRED("GC_400_04", HttpStatus.BAD_REQUEST, "paramValue không được để trống"),
    GC_PARAM_TYPE_REQUIRED("GC_400_05", HttpStatus.BAD_REQUEST, "paramType không được để trống"),

    GC_NOT_FOUND("GC_404", HttpStatus.NOT_FOUND, "Không tìm thấy nhóm danh mục"),
    GC_DUPLICATE("GC_409", HttpStatus.CONFLICT, "Bản ghi đã tồn tại"),
    GC_NO_CHANGES("GC_422", HttpStatus.UNPROCESSABLE_ENTITY, "Không có dữ liệu thay đổi"),
    GC_PENDING_CANNOT_DELETE("GC_422_01", HttpStatus.UNPROCESSABLE_ENTITY, "Bản ghi đang chờ duyệt thì không được xóa"),
    GC_APPROVED_CANNOT_DELETE("GC_422_02", HttpStatus.UNPROCESSABLE_ENTITY, "Bản ghi đã duyệt / đang hiển thị thì không được xóa"),
    GC_ONLY_PENDING_CAN_APPROVE("GC_422_03", HttpStatus.UNPROCESSABLE_ENTITY, "Chỉ bản ghi chờ phê duyệt mới được duyệt"),
    GC_ONLY_PENDING_CAN_REJECT("GC_422_04", HttpStatus.UNPROCESSABLE_ENTITY, "Chỉ bản ghi chờ phê duyệt mới được từ chối"),
    GC_ONLY_APPROVED_CAN_CANCEL("GC_422_05", HttpStatus.UNPROCESSABLE_ENTITY, "Chỉ bản ghi đã phê duyệt mới được hủy duyệt"),
    GC_ALREADY_PENDING("GC_422_06", HttpStatus.UNPROCESSABLE_ENTITY, "Bản ghi đang ở trạng thái chờ phê duyệt"),

    GC_INVALID_DATE_RANGE("GC_400_06", HttpStatus.BAD_REQUEST, "endEffectiveDate phải lớn hơn hoặc bằng effectiveDate"),
    GC_INVALID_NEW_DATA("GC_500_01", HttpStatus.INTERNAL_SERVER_ERROR, "NEW_DATA không đúng định dạng JSON"),
    GC_SERIALIZE_NEW_DATA_FAILED("GC_500_02", HttpStatus.INTERNAL_SERVER_ERROR, "Không serialize được NEW_DATA"),

    GC_CREATE_FAILED("GC_500_03", HttpStatus.INTERNAL_SERVER_ERROR, "Tạo nhóm danh mục thất bại"),
    GC_UPDATE_FAILED("GC_500_04", HttpStatus.INTERNAL_SERVER_ERROR, "Cập nhật nhóm danh mục thất bại"),
    GC_DELETE_FAILED("GC_500_05", HttpStatus.INTERNAL_SERVER_ERROR, "Xóa nhóm danh mục thất bại"),
    GC_SEARCH_FAILED("GC_500_06", HttpStatus.INTERNAL_SERVER_ERROR, "Tìm kiếm nhóm danh mục thất bại"),
    GC_GET_ALL_FAILED("GC_500_07", HttpStatus.INTERNAL_SERVER_ERROR, "Lấy danh sách nhóm danh mục thất bại");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
