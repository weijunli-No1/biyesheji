package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.entity.Topic;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
@Tag(name = "课题管理")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "课题列表（分页）")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Integer year,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) Long teacherId,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        int role = Optional.ofNullable(user).map(u -> u.getUser().getRole()).orElse(0);
        Long collegeId = (role == 6) ? user.getUser().getCollegeId() : null;
        Long majorId   = (role == 5) ? user.getUser().getMajorId()   : null;
        return topicService.listTopics(page, size, year, keyword, status, teacherId, collegeId, majorId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "课题详情")
    public Result<?> get(@PathVariable Long id) {
        return topicService.getTopic(id);
    }

    /** 教师申报新课题（insert） */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','MAJOR_ADMIN','COLLEGE_ADMIN')")
    @Operation(summary = "申报课题（教师）")
    public Result<?> apply(@RequestBody Topic topic,
                           @AuthenticationPrincipal UserDetailsImpl user) {
        // 从申报人信息自动填充学院/专业，确保管理员能按范围过滤
        if (topic.getCollegeId() == null || topic.getCollegeId() == 0) {
            topic.setCollegeId(user.getUser().getCollegeId());
        }
        if (topic.getMajorId() == null || topic.getMajorId() == 0) {
            topic.setMajorId(user.getUser().getMajorId());
        }
        return topicService.applyTopic(topic, user.getUser().getId());
    }

    /**
     * 修改课题（update）
     * 仅允许修改自己的、且处于待审批(0)或已驳回(3)状态的课题
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','MAJOR_ADMIN','COLLEGE_ADMIN')")
    @Operation(summary = "修改课题（仅待审批/已驳回可修改）")
    public Result<?> update(@PathVariable Long id, @RequestBody Topic topic,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        return topicService.updateTopic(id, topic, user.getUser().getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','MAJOR_ADMIN','COLLEGE_ADMIN')")
    @Operation(summary = "删除课题（仅待审批/已驳回且无确认选题可删）")
    public Result<?> delete(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        return topicService.deleteTopic(id, user.getUser().getId());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "审批课题（院系管理员，仅待审批可操作）")
    public Result<?> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String reason = (String) body.get("rejectReason");
        return topicService.approveTopic(id, approve, reason);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "我的课题（教师）")
    public Result<?> myTopics(@AuthenticationPrincipal UserDetailsImpl user) {
        return topicService.listMyTopics(user.getUser().getId());
    }

    /** 学生选题 */
    @PostMapping("/{id}/select")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "学生选题")
    public Result<?> selectTopic(@PathVariable Long id,
                                 @RequestParam Integer year,
                                 @AuthenticationPrincipal UserDetailsImpl user) {
        return topicService.selectTopic(user.getUser().getId(), id, year);
    }

    /** 教师确认/拒绝选题 */
    @PostMapping("/selections/{selectionId}/confirm")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "确认/拒绝选题（教师，仅待确认可操作）")
    public Result<?> confirm(@PathVariable Long selectionId,
                             @RequestBody Map<String, Object> body,
                             @AuthenticationPrincipal UserDetailsImpl user) {
        boolean confirm = Boolean.TRUE.equals(body.get("confirm"));
        return topicService.confirmSelection(selectionId, user.getUser().getId(), confirm);
    }

    /** 学生撤回选题（仅待确认状态可撤回） */
    @DeleteMapping("/selections/{selectionId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "撤回选题申请（仅待确认状态）")
    public Result<?> withdraw(@PathVariable Long selectionId,
                              @AuthenticationPrincipal UserDetailsImpl user) {
        return topicService.withdrawSelection(selectionId, user.getUser().getId());
    }

    /** 学生获取自己本届的选题信息 */
    @GetMapping("/selections/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取我的选题")
    public Result<?> mySelection(@RequestParam Integer year,
                                 @AuthenticationPrincipal UserDetailsImpl user) {
        return topicService.getMySelection(user.getUser().getId(), year);
    }

    /** 教师获取自己指导的所有学生选题 */
    @GetMapping("/selections/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "获取我指导的学生选题列表")
    public Result<?> teacherSelections(@AuthenticationPrincipal UserDetailsImpl user) {
        return topicService.getSelectionsByTeacher(user.getUser().getId());
    }
}
