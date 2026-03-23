package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("thesis_version")
public class ThesisVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private Long teacherId;
    private Integer version;
    /** 1初稿 2修改稿 3送审稿 4定稿 */
    private Integer versionType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String comment;
    /** 0待审阅 1已退回 2已通过 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
