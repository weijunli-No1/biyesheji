package com.biyesheji.dto;

import com.biyesheji.entity.Score;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScoreVO extends Score {
    private String studentName;
    private String studentNo;
    private Long collegeId;
    private Long majorId;
    private String topicTitle;
    private String teacherName;
    // 统计字段
    private Long total;
    private Long excellent;
    private Long good;
    private Long medium;
    private Long pass;
    private Long fail;
    private BigDecimal average;
}
