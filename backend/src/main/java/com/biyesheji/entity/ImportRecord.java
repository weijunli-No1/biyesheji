package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("import_record")
public class ImportRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 导入类型
     * USER_STUDENT  - 批量导入学生
     * USER_TEACHER  - 批量导入教师
     * ORG_CLASS     - 批量导入班级
     */
    private String type;

    /** 届次（导入学生时必填） */
    private Integer year;

    private String fileName;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;

    /** 0处理中 1完成 2部分失败 */
    private Integer status;

    /** 失败明细 JSON Array: [{row: 3, error: "学号重复"},...] */
    private String errorDetail;

    private Long operatorId;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
