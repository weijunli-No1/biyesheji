package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.entity.ThesisAnnotation;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.ThesisAnnotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/thesis/annotations")
@RequiredArgsConstructor
@Tag(name = "论文批注")
public class ThesisAnnotationController {

    private final ThesisAnnotationService annotationService;

    /**
     * 导师添加批注
     */
    @PostMapping
    @Operation(summary = "添加批注")
    public Result<?> add(@RequestBody ThesisAnnotation annotation,
                         @AuthenticationPrincipal UserDetailsImpl user) {
        return annotationService.addAnnotation(annotation, user.getUser().getId());
    }

    /**
     * 查询某论文版本的所有批注
     */
    @GetMapping("/{thesisId}")
    @Operation(summary = "查询批注列表")
    public Result<?> list(@PathVariable Long thesisId,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        return annotationService.listByThesis(thesisId, user.getUser().getId());
    }

    /**
     * 学生标记批注为已处理
     */
    @PostMapping("/{id}/resolve")
    @Operation(summary = "标记批注已处理")
    public Result<?> resolve(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetailsImpl user) {
        return annotationService.resolveAnnotation(id, user.getUser().getId());
    }

    /**
     * 删除批注（仅作者）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除批注")
    public Result<?> delete(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        return annotationService.deleteAnnotation(id, user.getUser().getId());
    }
}
