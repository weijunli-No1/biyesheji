package com.biyesheji.dto;

import com.biyesheji.entity.Topic;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TopicVO extends Topic {
    private String teacherName;
    private Integer selectedCount;
    /** 当前用户是否已申请 */
    private Boolean applied;
}
