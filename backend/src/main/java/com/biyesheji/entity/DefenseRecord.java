package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("defense_record")
public class DefenseRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private Long groupId;
    private LocalDateTime defenseTime;
    private Integer defenseRound;
    private Integer presentScore;
    private Integer qaScore;
    private Integer totalScore;
    /** 1通过 2修改后通过 3不通过 */
    private Integer result;
    /** 修改后通过时，管理员确认修改完成：0=未确认 1=已确认 */
    private Integer revisionConfirmed;
    private String questions;
    private String comment;
    private String secretaryNote;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
