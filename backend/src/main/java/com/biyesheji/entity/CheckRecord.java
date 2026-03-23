package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("check_record")
public class CheckRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long selectionId;
    private Long studentId;
    private String fileUrl;
    private LocalDateTime checkTime;
    private BigDecimal similarity;
    private String reportUrl;
    private String engine;
    /** 0待检测 1已通过 2未通过 */
    private Integer status;
    private BigDecimal threshold;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
