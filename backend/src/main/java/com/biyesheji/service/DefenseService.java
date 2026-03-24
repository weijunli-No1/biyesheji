package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.dto.DefenseStudentVO;
import com.biyesheji.entity.DefenseGroup;
import com.biyesheji.entity.DefenseRecord;
import com.biyesheji.entity.TopicSelection;
import com.biyesheji.mapper.DefenseGroupMapper;
import com.biyesheji.mapper.DefenseRecordMapper;
import com.biyesheji.mapper.TopicSelectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DefenseService {

    private final DefenseGroupMapper groupMapper;
    private final DefenseRecordMapper recordMapper;
    private final TopicSelectionMapper selectionMapper;
    private final ScoreService scoreService;

    /** 查询答辩小组列表 */
    public Result<List<DefenseGroup>> listGroups(Integer year) {
        var list = groupMapper.selectList(new LambdaQueryWrapper<DefenseGroup>()
                .eq(DefenseGroup::getYear, year)
                .orderByAsc(DefenseGroup::getId));
        return Result.ok(list);
    }

    /** 创建答辩小组 */
    public Result<DefenseGroup> createGroup(DefenseGroup group) {
        if (group.getYear() == null) group.setYear(java.time.LocalDate.now().getYear());
        groupMapper.insert(group);
        return Result.ok(group);
    }

    /** 获取小组内的答辩学生列表 */
    public Result<List<DefenseStudentVO>> listGroupStudents(Long groupId) {
        return Result.ok(recordMapper.selectByGroupId(groupId));
    }

    /** 将学生（选题）分配到答辩组 */
    public Result<?> assignStudent(Long groupId, Long selectionId) {
        DefenseGroup group = groupMapper.selectById(groupId);
        if (group == null) return Result.fail("答辩小组不存在");

        TopicSelection selection = selectionMapper.selectById(selectionId);
        if (selection == null || selection.getStatus() != 1)
            return Result.fail("选题不存在或未确认");

        // 若已有记录则更新 groupId，否则新建
        DefenseRecord existing = recordMapper.selectOne(new LambdaQueryWrapper<DefenseRecord>()
                .eq(DefenseRecord::getSelectionId, selectionId)
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setGroupId(groupId);
            existing.setDefenseTime(group.getDefenseTime());
            recordMapper.updateById(existing);
        } else {
            DefenseRecord record = new DefenseRecord();
            record.setSelectionId(selectionId);
            record.setStudentId(selection.getStudentId());
            record.setGroupId(groupId);
            record.setDefenseTime(group.getDefenseTime());
            record.setDefenseRound(1);
            recordMapper.insert(record);
        }
        return Result.ok();
    }

    /** 录入/更新答辩成绩 */
    public Result<?> saveRecord(Map<String, Object> body) {
        Long selectionId = Long.parseLong(body.get("selectionId").toString());
        Integer presentScore = body.get("presentScore") != null ? (Integer) body.get("presentScore") : null;
        Integer qaScore = body.get("qaScore") != null ? (Integer) body.get("qaScore") : null;
        Integer result = body.get("result") != null ? (Integer) body.get("result") : null;
        String questions = (String) body.get("questions");
        String comment = (String) body.get("comment");
        String secretaryNote = (String) body.get("secretaryNote");

        DefenseRecord record = recordMapper.selectOne(new LambdaQueryWrapper<DefenseRecord>()
                .eq(DefenseRecord::getSelectionId, selectionId)
                .last("LIMIT 1"));
        if (record == null) return Result.fail("请先将该学生分配到答辩组");

        if (presentScore != null) record.setPresentScore(presentScore);
        if (qaScore != null) record.setQaScore(qaScore);
        if (result != null) record.setResult(result);
        if (questions != null) record.setQuestions(questions);
        if (comment != null) record.setComment(comment);
        if (secretaryNote != null) record.setSecretaryNote(secretaryNote);

        // 计算答辩总分
        if (record.getPresentScore() != null && record.getQaScore() != null) {
            record.setTotalScore((record.getPresentScore() + record.getQaScore()) / 2);
        }

        recordMapper.updateById(record);

        // 仅「通过」或「修改后通过」才将答辩分同步到综合成绩表
        // 「不通过」不同步：学生流程未完成，分数无意义
        if (record.getTotalScore() != null && record.getResult() != null && record.getResult() != 3) {
            scoreService.saveDefenseScore(selectionId, record.getTotalScore());
        }

        return Result.ok(record);
    }

    /**
     * 管理员确认「修改后通过」学生的修改已完成，解锁成绩锁定资格
     */
    public Result<?> confirmRevision(Long recordId) {
        DefenseRecord record = recordMapper.selectById(recordId);
        if (record == null) return Result.fail("答辩记录不存在");
        if (record.getResult() == null || record.getResult() != 2)
            return Result.fail(400, "只有「修改后通过」状态的记录才需要确认修改");
        if (Integer.valueOf(1).equals(record.getRevisionConfirmed()))
            return Result.fail(400, "该记录已确认修改完成");
        record.setRevisionConfirmed(1);
        recordMapper.updateById(record);
        return Result.ok();
    }

    /** 学生查看自己的答辩记录 */
    public Result<DefenseStudentVO> myRecord(Long studentId) {
        return Result.ok(recordMapper.selectByStudentId(studentId));
    }

    /** 查询未分配到任何答辩组的已确认选题学生 */
    public Result<List<DefenseStudentVO>> unassignedStudents(Integer year) {
        return Result.ok(recordMapper.selectUnassigned(year));
    }
}
