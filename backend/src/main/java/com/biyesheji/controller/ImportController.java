package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
@Tag(name = "批量导入")
public class ImportController {

    private final ImportService importService;

    /**
     * 批量导入学生（Excel）
     * 权限：专业管理员 / 学院管理员 / 教务管理员
     */
    @PostMapping("/students")
    @Operation(summary = "批量导入学生")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    public Result<?> importStudents(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") Integer year,
            @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        int y = year == 0 ? java.time.LocalDate.now().getYear() : year;
        return importService.importUsers(file, "USER_STUDENT", y, user.getUser().getId());
    }

    /**
     * 批量导入教师（Excel）
     */
    @PostMapping("/teachers")
    @Operation(summary = "批量导入教师")
    @PreAuthorize("hasAnyRole('COLLEGE_ADMIN','ADMIN')")
    public Result<?> importTeachers(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        return importService.importUsers(file, "USER_TEACHER", null, user.getUser().getId());
    }

    /**
     * 查询导入历史（最近20条）
     */
    @GetMapping("/history")
    @Operation(summary = "导入历史")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    public Result<?> history(@AuthenticationPrincipal UserDetailsImpl user) {
        return importService.history(user.getUser().getId());
    }

    /**
     * 下载导入模板
     * type: student | teacher
     */
    @GetMapping("/template/{type}")
    @Operation(summary = "下载导入模板")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String type) throws Exception {
        String filename = "student".equals(type) ? "template_student.xlsx" : "template_teacher.xlsx";
        ClassPathResource resource = new ClassPathResource("templates/" + filename);
        if (!resource.exists()) {
            // 动态生成简易模板
            byte[] bytes = generateTemplate(type);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        }
        byte[] bytes = resource.getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    private byte[] generateTemplate(String type) throws Exception {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("导入数据");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);

            // 创建标题样式
            org.apache.poi.ss.usermodel.CellStyle style = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = wb.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
            style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            String[] studentHeaders = {"学号*", "姓名*", "学院ID", "专业ID", "班级ID", "邮箱", "手机号"};
            String[] teacherHeaders = {"工号*", "姓名*", "学院ID", "专业ID", "职称", "邮箱", "手机号"};
            String[] headers = "student".equals(type) ? studentHeaders : teacherHeaders;

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(style);
                sheet.setColumnWidth(i, 4000);
            }

            // 示例行
            org.apache.poi.ss.usermodel.Row example = sheet.createRow(1);
            if ("student".equals(type)) {
                example.createCell(0).setCellValue("2024001");
                example.createCell(1).setCellValue("张三");
                example.createCell(2).setCellValue(1);
                example.createCell(3).setCellValue(1);
                example.createCell(4).setCellValue(1);
                example.createCell(5).setCellValue("zhangsan@example.com");
                example.createCell(6).setCellValue("13800138001");
            } else {
                example.createCell(0).setCellValue("T20240001");
                example.createCell(1).setCellValue("李教授");
                example.createCell(2).setCellValue(1);
                example.createCell(3).setCellValue(1);
                example.createCell(4).setCellValue("副教授");
                example.createCell(5).setCellValue("liteacher@example.com");
                example.createCell(6).setCellValue("13800138002");
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
