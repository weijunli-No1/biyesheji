package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("guidance_log")
public class GuidanceLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private Long teacherId;
    private LocalDateTime guideTime;
    private String content;
    private String studentWork;
    private String nextPlan;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
