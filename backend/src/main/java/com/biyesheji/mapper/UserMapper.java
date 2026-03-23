package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT u.* FROM user u " +
            "JOIN topic_selection ts ON u.id = ts.student_id " +
            "WHERE ts.teacher_id = #{teacherId} AND ts.status = 1 AND u.deleted = 0")
    Page<User> selectStudentsByTeacher(Page<User> page, @Param("teacherId") Long teacherId);
}
