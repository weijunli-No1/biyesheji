package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/scores")
@RequiredArgsConstructor
@Tag(name = "成绩管理")
public class ScoreController {

    private final ScoreService scoreService;

    /**
     * 导师录入导师评分（只能录入自己指导学生的分，且只能录导师评分项）
     */
    @PostMapping("/{selectionId}/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "导师评分录入")
    public Result<?> saveTeacherScore(@PathVariable Long selectionId,
                                      @RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal UserDetailsImpl user) {
        Integer score = (Integer) body.get("teacherScore");
        String comment = (String) body.get("teacherComment");
        if (score == null || score < 0 || score > 100) return Result.fail(400, "评分须在0-100之间");
        return scoreService.saveTeacherScore(selectionId, user.getUser().getId(), score, comment);
    }

    /**
     * 开题评分录入（管理员录入，对应开题答辩评审结果）
     */
    @PostMapping("/{selectionId}/proposal")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "开题评分录入（管理员）")
    public Result<?> saveProposalScore(@PathVariable Long selectionId,
                                       @RequestBody Map<String, Object> body) {
        Integer score = (Integer) body.get("proposalScore");
        if (score == null || score < 0 || score > 100) return Result.fail(400, "评分须在0-100之间");
        return scoreService.saveProposalScore(selectionId, score);
    }

    /**
     * 评阅评分录入（管理员从 review 表同步，或评阅教师自行录入）
     */
    @PostMapping("/{selectionId}/review")
    @PreAuthorize("hasAnyRole('REVIEWER','MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "评阅评分录入")
    public Result<?> saveReviewScore(@PathVariable Long selectionId,
                                     @RequestBody Map<String, Object> body) {
        Integer score = (Integer) body.get("reviewScore");
        if (score == null || score < 0 || score > 100) return Result.fail(400, "评分须在0-100之间");
        return scoreService.saveReviewScore(selectionId, score);
    }

    /**
     * 答辩评分录入（答辩秘书/管理员录入）
     */
    @PostMapping("/{selectionId}/defense")
    @PreAuthorize("hasAnyRole('DEFENSE_MEMBER','MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "答辩评分录入")
    public Result<?> saveDefenseScore(@PathVariable Long selectionId,
                                      @RequestBody Map<String, Object> body) {
        Integer score = (Integer) body.get("defenseScore");
        if (score == null || score < 0 || score > 100) return Result.fail(400, "评分须在0-100之间");
        return scoreService.saveDefenseScore(selectionId, score);
    }

    /**
     * 锁定成绩（管理员，四项分数全部录入后才允许锁定）
     */
    @PostMapping("/{selectionId}/lock")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "锁定成绩（四项齐全后可锁定）")
    public Result<?> lock(@PathVariable Long selectionId) {
        return scoreService.lockScore(selectionId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "成绩列表")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        int role = user.getUser().getRole();
        // 教师只查自己指导的学生
        Long tid      = (role == 2) ? user.getUser().getId() : null;
        // 管理员按作用域过滤
        Long collegeId = (role == 6) ? user.getUser().getCollegeId() : null;
        Long majorId   = (role == 5) ? user.getUser().getMajorId()   : null;
        return scoreService.listScores(page, size, collegeId, majorId, tid);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "成绩统计")
    public Result<?> stats() {
        return scoreService.getStats();
    }

    @GetMapping("/selection/{selectionId}")
    @Operation(summary = "按选题获取成绩")
    public Result<?> getBySelection(@PathVariable Long selectionId) {
        return scoreService.getBySelectionId(selectionId);
    }
}
