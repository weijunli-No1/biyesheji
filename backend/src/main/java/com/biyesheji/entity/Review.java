package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private Long reviewerId;
    private String thesisUrl;
    private Integer contentScore;
    private Integer methodScore;
    private Integer innovationScore;
    private Integer writingScore;
    private Integer totalScore;
    private String comment;
    /** 0待评阅 1已完成 2已退回 */
    private Integer status;
    /** 1允许答辩 0不允许 */
    private Integer canDefense;
    private LocalDateTime reviewTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
