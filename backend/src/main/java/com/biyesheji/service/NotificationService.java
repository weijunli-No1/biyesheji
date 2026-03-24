package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.common.PageResult;
import com.biyesheji.common.Result;
import com.biyesheji.entity.Notification;
import com.biyesheji.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务
 * 负责创建通知、持久化到数据库，并通过 SSE 实时推送给在线用户。
 * 所有 send 方法均为 @Async，不阻塞主业务线程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final SseService sseService;
    private final MailService mailService;

    // ─────────────────────────────── 核心发送 ───────────────────────────────

    /**
     * 发送通知（持久化 + SSE 推送 + 可选邮件）
     */
    @Async
    public void send(Long userId, String type, String title, String content,
                     Long relatedId, String relatedType, String relatedUrl) {
        Notification n = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .relatedUrl(relatedUrl)
                .isRead(false)
                .createTime(LocalDateTime.now())
                .build();
        notificationMapper.insert(n);

        // SSE 实时推送（用户在线才生效）
        sseService.pushNotification(userId, n);

        // 邮件通知（由 MailService 判断是否启用）
        mailService.sendNotificationEmail(userId, title, content);
    }

    // ─────────────────────────────── 业务快捷方法 ───────────────────────────────

    /** 课题审批通过 → 通知教师 */
    public void topicApproved(Long teacherId, Long topicId, String topicTitle) {
        send(teacherId, "TOPIC_APPROVED",
                "课题审批通过",
                String.format("您申报的课题「%s」已通过审批，已正式发布。", topicTitle),
                topicId, "TOPIC", "/topics/my");
    }

    /** 课题被驳回 → 通知教师 */
    public void topicRejected(Long teacherId, Long topicId, String topicTitle, String reason) {
        send(teacherId, "TOPIC_REJECTED",
                "课题审批未通过",
                String.format("您申报的课题「%s」被驳回，原因：%s", topicTitle, reason),
                topicId, "TOPIC", "/topics/my");
    }

    /** 学生选题申请 → 通知导师 */
    public void selectionApplied(Long teacherId, Long selectionId, String studentName, String topicTitle) {
        send(teacherId, "SELECTION_APPLIED",
                "新的选题申请",
                String.format("学生「%s」申请了您的课题「%s」，请及时确认。", studentName, topicTitle),
                selectionId, "SELECTION", "/selections");
    }

    /** 选题被确认 → 通知学生 */
    public void selectionConfirmed(Long studentId, Long selectionId, String topicTitle) {
        send(studentId, "SELECTION_CONFIRMED",
                "选题已确认",
                String.format("您申请的课题「%s」已被导师确认，请尽快开始撰写开题报告。", topicTitle),
                selectionId, "SELECTION", "/proposal");
    }

    /** 选题被拒绝 → 通知学生 */
    public void selectionRejected(Long studentId, Long selectionId, String topicTitle) {
        send(studentId, "SELECTION_REJECTED",
                "选题申请被拒绝",
                String.format("您对课题「%s」的申请被导师拒绝，请重新选题。", topicTitle),
                selectionId, "SELECTION", "/topics");
    }

    /** 开题报告提交 → 通知导师 */
    public void proposalSubmitted(Long teacherId, Long proposalId, String studentName) {
        send(teacherId, "PROPOSAL_SUBMITTED",
                "开题报告待审核",
                String.format("学生「%s」已提交开题报告，请尽快审核。", studentName),
                proposalId, "PROPOSAL", "/proposals");
    }

    /** 开题报告导师通过 → 通知学生 */
    public void proposalTeacherPassed(Long studentId, Long proposalId) {
        send(studentId, "PROPOSAL_TEACHER_PASSED",
                "开题报告通过导师审核",
                "您的开题报告已通过导师审核，正在等待部门评审。",
                proposalId, "PROPOSAL", "/proposal");
    }

    /** 开题报告导师退回 → 通知学生 */
    public void proposalTeacherRejected(Long studentId, Long proposalId, String reason) {
        send(studentId, "PROPOSAL_TEACHER_REJECTED",
                "开题报告被退回",
                String.format("您的开题报告被导师退回，退回原因：%s，请修改后重新提交。", reason),
                proposalId, "PROPOSAL", "/proposal");
    }

    /** 开题报告评审通过 → 通知学生 */
    public void proposalDeptPassed(Long studentId, Long proposalId, Integer score) {
        send(studentId, "PROPOSAL_DEPT_PASSED",
                "开题报告评审通过",
                String.format("您的开题报告已通过部门评审，开题评分 %d 分，请继续推进论文工作。", score),
                proposalId, "PROPOSAL", "/proposal");
    }

    /** 开题报告评审退回 → 通知学生 */
    public void proposalDeptRejected(Long studentId, Long proposalId, String reason) {
        send(studentId, "PROPOSAL_DEPT_REJECTED",
                "开题报告评审未通过",
                String.format("您的开题报告评审未通过，原因：%s，请修改后重新提交。", reason),
                proposalId, "PROPOSAL", "/proposal");
    }

    /** 中期检查提交 → 通知导师 */
    public void midCheckSubmitted(Long teacherId, Long checkId, String studentName) {
        send(teacherId, "MIDCHECK_SUBMITTED",
                "中期检查待审核",
                String.format("学生「%s」已提交中期检查报告，请尽快审核。", studentName),
                checkId, "MID_CHECK", "/midchecks");
    }

    /** 中期检查审核通过 → 通知学生 */
    public void midCheckPassed(Long studentId, Long checkId) {
        send(studentId, "MIDCHECK_PASSED",
                "中期检查已通过",
                "您的中期检查报告已通过导师审核，请继续按计划推进论文工作。",
                checkId, "MID_CHECK", "/midcheck");
    }

    /** 中期检查退回 → 通知学生 */
    public void midCheckRejected(Long studentId, Long checkId, String comment) {
        send(studentId, "MIDCHECK_REJECTED",
                "中期检查被退回",
                String.format("您的中期检查报告被导师退回，意见：%s，请修改后重新提交。", comment),
                checkId, "MID_CHECK", "/midcheck");
    }

    /** 论文提交 → 通知导师 */
    public void thesisSubmitted(Long teacherId, Long thesisId, String studentName, int version) {
        send(teacherId, "THESIS_SUBMITTED",
                "论文版本待审阅",
                String.format("学生「%s」提交了第 %d 版论文，请尽快审阅。", studentName, version),
                thesisId, "THESIS", "/thesis/review");
    }

    /** 论文审阅通过 → 通知学生 */
    public void thesisPassed(Long studentId, Long thesisId, int version) {
        send(studentId, "THESIS_PASSED",
                "论文审阅通过",
                String.format("您提交的第 %d 版论文已通过导师审阅，可继续提交新版本或等待答辩安排。", version),
                thesisId, "THESIS", "/thesis");
    }

    /** 论文被退回 → 通知学生 */
    public void thesisRejected(Long studentId, Long thesisId, int version, String comment) {
        send(studentId, "THESIS_REJECTED",
                "论文需修改",
                String.format("您的第 %d 版论文被导师退回，修改意见：%s，请修改后重新提交。", version, comment),
                thesisId, "THESIS", "/thesis");
    }

    /** 导师添加批注 → 通知学生 */
    public void thesisAnnotated(Long studentId, Long thesisId, String teacherName, int pageNo) {
        send(studentId, "THESIS_ANNOTATED",
                "导师添加了论文批注",
                String.format("导师「%s」在您论文第 %d 页添加了批注，请查阅并处理。", teacherName, pageNo),
                thesisId, "THESIS", "/thesis");
    }

    /** 分配答辩组 → 通知学生 */
    public void defenseAssigned(Long studentId, String groupName, String location, String defenseTime) {
        send(studentId, "DEFENSE_ASSIGNED",
                "答辩安排已确定",
                String.format("您已被分配到答辩组「%s」，答辩地点：%s，答辩时间：%s，请提前准备。",
                        groupName, location, defenseTime),
                null, "DEFENSE", "/defense/my");
    }

    /** 成绩录入完成 → 通知学生 */
    public void scoreRecorded(Long studentId, Integer totalScore) {
        send(studentId, "SCORE_RECORDED",
                "综合成绩已录入",
                String.format("您的毕业设计综合成绩已录入，总分 %d 分，请前往成绩页面查看详情。", totalScore),
                null, "SCORE", "/score/my");
    }

    // ─────────────────────────────── 查询接口 ───────────────────────────────

    public Result<PageResult<Notification>> list(Long userId, int page, int size) {
        Page<Notification> p = notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
        return Result.ok(PageResult.of(p));
    }

    public Result<Long> unreadCount(Long userId) {
        long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false));
        return Result.ok(count);
    }

    public Result<?> markRead(Long notifId, Long userId) {
        Notification n = notificationMapper.selectById(notifId);
        if (n == null || !n.getUserId().equals(userId)) return Result.fail(403, "无权操作");
        n.setIsRead(true);
        notificationMapper.updateById(n);
        return Result.ok();
    }

    public Result<?> markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
        return Result.ok();
    }

    public Result<List<Notification>> recent(Long userId) {
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, false)
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 10"));
        return Result.ok(list);
    }
}
