package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.entity.ImportRecord;
import com.biyesheji.entity.User;
import com.biyesheji.mapper.ImportRecordMapper;
import com.biyesheji.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 批量导入服务
 *
 * 支持导入类型：
 *   USER_STUDENT  - 批量导入学生（列：学号/姓名/学院ID/专业ID/班级ID/邮箱/手机号）
 *   USER_TEACHER  - 批量导入教师（列：工号/姓名/学院ID/专业ID/职称/邮箱/手机号）
 *   ORG_CLASS     - 预留（班级批量导入）
 *
 * Excel 首行为表头，从第2行开始读取数据。
 * 默认密码：学号/工号（明文），系统用 BCrypt 加密存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final UserMapper userMapper;
    private final ImportRecordMapper importRecordMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    /**
     * 通用入口：解析 Excel 并执行批量导入，返回导入记录
     */
    @Transactional
    public Result<ImportRecord> importUsers(MultipartFile file, String type,
                                            Integer year, Long operatorId) throws Exception {
        ImportRecord record = new ImportRecord();
        record.setType(type);
        record.setYear(year);
        record.setFileName(file.getOriginalFilename());
        record.setOperatorId(operatorId);
        record.setStatus(0); // 处理中
        record.setCreateTime(LocalDateTime.now());
        importRecordMapper.insert(record);

        List<Map<String, Object>> errors = new ArrayList<>();
        int success = 0;

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                try {
                    User user = "USER_STUDENT".equals(type)
                            ? parseStudentRow(row, year)
                            : parseTeacherRow(row);

                    // 检查用户名是否已存在
                    long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, user.getUsername()));
                    if (exists > 0) {
                        errors.add(Map.of("row", i + 1,
                                "value", user.getUsername(),
                                "error", "学号/工号已存在，已跳过"));
                        continue;
                    }
                    userMapper.insert(user);
                    success++;
                } catch (Exception e) {
                    errors.add(Map.of("row", i + 1, "error", e.getMessage()));
                }
            }
        }

        record.setTotalCount(success + errors.size());
        record.setSuccessCount(success);
        record.setFailCount(errors.size());
        record.setStatus(errors.isEmpty() ? 1 : 2);
        record.setErrorDetail(objectMapper.writeValueAsString(errors));
        record.setFinishTime(LocalDateTime.now());
        importRecordMapper.updateById(record);

        return Result.ok(record);
    }

    // ─── 解析学生行 ───────────────────────────────
    // 列序：学号 | 姓名 | 学院ID | 专业ID | 班级ID | 邮箱 | 手机号
    private User parseStudentRow(Row row, Integer year) {
        String studentNo = getCellStr(row, 0);
        String realName  = getCellStr(row, 1);
        if (studentNo.isBlank()) throw new IllegalArgumentException("学号不能为空");
        if (realName.isBlank())  throw new IllegalArgumentException("姓名不能为空");

        User u = new User();
        u.setUsername(studentNo);
        u.setRealName(realName);
        u.setRole(1); // 学生
        u.setPassword(passwordEncoder.encode(studentNo)); // 默认密码=学号
        u.setCollegeId(getCellLong(row, 2));
        u.setMajorId(getCellLong(row, 3));
        u.setClassId(getCellLong(row, 4));
        u.setEmail(getCellStr(row, 5));
        u.setPhone(getCellStr(row, 6));
        u.setStatus(1);
        return u;
    }

    // ─── 解析教师行 ───────────────────────────────
    // 列序：工号 | 姓名 | 学院ID | 专业ID | 职称 | 邮箱 | 手机号
    private User parseTeacherRow(Row row) {
        String teacherNo = getCellStr(row, 0);
        String realName  = getCellStr(row, 1);
        if (teacherNo.isBlank()) throw new IllegalArgumentException("工号不能为空");
        if (realName.isBlank())  throw new IllegalArgumentException("姓名不能为空");

        User u = new User();
        u.setUsername(teacherNo);
        u.setRealName(realName);
        u.setRole(2); // 指导教师（可按实际调整）
        u.setPassword(passwordEncoder.encode(teacherNo));
        u.setCollegeId(getCellLong(row, 2));
        u.setMajorId(getCellLong(row, 3));
        u.setTitle(getCellStr(row, 4));
        u.setEmail(getCellStr(row, 5));
        u.setPhone(getCellStr(row, 6));
        u.setStatus(1);
        return u;
    }

    // ─── 工具方法（用 DataFormatter 避免 deprecated setCellType）─────────────
    private static final DataFormatter FORMATTER = new DataFormatter();

    private String getCellStr(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        String v = FORMATTER.formatCellValue(cell).trim();
        return v;
    }

    private Long getCellLong(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (long) cell.getNumericCellValue();
            }
            String v = FORMATTER.formatCellValue(cell).trim();
            return v.isBlank() ? null : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    /** 查询导入历史 */
    public Result<List<ImportRecord>> history(Long operatorId) {
        List<ImportRecord> list = importRecordMapper.selectList(
                new LambdaQueryWrapper<ImportRecord>()
                        .eq(operatorId != null, ImportRecord::getOperatorId, operatorId)
                        .orderByDesc(ImportRecord::getCreateTime)
                        .last("LIMIT 20"));
        return Result.ok(list);
    }
}
