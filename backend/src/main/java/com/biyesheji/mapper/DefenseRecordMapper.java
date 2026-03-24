package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.dto.DefenseStudentVO;
import com.biyesheji.entity.DefenseRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface DefenseRecordMapper extends BaseMapper<DefenseRecord> {

    @Select("SELECT dr.id as recordId, dr.selection_id, dr.student_id, " +
            "u.username as studentNo, u.real_name as studentName, t.title as topicTitle, " +
            "dr.present_score, dr.qa_score, dr.total_score, dr.result, dr.revision_confirmed, " +
            "dr.questions, dr.comment, dr.secretary_note, " +
            "COALESCE(dr.defense_time, dg.defense_time) as defense_time " +
            "FROM defense_record dr " +
            "JOIN user u ON dr.student_id = u.id " +
            "JOIN topic_selection ts ON dr.selection_id = ts.id " +
            "JOIN topic t ON ts.topic_id = t.id " +
            "LEFT JOIN defense_group dg ON dr.group_id = dg.id " +
            "WHERE dr.group_id = #{groupId} AND dr.deleted = 0")
    List<DefenseStudentVO> selectByGroupId(@Param("groupId") Long groupId);

    @Select("SELECT dr.id as recordId, dr.selection_id, dr.student_id, " +
            "u.username as studentNo, u.real_name as studentName, t.title as topicTitle, " +
            "dr.present_score, dr.qa_score, dr.total_score, dr.result, dr.revision_confirmed, " +
            "dr.questions, dr.comment, dr.secretary_note, " +
            "COALESCE(dr.defense_time, dg.defense_time) as defense_time " +
            "FROM defense_record dr " +
            "JOIN user u ON dr.student_id = u.id " +
            "JOIN topic_selection ts ON dr.selection_id = ts.id " +
            "JOIN topic t ON ts.topic_id = t.id " +
            "LEFT JOIN defense_group dg ON dr.group_id = dg.id " +
            "WHERE dr.student_id = #{studentId} AND dr.deleted = 0 " +
            "ORDER BY dr.defense_round DESC LIMIT 1")
    DefenseStudentVO selectByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT ts.id as selectionId, u.id as studentId, u.username as studentNo, " +
            "u.real_name as studentName, t.title as topicTitle " +
            "FROM topic_selection ts " +
            "JOIN user u ON ts.student_id = u.id " +
            "JOIN topic t ON ts.topic_id = t.id " +
            "WHERE ts.status = 1 AND ts.year = #{year} " +
            "AND ts.id NOT IN (" +
            "  SELECT DISTINCT selection_id FROM defense_record WHERE group_id IS NOT NULL AND deleted = 0" +
            ")")
    List<DefenseStudentVO> selectUnassigned(@Param("year") Integer year);
}
