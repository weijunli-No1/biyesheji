package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("thesis_annotation")
public class ThesisAnnotation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属论文版本 */
    private Long thesisId;

    private Long authorId;
    private String authorName;

    /** PDF 页码（从 1 开始） */
    private Integer pageNo;

    /** 批注内容 */
    private String content;

    /** 高亮颜色（十六进制） */
    private String color;

    /** 是否已解决（导师标记批注已处理） */
    private Boolean resolved;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
