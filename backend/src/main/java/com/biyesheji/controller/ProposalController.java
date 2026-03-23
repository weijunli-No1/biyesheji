package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.entity.Proposal;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/proposals")
@RequiredArgsConstructor
@Tag(name = "开题报告")
public class ProposalController {

    private final ProposalService proposalService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "保存开题报告（草稿）")
    public Result<?> save(@RequestBody Proposal proposal,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        return proposalService.saveProposal(proposal, user.getUser().getId(), false);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "提交开题报告")
    public Result<?> submit(@RequestBody Proposal proposal,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        return proposalService.saveProposal(proposal, user.getUser().getId(), true);
    }

    @GetMapping("/selection/{selectionId}")
    @Operation(summary = "按选题ID获取开题报告")
    public Result<?> getBySelection(@PathVariable Long selectionId) {
        return proposalService.getBySelectionId(selectionId);
    }

    @PostMapping("/{id}/teacher-review")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "导师审核开题报告")
    public Result<?> teacherReview(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body,
                                   @AuthenticationPrincipal UserDetailsImpl user) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        String comment = (String) body.get("comment");
        String reason = (String) body.get("rejectReason");
        return proposalService.teacherReview(id, user.getUser().getId(), pass, comment, reason);
    }

    @PostMapping("/{id}/dept-review")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "评审组审核开题报告")
    public Result<?> deptReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        String comment = (String) body.get("reviewComment");
        Integer score = (Integer) body.get("reviewScore");
        String reason = (String) body.get("rejectReason");
        return proposalService.deptReview(id, pass, comment, score, reason);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "开题报告列表")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Long teacherId,
                          @RequestParam(required = false) Integer status,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        Long tid = user.getUser().getRole() == 2 ? user.getUser().getId() : teacherId;
        return proposalService.listProposals(page, size, tid, status);
    }
}
