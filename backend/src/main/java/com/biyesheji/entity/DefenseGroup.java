package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("defense_group")
public class DefenseGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer year;
    private Long collegeId;
    private Long majorId;
    private String location;
    private LocalDateTime defenseTime;
    private Long leaderId;
    private Long secretaryId;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
