package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("topic")
public class Topic {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    /** 1设计类 2论文类 3软件类 4实验类 */
    private Integer type;
    private String description;
    private String requirement;
    private Integer maxStudents;
    private Long teacherId;
    private Long collegeId;
    private Long majorId;
    private Integer year;
    /** 0待审批 1已发布 2已关闭 3已驳回 */
    private Integer status;
    private String rejectReason;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
