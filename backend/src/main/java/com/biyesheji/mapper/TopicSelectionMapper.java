package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.TopicSelection;
import com.biyesheji.dto.SelectionVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface TopicSelectionMapper extends BaseMapper<TopicSelection> {

    @Select("SELECT ts.*, t.title AS topic_title, t.type AS topic_type, " +
            "u_s.real_name AS student_name, u_s.username AS student_no, " +
            "u_t.real_name AS teacher_name " +
            "FROM topic_selection ts " +
            "LEFT JOIN topic t ON ts.topic_id = t.id " +
            "LEFT JOIN user u_s ON ts.student_id = u_s.id " +
            "LEFT JOIN user u_t ON ts.teacher_id = u_t.id " +
            "WHERE ts.student_id = #{studentId} AND ts.year = #{year} AND ts.deleted = 0")
    SelectionVO selectByStudentAndYear(@Param("studentId") Long studentId, @Param("year") Integer year);

    @Select("SELECT ts.*, t.title AS topic_title, t.type AS topic_type, " +
            "u_s.real_name AS student_name, u_s.username AS student_no, " +
            "u_s.email AS student_email, " +
            "u_t.real_name AS teacher_name " +
            "FROM topic_selection ts " +
            "LEFT JOIN topic t ON ts.topic_id = t.id " +
            "LEFT JOIN user u_s ON ts.student_id = u_s.id " +
            "LEFT JOIN user u_t ON ts.teacher_id = u_t.id " +
            "WHERE ts.teacher_id = #{teacherId} AND ts.status = 1 AND ts.deleted = 0")
    List<SelectionVO> selectByTeacher(@Param("teacherId") Long teacherId);
}
