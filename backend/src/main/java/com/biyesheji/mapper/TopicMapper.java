package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.Topic;
import com.biyesheji.dto.TopicVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TopicMapper extends BaseMapper<Topic> {

    @Select("SELECT t.*, u.real_name AS teacher_name, " +
            "(SELECT COUNT(*) FROM topic_selection ts WHERE ts.topic_id = t.id AND ts.status = 1) AS selected_count " +
            "FROM topic t LEFT JOIN user u ON t.teacher_id = u.id " +
            "WHERE t.deleted = 0 " +
            "AND (#{year} IS NULL OR t.year = #{year}) " +
            "AND (#{keyword} IS NULL OR t.title LIKE CONCAT('%',#{keyword},'%')) " +
            "AND (#{status} IS NULL OR t.status = #{status}) " +
            "AND (#{teacherId} IS NULL OR t.teacher_id = #{teacherId}) " +
            "AND (#{collegeId} IS NULL OR COALESCE(t.college_id, u.college_id) = #{collegeId}) " +
            "AND (#{majorId} IS NULL OR COALESCE(t.major_id, u.major_id) = #{majorId})")
    Page<TopicVO> selectTopicList(Page<TopicVO> page,
                                  @Param("year") Integer year,
                                  @Param("keyword") String keyword,
                                  @Param("status") Integer status,
                                  @Param("teacherId") Long teacherId,
                                  @Param("collegeId") Long collegeId,
                                  @Param("majorId") Long majorId);
}
