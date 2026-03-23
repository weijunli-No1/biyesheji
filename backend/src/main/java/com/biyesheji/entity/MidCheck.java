package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mid_check")
public class MidCheck {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private Long teacherId;
    private String completedWork;
    private String problems;
    private String nextPlan;
    /** 1正常 2滞后 3严重滞后 */
    private Integer progress;
    /** 0未提交 1待导师审核 2已通过 3已退回 */
    private Integer status;
    private String teacherComment;
    private LocalDateTime submitTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
