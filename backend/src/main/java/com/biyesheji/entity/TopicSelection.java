package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("topic_selection")
public class TopicSelection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long topicId;
    private Long teacherId;
    private Long collegeId;
    private Long majorId;
    private Integer year;
    /** 0待确认 1已确认 2已拒绝 3已解除 */
    private Integer status;
    private LocalDateTime applyTime;
    private LocalDateTime confirmTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
