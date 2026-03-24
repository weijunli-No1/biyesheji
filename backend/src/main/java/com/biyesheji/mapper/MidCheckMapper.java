package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.dto.MidCheckVO;
import com.biyesheji.entity.MidCheck;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface MidCheckMapper extends BaseMapper<MidCheck> {

    @Select("SELECT mc.*, u.real_name AS student_name, u.username AS student_no, " +
            "t.title AS topic_title " +
            "FROM mid_check mc " +
            "LEFT JOIN user u ON mc.student_id = u.id " +
            "LEFT JOIN topic_selection ts ON mc.selection_id = ts.id " +
            "LEFT JOIN topic t ON ts.topic_id = t.id " +
            "WHERE mc.deleted = 0 " +
            "AND (#{teacherId} IS NULL OR mc.teacher_id = #{teacherId}) " +
            "AND (#{status}    IS NULL OR mc.status     = #{status}) " +
            "ORDER BY mc.create_time DESC")
    List<MidCheckVO> selectMidCheckList(@Param("teacherId") Long teacherId,
                                        @Param("status")    Integer status);
}
