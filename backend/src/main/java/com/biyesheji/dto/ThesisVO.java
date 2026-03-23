package com.biyesheji.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ThesisVO {
    private Long id;
    private Long selectionId;
    private Long studentId;
    private String studentName;
    private String topicTitle;
    private Integer version;
    private Integer versionType;
    private String fileUrl;
    private String fileName;
    private String comment;
    /** 0待审阅 1已退回 2已通过 */
    private Integer status;
    private LocalDateTime createTime;
}
