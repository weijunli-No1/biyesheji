package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("proposal")
public class Proposal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private Long teacherId;
    private String background;
    private String literature;
    private String method;
    private String plan;
    private String expectedResult;
    private String fileUrl;
    private String translationUrl;
    /** 0草稿 1待导师审核 2待评审 3已通过 4已退回 */
    private Integer status;
    private String teacherComment;
    private String reviewComment;
    private Integer reviewScore;
    private String rejectReason;
    private LocalDateTime submitTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
