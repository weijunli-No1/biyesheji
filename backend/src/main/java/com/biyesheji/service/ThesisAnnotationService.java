package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.entity.ThesisAnnotation;
import com.biyesheji.entity.ThesisVersion;
import com.biyesheji.entity.User;
import com.biyesheji.mapper.ThesisAnnotationMapper;
import com.biyesheji.mapper.ThesisVersionMapper;
import com.biyesheji.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 论文批注服务
 * 支持导师在论文指定页面添加批注，学生可查看并标记为已处理。
 */
@Service
@RequiredArgsConstructor
public class ThesisAnnotationService {

    private final ThesisAnnotationMapper annotationMapper;
    private final ThesisVersionMapper thesisVersionMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /**
     * 导师添加批注
     */
    public Result<ThesisAnnotation> addAnnotation(ThesisAnnotation annotation, Long teacherId) {
        ThesisVersion thesis = thesisVersionMapper.selectById(annotation.getThesisId());
        if (thesis == null) return Result.fail("论文版本不存在");
        if (!thesis.getTeacherId().equals(teacherId)) return Result.fail(403, "无权批注此论文");

        User teacher = userMapper.selectById(teacherId);
        annotation.setAuthorId(teacherId);
        annotation.setAuthorName(teacher != null ? teacher.getRealName() : "导师");
        annotation.setResolved(false);
        annotationMapper.insert(annotation);

        // 通知学生有新批注
        notificationService.thesisAnnotated(thesis.getStudentId(), annotation.getThesisId(),
                annotation.getAuthorName(), annotation.getPageNo());
        return Result.ok(annotation);
    }

    /**
     * 查询某论文版本的所有批注
     */
    public Result<List<ThesisAnnotation>> listByThesis(Long thesisId, Long userId) {
        ThesisVersion thesis = thesisVersionMapper.selectById(thesisId);
        if (thesis == null) return Result.fail("论文版本不存在");
        // 只有论文的学生或导师可以查看
        if (!thesis.getStudentId().equals(userId) && !thesis.getTeacherId().equals(userId)) {
            return Result.fail(403, "无权查看此批注");
        }
        List<ThesisAnnotation> list = annotationMapper.selectList(
                new LambdaQueryWrapper<ThesisAnnotation>()
                        .eq(ThesisAnnotation::getThesisId, thesisId)
                        .orderByAsc(ThesisAnnotation::getPageNo));
        return Result.ok(list);
    }

    /**
     * 学生标记批注为已处理
     */
    public Result<?> resolveAnnotation(Long annotationId, Long studentId) {
        ThesisAnnotation annotation = annotationMapper.selectById(annotationId);
        if (annotation == null) return Result.fail("批注不存在");

        ThesisVersion thesis = thesisVersionMapper.selectById(annotation.getThesisId());
        if (thesis == null || !thesis.getStudentId().equals(studentId)) {
            return Result.fail(403, "无权操作此批注");
        }
        annotation.setResolved(true);
        annotationMapper.updateById(annotation);
        return Result.ok();
    }

    /**
     * 删除批注（仅批注作者可删除）
     */
    public Result<?> deleteAnnotation(Long annotationId, Long userId) {
        ThesisAnnotation annotation = annotationMapper.selectById(annotationId);
        if (annotation == null) return Result.fail("批注不存在");
        if (!annotation.getAuthorId().equals(userId)) return Result.fail(403, "无权删除此批注");
        annotationMapper.deleteById(annotationId);
        return Result.ok();
    }
}
