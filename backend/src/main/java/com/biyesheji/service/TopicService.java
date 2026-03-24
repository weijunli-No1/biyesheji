package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.common.PageResult;
import com.biyesheji.common.Result;
import com.biyesheji.dto.SelectionVO;
import com.biyesheji.dto.TopicVO;
import com.biyesheji.entity.Topic;
import com.biyesheji.entity.TopicSelection;
import com.biyesheji.entity.User;
import com.biyesheji.mapper.TopicMapper;
import com.biyesheji.mapper.TopicSelectionMapper;
import com.biyesheji.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicMapper topicMapper;
    private final TopicSelectionMapper selectionMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /** 教师申报新课题 */
    public Result<Topic> applyTopic(Topic topic, Long teacherId) {
        topic.setTeacherId(teacherId);
        topic.setStatus(0); // 新申报，待审批
        topic.setId(null);  // 防止误更新已有记录
        topicMapper.insert(topic);
        return Result.ok(topic);
    }

    /**
     * 修改课题（仅允许修改自己的、且处于"待审批"或"已驳回"状态的课题）
     * 修改后重置为待审批，需重新审批
     */
    public Result<Topic> updateTopic(Long topicId, Topic topic, Long teacherId) {
        Topic existing = topicMapper.selectById(topicId);
        if (existing == null) return Result.fail("课题不存在");

        // 只能修改自己的课题
        if (!existing.getTeacherId().equals(teacherId)) {
            return Result.fail(403, "只能修改自己申报的课题");
        }
        // 只允许修改"待审批(0)"或"已驳回(3)"状态的课题
        if (existing.getStatus() != 0 && existing.getStatus() != 3) {
            return Result.fail(400, "只有待审批或已驳回的课题可以修改");
        }
        // 已有学生申请或确认选题时，禁止修改
        long activeCount = selectionMapper.selectCount(new LambdaQueryWrapper<TopicSelection>()
                .eq(TopicSelection::getTopicId, topicId)
                .in(TopicSelection::getStatus, List.of(0, 1))); // 待确认或已确认
        if (activeCount > 0) {
            return Result.fail(400, "已有学生申请或确认选题，无法修改课题");
        }

        topic.setId(topicId);
        topic.setTeacherId(teacherId);
        topic.setStatus(0); // 修改后重新进入待审批
        topic.setRejectReason(null);
        topicMapper.updateById(topic);
        return Result.ok(topic);
    }

    /**
     * 教师删除课题
     * 仅允许删除自己的、且处于"待审批(0)"或"已驳回(3)"且没有已确认选题的课题
     */
    public Result<?> deleteTopic(Long topicId, Long teacherId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) return Result.fail("课题不存在");
        if (!topic.getTeacherId().equals(teacherId)) return Result.fail(403, "只能删除自己申报的课题");
        if (topic.getStatus() != 0 && topic.getStatus() != 3) {
            return Result.fail(400, "只有待审批或已驳回的课题可以删除");
        }
        long activeSelections = selectionMapper.selectCount(new LambdaQueryWrapper<TopicSelection>()
                .eq(TopicSelection::getTopicId, topicId)
                .in(TopicSelection::getStatus, List.of(0, 1))); // 待确认或已确认
        if (activeSelections > 0) return Result.fail(400, "已有学生申请或确认选题，无法删除");
        topicMapper.deleteById(topicId);
        return Result.ok();
    }

    /** 院系审批课题（只有待审批状态可被审批，管理员只能审批本范围内的课题） */
    public Result<?> approveTopic(Long topicId, boolean approve, String rejectReason,
                                  int adminRole, Long adminCollegeId, Long adminMajorId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) return Result.fail("课题不存在");
        if (topic.getStatus() != 0) return Result.fail(400, "只有待审批状态的课题可以审批");
        if (!approve && (rejectReason == null || rejectReason.isBlank())) {
            return Result.fail(400, "驳回时必须填写原因");
        }
        // 范围校验：学院管理员只能审批本院课题，专业管理员只能审批本专业课题
        if (adminRole == 6 && adminCollegeId != null) {
            Long topicCollege = topic.getCollegeId();
            if (topicCollege == null || !topicCollege.equals(adminCollegeId)) {
                return Result.fail(403, "只能审批本学院的课题");
            }
        }
        if (adminRole == 5 && adminMajorId != null) {
            Long topicMajor = topic.getMajorId();
            if (topicMajor == null || !topicMajor.equals(adminMajorId)) {
                return Result.fail(403, "只能审批本专业的课题");
            }
        }
        topic.setStatus(approve ? 1 : 3);
        if (!approve) topic.setRejectReason(rejectReason);
        topicMapper.updateById(topic);

        if (approve) {
            notificationService.topicApproved(topic.getTeacherId(), topicId, topic.getTitle());
        } else {
            notificationService.topicRejected(topic.getTeacherId(), topicId, topic.getTitle(), rejectReason);
        }
        return Result.ok();
    }

    /**
     * 学生选题
     * P4 架构调整：每学生每届只保留一条选题记录，被拒/解除后重新选题复用旧记录而非新增，
     * 避免历史脏数据堆积并确保数据一致性。
     */
    @Transactional
    public Result<?> selectTopic(Long studentId, Long topicId, Integer year) {
        // 查询该学生本届任意状态的选题记录（统一用一条记录管理整个生命周期）
        TopicSelection anyRecord = selectionMapper.selectOne(new LambdaQueryWrapper<TopicSelection>()
                .eq(TopicSelection::getStudentId, studentId)
                .eq(TopicSelection::getYear, year)
                .last("LIMIT 1"));

        if (anyRecord != null && (anyRecord.getStatus() == 0 || anyRecord.getStatus() == 1)) {
            return Result.fail(400, "您本届已有选题申请，请等待导师确认");
        }

        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) return Result.fail(400, "课题不存在");
        if (topic.getStatus() != 1) return Result.fail(400, "该课题不在可选状态");

        // 名额检查：只计算"已确认(1)"的选题
        long confirmed = selectionMapper.selectCount(new LambdaQueryWrapper<TopicSelection>()
                .eq(TopicSelection::getTopicId, topicId)
                .eq(TopicSelection::getStatus, 1));
        if (confirmed >= topic.getMaxStudents()) return Result.fail(400, "该课题名额已满");

        Long selectionId;
        if (anyRecord != null) {
            // 已拒绝(2)或已解除(3) → 复用记录，重置为待确认
            anyRecord.setTopicId(topicId);
            anyRecord.setTeacherId(topic.getTeacherId());
            anyRecord.setCollegeId(topic.getCollegeId());
            anyRecord.setMajorId(topic.getMajorId());
            anyRecord.setStatus(0);
            anyRecord.setApplyTime(LocalDateTime.now());
            anyRecord.setConfirmTime(null);
            selectionMapper.updateById(anyRecord);
            selectionId = anyRecord.getId();
        } else {
            TopicSelection selection = new TopicSelection();
            selection.setStudentId(studentId);
            selection.setTopicId(topicId);
            selection.setTeacherId(topic.getTeacherId());
            selection.setCollegeId(topic.getCollegeId());
            selection.setMajorId(topic.getMajorId());
            selection.setYear(year);
            selection.setStatus(0);
            selection.setApplyTime(LocalDateTime.now());
            selectionMapper.insert(selection);
            selectionId = selection.getId();
        }

        User student = userMapper.selectById(studentId);
        String studentName = student != null ? student.getRealName() : "学生";
        notificationService.selectionApplied(topic.getTeacherId(), selectionId, studentName, topic.getTitle());
        return Result.ok();
    }

    /**
     * P3: 管理员关闭课题（已发布 → 已关闭）
     * 适用场景：名额已满手动关闭、或停办该课题。
     */
    public Result<?> closeTopic(Long topicId, int adminRole, Long adminCollegeId, Long adminMajorId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) return Result.fail("课题不存在");
        if (topic.getStatus() != 1) return Result.fail(400, "只有已发布的课题可以关闭");
        if (adminRole == 6 && adminCollegeId != null && !adminCollegeId.equals(topic.getCollegeId()))
            return Result.fail(403, "只能关闭本学院的课题");
        if (adminRole == 5 && adminMajorId != null && !adminMajorId.equals(topic.getMajorId()))
            return Result.fail(403, "只能关闭本专业的课题");
        topic.setStatus(2);
        topicMapper.updateById(topic);
        return Result.ok();
    }

    /**
     * 教师确认/拒绝选题
     * 前置条件：选题必须处于"待确认(0)"状态
     */
    public Result<?> confirmSelection(Long selectionId, Long teacherId, boolean confirm) {
        TopicSelection selection = selectionMapper.selectById(selectionId);
        if (selection == null) return Result.fail("选题记录不存在");
        if (!selection.getTeacherId().equals(teacherId)) return Result.fail(403, "无权操作此选题");
        if (selection.getStatus() != 0) return Result.fail(400, "只有待确认状态的选题可以操作");

        // 确认时再次检查名额（防止并发场景下名额溢出）
        if (confirm) {
            long confirmed = selectionMapper.selectCount(new LambdaQueryWrapper<TopicSelection>()
                    .eq(TopicSelection::getTopicId, selection.getTopicId())
                    .eq(TopicSelection::getStatus, 1));
            Topic topic = topicMapper.selectById(selection.getTopicId());
            if (confirmed >= topic.getMaxStudents()) return Result.fail(400, "课题名额已满，无法确认");
        }

        selection.setStatus(confirm ? 1 : 2);
        selection.setConfirmTime(LocalDateTime.now());
        selectionMapper.updateById(selection);

        Topic topic = topicMapper.selectById(selection.getTopicId());
        String topicTitle = topic != null ? topic.getTitle() : "课题";
        if (confirm) {
            notificationService.selectionConfirmed(selection.getStudentId(), selectionId, topicTitle);
        } else {
            notificationService.selectionRejected(selection.getStudentId(), selectionId, topicTitle);
        }
        return Result.ok();
    }

    /** 学生撤回选题申请（仅限"待确认(0)"状态） */
    public Result<?> withdrawSelection(Long selectionId, Long studentId) {
        TopicSelection selection = selectionMapper.selectById(selectionId);
        if (selection == null) return Result.fail("选题记录不存在");
        if (!selection.getStudentId().equals(studentId)) return Result.fail(403, "无权操作此选题");
        if (selection.getStatus() != 0) return Result.fail(400, "只有待确认状态的申请可以撤回");
        selectionMapper.deleteById(selectionId);
        return Result.ok();
    }

    /** 分页查询课题列表 */
    public Result<PageResult<TopicVO>> listTopics(int page, int size, Integer year, String keyword,
                                                  Integer type, Integer status, Long teacherId,
                                                  Long collegeId, Long majorId) {
        Page<TopicVO> p = topicMapper.selectTopicList(new Page<>(page, size), year, keyword, type, status, teacherId, collegeId, majorId);
        return Result.ok(PageResult.of(p));
    }

    /** 学生获取自己本届的选题信息 */
    public Result<SelectionVO> getMySelection(Long studentId, Integer year) {
        SelectionVO vo = selectionMapper.selectByStudentAndYear(studentId, year);
        return Result.ok(vo);
    }

    /** 教师获取自己指导的学生选题列表 */
    public Result<List<SelectionVO>> getSelectionsByTeacher(Long teacherId) {
        List<SelectionVO> list = selectionMapper.selectByTeacher(teacherId);
        return Result.ok(list);
    }

    public Result<List<TopicVO>> listMyTopics(Long teacherId) {
        return Result.ok(topicMapper.selectMyTopics(teacherId));
    }

    public Result<TopicVO> getTopic(Long topicId) {
        TopicVO topic = topicMapper.selectTopicVOById(topicId);
        if (topic == null) return Result.fail("课题不存在");
        return Result.ok(topic);
    }
}
