package com.example.demo.groupcategory.service;

import com.example.demo.groupcategory.dto.excel.GroupCatExcelImportResultRes;
import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

public interface GroupCategoryExcelService {
    byte[] exportExcel(GroupCategorySearchReq req);
    GroupCatExcelImportResultRes importExcel(MultipartFile file, boolean submitAfterImport);
    ByteArrayResource dowloadTemplate();
}
