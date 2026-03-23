package com.biyesheji.dto;

import com.biyesheji.entity.TopicSelection;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SelectionVO extends TopicSelection {
    private String topicTitle;
    private Integer topicType;
    private String studentName;
    private String studentNo;
    private String studentEmail;
    private String teacherName;
}
