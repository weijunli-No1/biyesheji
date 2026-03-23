package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("workflow_config")
public class WorkflowConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String stage;
    private String stageName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer year;
    /** 0=全局，>0 表示具体学院ID */
    private Long collegeId;
    /** 0=全局或学院级，>0 表示具体专业ID */
    private Long majorId;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
