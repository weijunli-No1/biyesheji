package com.biyesheji.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DefenseStudentVO {
    private Long recordId;
    private Long selectionId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String topicTitle;
    private Integer presentScore;
    private Integer qaScore;
    private Integer totalScore;
    /** 1通过 2修改后通过 3不通过 */
    private Integer result;
    private String questions;
    private String comment;
    private String secretaryNote;
    private LocalDateTime defenseTime;
}
