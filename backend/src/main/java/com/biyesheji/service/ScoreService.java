package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.common.PageResult;
import com.biyesheji.common.Result;
import com.biyesheji.dto.ScoreVO;
import com.biyesheji.entity.DefenseRecord;
import com.biyesheji.entity.Score;
import com.biyesheji.entity.TopicSelection;
import com.biyesheji.mapper.DefenseRecordMapper;
import com.biyesheji.mapper.ScoreMapper;
import com.biyesheji.mapper.TopicSelectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreMapper scoreMapper;
    private final TopicSelectionMapper selectionMapper;
    private final DefenseRecordMapper defenseRecordMapper;

    // 各项权重
    private static final BigDecimal WEIGHT_PROPOSAL = new BigDecimal("0.10");
    private static final BigDecimal WEIGHT_TEACHER  = new BigDecimal("0.30");
    private static final BigDecimal WEIGHT_REVIEW   = new BigDecimal("0.20");
    private static final BigDecimal WEIGHT_DEFENSE  = new BigDecimal("0.40");

    /**
     * 导师录入"导师评分"和"导师评语"（仅能修改自己指导的学生，且成绩未锁定）
     * 导师不能修改开题分、评阅分、答辩分
     */
    public Result<Score> saveTeacherScore(Long selectionId, Long teacherId,
                                          Integer teacherScore, String teacherComment) {
        TopicSelection selection = selectionMapper.selectById(selectionId);
        if (selection == null) return Result.fail(404, "选题记录不存在");
        // teacherId != null 时（导师操作）校验归属；null 时（管理员操作）跳过
        if (teacherId != null && !selection.getTeacherId().equals(teacherId)) {
            return Result.fail(403, "只能录入自己指导的学生成绩");
        }
        Score score = getOrCreate(selectionId, selection.getStudentId(), selection.getTeacherId());
        if (score.getIsLocked() == 1) return Result.fail(400, "成绩已锁定，不可修改");

        score.setTeacherScore(teacherScore);
        score.setTeacherComment(teacherComment);
        recalculate(score);
        upsert(score);
        return Result.ok(score);
    }

    /**
     * 管理员录入开题评分（仅管理员可操作）
     */
    public Result<Score> saveProposalScore(Long selectionId, Integer proposalScore) {
        Score score = getOrCreate(selectionId, null, null);
        if (score.getIsLocked() == 1) return Result.fail(400, "成绩已锁定，不可修改");
        score.setProposalScore(proposalScore);
        recalculate(score);
        upsert(score);
        return Result.ok(score);
    }

    /**
     * 录入评阅评分（由管理员从 Review 表同步，或评审教师录入）
     */
    public Result<Score> saveReviewScore(Long selectionId, Integer reviewScore) {
        Score score = getOrCreate(selectionId, null, null);
        if (score.getIsLocked() == 1) return Result.fail(400, "成绩已锁定，不可修改");
        score.setReviewScore(reviewScore);
        recalculate(score);
        upsert(score);
        return Result.ok(score);
    }

    /**
     * 录入答辩评分（由答辩秘书/管理员录入）
     */
    public Result<Score> saveDefenseScore(Long selectionId, Integer defenseScore) {
        Score score = getOrCreate(selectionId, null, null);
        if (score.getIsLocked() == 1) return Result.fail(400, "成绩已锁定，不可修改");
        score.setDefenseScore(defenseScore);
        recalculate(score);
        upsert(score);
        return Result.ok(score);
    }

    /**
     * 成绩锁定（仅管理员可操作）
     * 锁定前校验：
     * 1. 四项分数均已录入
     * 2. 答辩记录存在且结果不为「不通过(3)」
     * 3. 「修改后通过(2)」须管理员已确认修改完成
     */
    public Result<?> lockScore(Long selectionId) {
        Score score = scoreMapper.selectOne(new LambdaQueryWrapper<Score>()
                .eq(Score::getSelectionId, selectionId));
        if (score == null) return Result.fail("成绩记录不存在");
        if (score.getIsLocked() == 1) return Result.fail(400, "成绩已锁定");

        // 答辩状态前置校验
        DefenseRecord defenseRecord = defenseRecordMapper.selectOne(new LambdaQueryWrapper<DefenseRecord>()
                .eq(DefenseRecord::getSelectionId, selectionId)
                .last("LIMIT 1"));
        if (defenseRecord == null || defenseRecord.getResult() == null)
            return Result.fail(400, "学生尚未完成答辩，无法锁定成绩");
        if (defenseRecord.getResult() == 3)
            return Result.fail(400, "答辩结果为「不通过」，无法锁定成绩");
        if (defenseRecord.getResult() == 2 && !Integer.valueOf(1).equals(defenseRecord.getRevisionConfirmed()))
            return Result.fail(400, "答辩结果为「修改后通过」，请先确认学生修改完成后再锁定");

        if (score.getProposalScore() == null) return Result.fail(400, "开题评分未录入，无法锁定");
        if (score.getTeacherScore() == null)  return Result.fail(400, "导师评分未录入，无法锁定");
        if (score.getReviewScore() == null)   return Result.fail(400, "评阅评分未录入，无法锁定");
        if (score.getDefenseScore() == null)  return Result.fail(400, "答辩评分未录入，无法锁定");

        score.setIsLocked(1);
        scoreMapper.updateById(score);
        return Result.ok();
    }

    public Result<PageResult<ScoreVO>> listScores(int page, int size, Long collegeId, Long majorId, Long teacherId, String keyword) {
        Page<ScoreVO> p = scoreMapper.selectScoreList(new Page<>(page, size), collegeId, majorId, teacherId, keyword);
        return Result.ok(PageResult.of(p));
    }

    public Result<ScoreVO> getStats() {
        return Result.ok(scoreMapper.selectScoreStat());
    }

    public Result<Score> getBySelectionId(Long selectionId) {
        var score = scoreMapper.selectOne(new LambdaQueryWrapper<Score>()
                .eq(Score::getSelectionId, selectionId));
        return Result.ok(score);
    }

    // -------- 私有工具方法 --------

    private Score getOrCreate(Long selectionId, Long studentId, Long teacherId) {
        Score score = scoreMapper.selectOne(new LambdaQueryWrapper<Score>()
                .eq(Score::getSelectionId, selectionId));
        if (score == null) {
            score = new Score();
            score.setSelectionId(selectionId);
            score.setIsLocked(0);
            score.setIsExcellent(0);
            // 若未传入 studentId/teacherId，则从 selection 表读取
            if (studentId == null || teacherId == null) {
                TopicSelection sel = selectionMapper.selectById(selectionId);
                if (sel != null) {
                    score.setStudentId(sel.getStudentId());
                    score.setTeacherId(sel.getTeacherId());
                }
            } else {
                score.setStudentId(studentId);
                score.setTeacherId(teacherId);
            }
        }
        return score;
    }

    /** 只有四项均已录入时才计算总分和等级 */
    private void recalculate(Score score) {
        if (score.getProposalScore() == null || score.getTeacherScore() == null
                || score.getReviewScore() == null || score.getDefenseScore() == null) {
            score.setTotalScore(null);
            score.setGrade(null);
            return;
        }
        BigDecimal total = BigDecimal.valueOf(score.getProposalScore()).multiply(WEIGHT_PROPOSAL)
                .add(BigDecimal.valueOf(score.getTeacherScore()).multiply(WEIGHT_TEACHER))
                .add(BigDecimal.valueOf(score.getReviewScore()).multiply(WEIGHT_REVIEW))
                .add(BigDecimal.valueOf(score.getDefenseScore()).multiply(WEIGHT_DEFENSE))
                .setScale(2, RoundingMode.HALF_UP);
        score.setTotalScore(total);

        int t = total.intValue();
        if (t >= 90)      score.setGrade("优秀");
        else if (t >= 80) score.setGrade("良好");
        else if (t >= 70) score.setGrade("中等");
        else if (t >= 60) score.setGrade("及格");
        else               score.setGrade("不及格");
    }

    private void upsert(Score score) {
        if (score.getId() == null) {
            scoreMapper.insert(score);
        } else {
            scoreMapper.updateById(score);
        }
    }
}
