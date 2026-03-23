package com.biyesheji.dto;

import com.biyesheji.entity.Proposal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProposalVO extends Proposal {
    private String studentName;
    private String studentNo;
    private String topicTitle;
    private String teacherName;
}
