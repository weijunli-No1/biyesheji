package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.dto.ThesisVO;
import com.biyesheji.entity.ThesisVersion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ThesisVersionMapper extends BaseMapper<ThesisVersion> {

    @Select("SELECT tv.id, tv.selection_id, tv.student_id, " +
            "u.real_name as studentName, t.title as topicTitle, " +
            "tv.version, tv.version_type, tv.file_url, tv.file_name, " +
            "tv.comment, tv.status, tv.create_time " +
            "FROM thesis_version tv " +
            "JOIN user u ON tv.student_id = u.id " +
            "JOIN topic_selection ts ON tv.selection_id = ts.id " +
            "JOIN topic t ON ts.topic_id = t.id " +
            "WHERE tv.teacher_id = #{teacherId} " +
            "ORDER BY tv.create_time DESC")
    List<ThesisVO> selectByTeacher(@Param("teacherId") Long teacherId);
}
