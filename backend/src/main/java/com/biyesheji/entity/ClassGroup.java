package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("class_group")
public class ClassGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long majorId;
    private Long collegeId;
    private Integer enrollYear;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
