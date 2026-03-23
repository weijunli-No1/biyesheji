package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("score")
public class Score {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private Long teacherId;
    private Integer proposalScore;
    private Integer teacherScore;
    private Integer reviewScore;
    private Integer defenseScore;
    private BigDecimal totalScore;
    private String grade;
    private Integer isExcellent;
    private Integer isLocked;
    private String teacherComment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
