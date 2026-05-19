package com.example.demo.groupcategory.service.Impl;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.groupcategory.constant.GroupCategoryConstant;
import com.example.demo.groupcategory.dto.excel.GroupCatExcelImportErrorRes;
import com.example.demo.groupcategory.dto.excel.GroupCatExcelImportResultRes;
import com.example.demo.groupcategory.dto.excel.GroupCategoryExcelRowReq;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import com.example.demo.groupcategory.repository.GroupCategoryRepository;
import com.example.demo.groupcategory.repository.specification.GroupCategorySpecification;
import com.example.demo.groupcategory.service.GroupCategoryExcelService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImplGroupCategoryExcelService implements GroupCategoryExcelService {

    private static final List<String> EXPORT_HEADERS = List.of(
            "Param Name",
            "Param Value",
            "Param Type",
            "Description",
            "Component Code",
            "Status",
            "Is Active",
            "Is Display",
            "Effective Date",
            "End Effective Date"
    );

    private static final List<String> TEMPLATE_HEADERS = List.of(
            "Danh mục theo nhóm",
            "Giá trị thành phần",
            "Tên thành phần",
            "Cấu phần xử lý",
            "Ngày hiệu lực",
            "Ngày hết hiệu lực",
            "Mô tả"
    );

    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final List<DateTimeFormatter> IMPORT_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d.M.uuuu")
    );

    private static final int BATCH_SIZE = 500;
    private static final int MAX_IMPORT_ROWS = 10_000;
    private static final int TEMPLATE_FIRST_DATA_ROW = 3;
    private static final int TEMPLATE_LAST_DATA_ROW = 1000;

    private final GroupCategoryRepository repository;
    private final DataFormatter dataFormatter = new DataFormatter(Locale.ROOT);

    @Override
    public byte[] exportExcel(GroupCategorySearchReq req) {
        try {
            Specification<GroupCategory> spec = req == null
                    ? Specification.allOf()
                    : GroupCategorySpecification.search(req);

            Sort sort = buildSort(req);
            List<GroupCategory> rows = repository.findAll(spec, sort);

            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                Sheet sheet = workbook.createSheet("group-categories");

                writeHeader(sheet);
                writeData(sheet, rows);

                for (int i = 0; i < EXPORT_HEADERS.size(); i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(outputStream);
                return outputStream.toByteArray();
            }

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.GC_SEARCH_FAILED,
                    "Xuất Excel thất bại: " + ex.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public GroupCatExcelImportResultRes importExcel(MultipartFile file, boolean submitAfterImport) {
        validateFile(file);

        List<GroupCatExcelImportErrorRes> errors = new ArrayList<>();
        List<GroupCategoryExcelRowReq> parsedRows = new ArrayList<>();
        int totalRows = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;

            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "File Excel không có dữ liệu");
            }

            Map<String, Integer> headerIndex = readHeaderIndex(sheet.getRow(sheet.getFirstRowNum()));

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (isEmptyRow(row)) {
                    continue;
                }

                totalRows++;

                if (totalRows > MAX_IMPORT_ROWS) {
                    throw new BusinessException(
                            ErrorCode.INVALID_REQUEST,
                            "File import tối đa " + MAX_IMPORT_ROWS + " dòng"
                    );
                }

                try {
                    GroupCategoryExcelRowReq excelRow = mapExcelRow(row, headerIndex, rowIndex + 1);
                    validateBusiness(excelRow);
                    parsedRows.add(excelRow);
                } catch (BusinessException ex) {
                    errors.add(new GroupCatExcelImportErrorRes(rowIndex + 1, ex.getMessage()));
                } catch (RuntimeException ex) {
                    errors.add(new GroupCatExcelImportErrorRes(
                            rowIndex + 1,
                            "Dữ liệu không hỡp lệ: " + ex.getMessage()
                    ));
                }
            }

            List<GroupCategoryExcelRowReq> afterFileDuplicate = filterDuplicateInFile(parsedRows, errors);
            List<GroupCategoryExcelRowReq> finalValidRows = filterDuplicateInDb(afterFileDuplicate, errors);

            List<GroupCategory> entities = finalValidRows.stream()
                    .map(row -> toEntity(row, submitAfterImport))
                    .toList();

            saveInBatches(entities);

            return new GroupCatExcelImportResultRes(
                    totalRows,
                    entities.size(),
                    errors.size(),
                    errors
            );

        } catch (BusinessException ex) {
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Dữ liệu import bị trùng với dữ liệu đã có trong hệ thống"
            );
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Không đọc được file Excel: " + ex.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public ByteArrayResource dowloadTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet categorySheet = workbook.createSheet("Category");
            Sheet dataSheet = workbook.createSheet("DATA");
            List<String> componentCodes = repository
                    .findDistinctComponentCodesByIsActive(GroupCategoryConstant.ACTIVE_DEFAULT)
                    .stream()
                    .filter(this::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();

            CellStyle titleStyle = createTemplateTitleStyle(workbook);
            CellStyle headerStyle = createTemplateHeaderStyle(workbook);
            CellStyle dateStyle = createTemplateDateStyle(workbook);

            writeTemplateTitle(categorySheet, titleStyle);
            writeTemplateHeaders(categorySheet, headerStyle);
            writeTemplateDataRows(categorySheet, dateStyle);
            writeTemplateComponentData(dataSheet, componentCodes);
            applyComponentValidation(categorySheet, componentCodes.size());
            applyTemplateLayout(categorySheet);

            workbook.setSheetHidden(workbook.getSheetIndex(dataSheet), true);
            workbook.write(outputStream);

            if (outputStream.size() <= 0) {
                throw new BusinessException(
                        ErrorCode.GC_CREATE_FAILED,
                        "Template import category được tạo rỗng"
                );
            }

            return new ByteArrayResource(outputStream.toByteArray());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.GC_CREATE_FAILED,
                    "Không thể tạo template import category: " + ex.getMessage()
            );
        }
    }

    private CellStyle createTemplateTitleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createTemplateHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        return style;
    }

    private CellStyle createTemplateDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper creationHelper = workbook.getCreationHelper();
        style.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy/MM/dd"));

        return style;
    }

    private void writeTemplateTitle(Sheet sheet, CellStyle titleStyle) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("TEMPLATE IMPORT CATEGORY");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, TEMPLATE_HEADERS.size() - 1));
    }

    private void writeTemplateHeaders(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(1);
        Row headerBottomRow = sheet.createRow(2);
        headerRow.setHeightInPoints(32);
        headerBottomRow.setHeightInPoints(12);

        for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
            Cell topCell = headerRow.createCell(i);
            topCell.setCellValue(TEMPLATE_HEADERS.get(i));
            topCell.setCellStyle(headerStyle);

            Cell bottomCell = headerBottomRow.createCell(i);
            bottomCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, i, i));
        }

        addDateHeaderComment(sheet, 4);
        addDateHeaderComment(sheet, 5);
    }

    private void addDateHeaderComment(Sheet sheet, int columnIndex) {
        Workbook workbook = sheet.getWorkbook();
        CreationHelper creationHelper = workbook.getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = creationHelper.createClientAnchor();
        anchor.setCol1(columnIndex);
        anchor.setCol2(columnIndex + 3);
        anchor.setRow1(1);
        anchor.setRow2(4);

        Comment comment = drawing.createCellComment(anchor);
        comment.setString(creationHelper.createRichTextString(
                "Định dạng: yyyy/MM/dd\nVí dụ: 2006/09/09"
        ));

        sheet.getRow(1).getCell(columnIndex).setCellComment(comment);
    }

    private void writeTemplateDataRows(Sheet sheet, CellStyle dateStyle) {
        for (int rowIndex = TEMPLATE_FIRST_DATA_ROW; rowIndex <= TEMPLATE_LAST_DATA_ROW; rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            row.createCell(4).setCellStyle(dateStyle);
            row.createCell(5).setCellStyle(dateStyle);
        }
    }

    private void writeTemplateComponentData(Sheet dataSheet, List<String> componentCodes) {
        for (int i = 0; i < componentCodes.size(); i++) {
            Row row = dataSheet.createRow(i);
            row.createCell(0).setCellValue(componentCodes.get(i));
            row.createCell(1).setCellValue(componentCodes.get(i));
        }

        dataSheet.setColumnWidth(0, 28 * 256);
        dataSheet.setColumnWidth(1, 28 * 256);
    }

    private void applyComponentValidation(Sheet sheet, int componentCount) {
        if (componentCount <= 0) {
            return;
        }

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(
                "'DATA'!$A$1:$A$" + componentCount
        );
        CellRangeAddressList addressList = new CellRangeAddressList(
                TEMPLATE_FIRST_DATA_ROW,
                TEMPLATE_LAST_DATA_ROW,
                3,
                3
        );
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox(
                "Dữ liệu không hợp lệ",
                "Vui lòng chọn cấu phần xử lý từ danh sách!"
        );

        sheet.addValidationData(validation);
    }

    private void applyTemplateLayout(Sheet sheet) {
        int[] widths = {24, 24, 24, 28, 18, 18, 40};

        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }

        sheet.createFreezePane(0, TEMPLATE_FIRST_DATA_ROW);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "File import không được để trống");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Tên file import không hợp lệ");
        }

        String lowerFilename = filename.toLowerCase(Locale.ROOT);

        if (!lowerFilename.endsWith(".xlsx") && !lowerFilename.endsWith(".xls")) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Chỉ hỗ trợ file Excel .xlsx hoặc .xls"
            );
        }
    }

    private Sort buildSort(GroupCategorySearchReq req) {
        Set<String> allowedSortFields = Set.of(
                "id",
                "paramName",
                "paramValue",
                "paramType",
                "description",
                "componentCode",
                "status",
                "isActive",
                "isDisplay",
                "effectiveDate",
                "endEffectiveDate"
        );

        String sortBy = req != null && hasText(req.sortBy())
                ? req.sortBy().trim()
                : "id";

        if (!allowedSortFields.contains(sortBy)) {
            sortBy = "id";
        }

        Sort.Direction direction = req != null && "asc".equalsIgnoreCase(req.sortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, sortBy);
    }

    private void writeHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < EXPORT_HEADERS.size(); i++) {
            headerRow.createCell(i).setCellValue(EXPORT_HEADERS.get(i));
        }
    }

    private void writeData(Sheet sheet, List<GroupCategory> rows) {
        int rowIndex = 1;

        for (GroupCategory item : rows) {
            Row row = sheet.createRow(rowIndex++);

            row.createCell(0).setCellValue(stringValue(item.getParamName()));
            row.createCell(1).setCellValue(stringValue(item.getParamValue()));
            row.createCell(2).setCellValue(stringValue(item.getParamType()));
            row.createCell(3).setCellValue(stringValue(item.getDescription()));
            row.createCell(4).setCellValue(stringValue(item.getComponentCode()));

            setIntegerCell(row, 5, item.getStatus());
            setIntegerCell(row, 6, item.getIsActive());
            setIntegerCell(row, 7, item.getIsDisplay());

            row.createCell(8).setCellValue(dateValue(item.getEffectiveDate()));
            row.createCell(9).setCellValue(dateValue(item.getEndEffectiveDate()));
        }
    }

    private void setIntegerCell(Row row, int columnIndex, Integer value) {
        Cell cell = row.createCell(columnIndex);

        if (value == null) {
            cell.setBlank();
            return;
        }

        cell.setCellValue(value);
    }

    private Map<String, Integer> readHeaderIndex(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "File Excel thiếu dòng header");
        }

        Map<String, Integer> headerIndex = new HashMap<>();

        for (Cell cell : headerRow) {
            String normalized = normalizeHeader(dataFormatter.formatCellValue(cell));

            if (!normalized.isEmpty()) {
                headerIndex.put(normalized, cell.getColumnIndex());
            }
        }

        for (String required : List.of("paramname", "paramvalue", "paramtype", "effectivedate")) {
            if (!headerIndex.containsKey(required)) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Header Excel thiếu cột bắt buộc: " + required
                );
            }
        }

        return headerIndex;
    }

    private GroupCategoryExcelRowReq mapExcelRow(
            Row row,
            Map<String, Integer> headerIndex,
            int rowNumber
    ) {
        return new GroupCategoryExcelRowReq(
                rowNumber,
                readString(row, headerIndex, "paramname"),
                readString(row, headerIndex, "paramvalue"),
                readString(row, headerIndex, "paramtype"),
                readString(row, headerIndex, "description"),
                readString(row, headerIndex, "componentcode"),
                readInteger(row, headerIndex, "isactive"),
                readInteger(row, headerIndex, "isdisplay"),
                readDate(row, headerIndex, "effectivedate"),
                readDate(row, headerIndex, "endeffectivedate")
        );
    }

    private void validateBusiness(GroupCategoryExcelRowReq row) {
        if (!hasText(row.paramName())) {
            throw new IllegalArgumentException("Param Name không được để trống");
        }

        if (!hasText(row.paramValue())) {
            throw new IllegalArgumentException("Param Value không được để trống");
        }

        if (!hasText(row.paramType())) {
            throw new IllegalArgumentException("Param Type không được để trống");
        }

        if (row.effectiveDate() == null) {
            throw new IllegalArgumentException("Effective Date không được để trống");
        }

        if (row.endEffectiveDate() != null && row.endEffectiveDate().isBefore(row.effectiveDate())) {
            throw new IllegalArgumentException("End Effective Date phải lớn hơn hoặc bằng Effective Date");
        }

        if (
                row.isActive() != null
                        && row.isActive() != 0
                        && row.isActive() != 1
        ) {
            throw new IllegalArgumentException("Is Active chỉ nhận 0 hoặc 1");
        }

        if (
                row.isDisplay() != null
                        && !row.isDisplay().equals(GroupCategoryConstant.DISPLAY_HIDDEN)
                        && !row.isDisplay().equals(GroupCategoryConstant.DISPLAY_VISIBLE)
        ) {
            throw new IllegalArgumentException("Is Display chỉ nhận 1 hoặc 2");
        }
    }

    private List<GroupCategoryExcelRowReq> filterDuplicateInFile(
            List<GroupCategoryExcelRowReq> rows,
            List<GroupCatExcelImportErrorRes> errors
    ) {
        Map<String, Integer> firstSeenRowByKey = new HashMap<>();
        List<GroupCategoryExcelRowReq> result = new ArrayList<>();

        for (GroupCategoryExcelRowReq row : rows) {
            String key = buildUniqueKey(row.paramName(), row.paramValue(), row.paramType());

            if (firstSeenRowByKey.containsKey(key)) {
                errors.add(new GroupCatExcelImportErrorRes(
                        row.rowNumber(),
                        "Trùng dữ liệu trong file vỡi dòng " + firstSeenRowByKey.get(key)
                ));
                continue;
            }

            firstSeenRowByKey.put(key, row.rowNumber());
            result.add(row);
        }

        return result;
    }

    private List<GroupCategoryExcelRowReq> filterDuplicateInDb(
            List<GroupCategoryExcelRowReq> rows,
            List<GroupCatExcelImportErrorRes> errors
    ) {
        if (rows.isEmpty()) {
            return rows;
        }

        Set<String> paramNames = rows.stream()
                .map(GroupCategoryExcelRowReq::paramName)
                .filter(this::hasText)
                .map(this::normalize)
                .collect(Collectors.toSet());

        Set<String> paramValues = rows.stream()
                .map(GroupCategoryExcelRowReq::paramValue)
                .filter(this::hasText)
                .map(this::normalize)
                .collect(Collectors.toSet());

        Set<String> paramTypes = rows.stream()
                .map(GroupCategoryExcelRowReq::paramType)
                .filter(this::hasText)
                .map(this::normalize)
                .collect(Collectors.toSet());

        List<GroupCategory> existing = repository.findForImportDuplicateCheck(
                paramNames,
                paramValues,
                paramTypes
        );

        Set<String> existingKeys = existing.stream()
                .map(item -> buildUniqueKey(
                        item.getParamName(),
                        item.getParamValue(),
                        item.getParamType()
                ))
                .collect(Collectors.toSet());

        List<GroupCategoryExcelRowReq> result = new ArrayList<>();

        for (GroupCategoryExcelRowReq row : rows) {
            String key = buildUniqueKey(row.paramName(), row.paramValue(), row.paramType());

            if (existingKeys.contains(key)) {
                errors.add(new GroupCatExcelImportErrorRes(
                        row.rowNumber(),
                        "Dữ liệu đã tồn tại trong hệ thống"
                ));
                continue;
            }

            result.add(row);
        }

        return result;
    }

    private GroupCategory toEntity(GroupCategoryExcelRowReq row, boolean submitAfterImport) {
        GroupCategory entity = new GroupCategory();

        entity.setParamName(trimToNull(row.paramName()));
        entity.setParamValue(trimToNull(row.paramValue()));
        entity.setParamType(trimToNull(row.paramType()));
        entity.setDescription(trimToNull(row.description()));
        entity.setComponentCode(trimToNull(row.componentCode()));

        entity.setEffectiveDate(row.effectiveDate());
        entity.setEndEffectiveDate(row.endEffectiveDate());

        entity.setIsActive(
                row.isActive() == null
                        ? GroupCategoryConstant.ACTIVE_DEFAULT
                        : row.isActive()
        );

        entity.setIsDisplay(
                row.isDisplay() == null
                        ? GroupCategoryConstant.DISPLAY_HIDDEN
                        : row.isDisplay()
        );

        entity.setNewData(null);

        entity.setStatus(
                submitAfterImport
                        ? GroupCategoryConstant.STATUS_PENDING
                        : GroupCategoryConstant.STATUS_DRAFT
        );

        return entity;
    }

    private void saveInBatches(List<GroupCategory> entities) {
        if (entities.isEmpty()) {
            return;
        }

        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, entities.size());
            List<GroupCategory> batch = entities.subList(i, end);

            repository.saveAll(batch);
            repository.flush();
        }
    }

    private String readString(Row row, Map<String, Integer> headerIndex, String key) {
        Integer columnIndex = headerIndex.get(key);

        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex);

        return cell == null ? null : dataFormatter.formatCellValue(cell).trim();
    }

    private Integer readInteger(Row row, Map<String, Integer> headerIndex, String key) {
        String value = readString(row, headerIndex, key);

        if (!hasText(value)) {
            return null;
        }

        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cột " + key + " phải là số nguyên");
        }
    }

    private LocalDate readDate(Row row, Map<String, Integer> headerIndex, String key) {
        Integer columnIndex = headerIndex.get(key);

        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex);

        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }

            throw new IllegalArgumentException(
                    "Cột " + key + " phải là ngày hợp lệ, không được là số thường"
            );
        }

        String rawValue = dataFormatter.formatCellValue(cell).trim();

        if (!hasText(rawValue)) {
            return null;
        }

        for (DateTimeFormatter formatter : IMPORT_DATE_FORMATS) {
            try {
                return LocalDate.parse(rawValue, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }

        throw new IllegalArgumentException(
                "Cột " + key + " Phải theo định dạng yyyy-MM-dd hoac dd/MM/yyyy"
        );
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }

        short firstCellNum = row.getFirstCellNum();
        short lastCellNum = row.getLastCellNum();

        if (firstCellNum < 0 || lastCellNum < 0) {
            return true;
        }

        for (int i = firstCellNum; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);

            if (cell != null && hasText(dataFormatter.formatCellValue(cell))) {
                return false;
            }
        }

        return true;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }

        String compact = header.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");

        Map<String, String> aliases = new HashMap<>();
        aliases.put("paramname", "paramname");
        aliases.put("paramvalue", "paramvalue");
        aliases.put("paramtype", "paramtype");
        aliases.put("description", "description");
        aliases.put("componentcode", "componentcode");
        aliases.put("status", "status");
        aliases.put("isactive", "isactive");
        aliases.put("isdisplay", "isdisplay");
        aliases.put("effectivedate", "effectivedate");
        aliases.put("endeffectivedate", "endeffectivedate");

        return aliases.getOrDefault(compact, compact);
    }

    private String buildUniqueKey(String paramName, String paramValue, String paramType) {
        return normalize(paramName)
                + "|"
                + normalize(paramValue)
                + "|"
                + normalize(paramType);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private String stringValue(String value) {
        return value == null ? "" : value;
    }

    private String dateValue(LocalDate value) {
        return value == null ? "" : value.format(EXPORT_DATE_FORMAT);
    }
}
