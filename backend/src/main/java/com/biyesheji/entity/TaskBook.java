package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_book")
public class TaskBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private Long teacherId;
    private String content;
    private String references;
    private String schedule;
    private String fileUrl;
    /** 0草稿 1已下达 2学生已确认 */
    private Integer status;
    private LocalDateTime issueTime;
    private LocalDateTime confirmTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
