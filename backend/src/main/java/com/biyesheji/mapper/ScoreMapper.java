package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.Score;
import com.biyesheji.dto.ScoreVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ScoreMapper extends BaseMapper<Score> {

    @Select("SELECT ts.id AS selection_id, ts.teacher_id, " +
            "u_s.real_name AS student_name, u_s.username AS student_no, " +
            "u_s.college_id, u_s.major_id, t.title AS topic_title, u_t.real_name AS teacher_name, " +
            "s.id, s.student_id, s.proposal_score, s.teacher_score, s.review_score, s.defense_score, " +
            "s.total_score, s.grade, s.is_locked, s.is_excellent, s.teacher_comment " +
            "FROM topic_selection ts " +
            "LEFT JOIN score s ON ts.id = s.selection_id " +
            "LEFT JOIN user u_s ON ts.student_id = u_s.id " +
            "LEFT JOIN user u_t ON ts.teacher_id = u_t.id " +
            "LEFT JOIN topic t ON ts.topic_id = t.id " +
            "WHERE ts.status = 1 " +
            "AND (#{collegeId} IS NULL OR u_s.college_id = #{collegeId}) " +
            "AND (#{majorId} IS NULL OR u_s.major_id = #{majorId}) " +
            "AND (#{teacherId} IS NULL OR ts.teacher_id = #{teacherId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' " +
            "  OR u_s.real_name LIKE CONCAT('%',#{keyword},'%') " +
            "  OR u_s.username LIKE CONCAT('%',#{keyword},'%')) " +
            "ORDER BY ts.id")
    Page<ScoreVO> selectScoreList(Page<ScoreVO> page,
                                  @Param("collegeId") Long collegeId,
                                  @Param("majorId") Long majorId,
                                  @Param("teacherId") Long teacherId,
                                  @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) AS total, " +
            "SUM(CASE WHEN grade='优秀' THEN 1 ELSE 0 END) AS excellent, " +
            "SUM(CASE WHEN grade='良好' THEN 1 ELSE 0 END) AS good, " +
            "SUM(CASE WHEN grade='中等' THEN 1 ELSE 0 END) AS medium, " +
            "SUM(CASE WHEN grade='及格' THEN 1 ELSE 0 END) AS pass, " +
            "SUM(CASE WHEN grade='不及格' THEN 1 ELSE 0 END) AS fail, " +
            "AVG(total_score) AS average " +
            "FROM score WHERE is_locked = 1")
    ScoreVO selectScoreStat();
}
