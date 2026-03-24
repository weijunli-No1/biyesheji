package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者 */
    private Long userId;

    /**
     * 通知类型常量（也可提取为独立枚举）
     * TOPIC_APPROVED / TOPIC_REJECTED
     * SELECTION_CONFIRMED / SELECTION_REJECTED
     * PROPOSAL_SUBMITTED / PROPOSAL_TEACHER_PASSED / PROPOSAL_TEACHER_REJECTED
     * PROPOSAL_DEPT_PASSED / PROPOSAL_DEPT_REJECTED
     * MIDCHECK_SUBMITTED / MIDCHECK_PASSED / MIDCHECK_REJECTED
     * THESIS_SUBMITTED / THESIS_REVIEWED / THESIS_PASSED / THESIS_REJECTED
     * DEFENSE_ASSIGNED / DEFENSE_SCORED
     * SYSTEM_NOTICE
     */
    private String type;

    private String title;
    private String content;

    /** 关联业务主键（跳转时使用） */
    private Long relatedId;
    private String relatedType;

    /** 前端路由（如 /proposal、/thesis） */
    private String relatedUrl;

    private Boolean isRead;
    private LocalDateTime createTime;
}
