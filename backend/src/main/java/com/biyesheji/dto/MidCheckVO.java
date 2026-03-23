package com.biyesheji.dto;

import com.biyesheji.entity.MidCheck;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MidCheckVO extends MidCheck {
    private String studentName;
    private String studentNo;
    private String topicTitle;
}
