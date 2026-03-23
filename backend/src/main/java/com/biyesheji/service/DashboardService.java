package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.entity.*;
import com.biyesheji.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TopicSelectionMapper selectionMapper;
    private final ProposalMapper proposalMapper;
    private final MidCheckMapper midCheckMapper;
    private final ThesisVersionMapper thesisVersionMapper;
    private final ScoreMapper scoreMapper;
    private final TopicMapper topicMapper;
    private final DefenseRecordMapper defenseRecordMapper;
    private final CheckRecordMapper checkRecordMapper;
    private final ReviewMapper reviewMapper;

    /** 学生工作台统计 */
    public Result<Map<String, Object>> studentStats(Long studentId) {
        Map<String, Object> result = new HashMap<>();

        // 当前选题（已确认）
        TopicSelection selection = selectionMapper.selectOne(new LambdaQueryWrapper<TopicSelection>()
                .eq(TopicSelection::getStudentId, studentId)
                .eq(TopicSelection::getStatus, 1)
                .last("LIMIT 1"));

        if (selection == null) {
            result.put("selectionStatus", null);
            result.put("proposalStatus", null);
            result.put("midCheckStatus", null);
            result.put("thesisVersion", 0);
            result.put("processStep", 0);
            return Result.ok(result);
        }

        result.put("selectionStatus", selection.getStatus());

        // 开题报告
        Proposal proposal = proposalMapper.selectOne(new LambdaQueryWrapper<Proposal>()
                .eq(Proposal::getSelectionId, selection.getId()));
        result.put("proposalStatus", proposal != null ? proposal.getStatus() : null);

        // 中期检查
        MidCheck midCheck = midCheckMapper.selectOne(new LambdaQueryWrapper<MidCheck>()
                .eq(MidCheck::getSelectionId, selection.getId()));
        result.put("midCheckStatus", midCheck != null ? midCheck.getStatus() : null);

        // 最新论文版本
        long thesisCount = thesisVersionMapper.selectCount(new LambdaQueryWrapper<ThesisVersion>()
                .eq(ThesisVersion::getSelectionId, selection.getId()));
        result.put("thesisVersion", thesisCount);

        // 查重
        CheckRecord checkRecord = checkRecordMapper.selectOne(new LambdaQueryWrapper<CheckRecord>()
                .eq(CheckRecord::getSelectionId, selection.getId())
                .eq(CheckRecord::getStatus, 1)
                .last("LIMIT 1"));
        result.put("checkPassed", checkRecord != null);

        // 评阅
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getSelectionId, selection.getId())
                .eq(Review::getStatus, 1)
                .last("LIMIT 1"));
        result.put("reviewPassed", review != null);

        // 答辩
        DefenseRecord defenseRecord = defenseRecordMapper.selectOne(new LambdaQueryWrapper<DefenseRecord>()
                .eq(DefenseRecord::getSelectionId, selection.getId())
                .isNotNull(DefenseRecord::getResult)
                .last("LIMIT 1"));
        result.put("defenseResult", defenseRecord != null ? defenseRecord.getResult() : null);

        // 计算进度步骤
        int step = 1; // 已选题
        if (proposal != null && proposal.getStatus() >= 1) step = 2; // 开题已提交
        if (proposal != null && proposal.getStatus() == 3) step = 3; // 开题已通过
        if (midCheck != null && midCheck.getStatus() == 2) step = 4; // 中期已通过
        if (thesisCount > 0) step = 5; // 已提交论文
        if (checkRecord != null) step = 6; // 查重通过
        if (review != null) step = 7; // 评阅通过（含答辩资格）
        if (defenseRecord != null) step = 8; // 答辩完成
        result.put("processStep", step);

        return Result.ok(result);
    }

    /** 教师工作台统计 */
    public Result<Map<String, Object>> teacherStats(Long teacherId) {
        Map<String, Object> result = new HashMap<>();

        long studentCount = selectionMapper.selectCount(new LambdaQueryWrapper<TopicSelection>()
                .eq(TopicSelection::getTeacherId, teacherId)
                .eq(TopicSelection::getStatus, 1));
        result.put("studentCount", studentCount);

        long pendingProposal = proposalMapper.selectCount(new LambdaQueryWrapper<Proposal>()
                .eq(Proposal::getTeacherId, teacherId)
                .eq(Proposal::getStatus, 1));
        result.put("pendingProposal", pendingProposal);

        long pendingThesis = thesisVersionMapper.selectCount(new LambdaQueryWrapper<ThesisVersion>()
                .eq(ThesisVersion::getTeacherId, teacherId)
                .eq(ThesisVersion::getStatus, 0));
        result.put("pendingThesis", pendingThesis);

        long scoredCount = scoreMapper.selectCount(new LambdaQueryWrapper<Score>()
                .eq(Score::getTeacherId, teacherId)
                .isNotNull(Score::getTeacherScore));
        result.put("scoredCount", scoredCount);

        return Result.ok(result);
    }

    /** 管理员工作台统计 + 图表数据 */
    public Result<Map<String, Object>> adminStats() {
        Map<String, Object> result = new HashMap<>();

        // 四项基础统计
        long topicCount = topicMapper.selectCount(new LambdaQueryWrapper<Topic>()
                .eq(Topic::getStatus, 1));
        long selectedCount = selectionMapper.selectCount(new LambdaQueryWrapper<TopicSelection>()
                .eq(TopicSelection::getStatus, 1));
        long proposalPassCount = proposalMapper.selectCount(new LambdaQueryWrapper<Proposal>()
                .eq(Proposal::getStatus, 3));
        long scoredCount = scoreMapper.selectCount(new LambdaQueryWrapper<Score>()
                .eq(Score::getIsLocked, 1));

        result.put("topicCount", topicCount);
        result.put("selectedCount", selectedCount);
        result.put("proposalPassCount", proposalPassCount);
        result.put("scoredCount", scoredCount);

        // 各阶段完成率（相对于已确认选题数）
        long base = selectedCount == 0 ? 1 : selectedCount;

        long midCheckCount = midCheckMapper.selectCount(new LambdaQueryWrapper<MidCheck>()
                .eq(MidCheck::getStatus, 2));
        long thesisCount = thesisVersionMapper.selectCount(null); // 任意版本已提交
        long checkCount = checkRecordMapper.selectCount(new LambdaQueryWrapper<CheckRecord>()
                .eq(CheckRecord::getStatus, 1));
        long reviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getStatus, 1));
        long defenseCount = defenseRecordMapper.selectCount(new LambdaQueryWrapper<DefenseRecord>()
                .isNotNull(DefenseRecord::getResult));

        List<Map<String, Object>> stageCompletion = new ArrayList<>();
        String[] labels = {"选题", "开题", "中期", "论文", "查重", "评阅", "答辩"};
        long[] counts = {selectedCount, proposalPassCount, midCheckCount, thesisCount, checkCount, reviewCount, defenseCount};
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("stage", labels[i]);
            item.put("percent", Math.min(100, (int) (counts[i] * 100 / base)));
            stageCompletion.add(item);
        }
        result.put("stageCompletion", stageCompletion);

        // 成绩分布
        List<Score> scores = scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                .isNotNull(Score::getGrade));
        Map<String, Long> gradeMap = new LinkedHashMap<>();
        gradeMap.put("优秀", 0L); gradeMap.put("良好", 0L);
        gradeMap.put("中等", 0L); gradeMap.put("及格", 0L); gradeMap.put("不及格", 0L);
        for (Score s : scores) {
            gradeMap.merge(s.getGrade(), 1L, (a, b) -> a + b);
        }
        List<Map<String, Object>> gradeDistribution = new ArrayList<>();
        gradeMap.forEach((name, value) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", value);
            gradeDistribution.add(item);
        });
        result.put("gradeDistribution", gradeDistribution);

        return Result.ok(result);
    }
}
