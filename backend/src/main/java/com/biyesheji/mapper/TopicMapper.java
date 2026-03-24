package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.Topic;
import com.biyesheji.dto.TopicVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface TopicMapper extends BaseMapper<Topic> {

    @Select("SELECT t.*, u.real_name AS teacher_name, c.name AS college_name, " +
            "(SELECT COUNT(*) FROM topic_selection ts WHERE ts.topic_id = t.id AND ts.status IN (0,1)) AS selected_count " +
            "FROM topic t LEFT JOIN user u ON t.teacher_id = u.id " +
            "LEFT JOIN college c ON COALESCE(t.college_id, u.college_id) = c.id " +
            "WHERE t.deleted = 0 AND t.teacher_id = #{teacherId} " +
            "ORDER BY t.create_time DESC")
    List<TopicVO> selectMyTopics(@Param("teacherId") Long teacherId);

    @Select("SELECT t.*, u.real_name AS teacher_name, c.name AS college_name, " +
            "(SELECT COUNT(*) FROM topic_selection ts WHERE ts.topic_id = t.id AND ts.status IN (0,1)) AS selected_count " +
            "FROM topic t LEFT JOIN user u ON t.teacher_id = u.id " +
            "LEFT JOIN college c ON COALESCE(t.college_id, u.college_id) = c.id " +
            "WHERE t.deleted = 0 " +
            "AND (#{year} IS NULL OR t.year = #{year}) " +
            "AND (#{keyword} IS NULL OR t.title LIKE CONCAT('%',#{keyword},'%')) " +
            "AND (#{type} IS NULL OR t.type = #{type}) " +
            "AND (#{status} IS NULL OR t.status = #{status}) " +
            "AND (#{teacherId} IS NULL OR t.teacher_id = #{teacherId}) " +
            "AND (#{collegeId} IS NULL OR COALESCE(t.college_id, u.college_id) = #{collegeId}) " +
            "AND (#{majorId} IS NULL OR COALESCE(t.major_id, u.major_id) = #{majorId}) " +
            "ORDER BY t.create_time DESC")
    Page<TopicVO> selectTopicList(Page<TopicVO> page,
                                  @Param("year") Integer year,
                                  @Param("keyword") String keyword,
                                  @Param("type") Integer type,
                                  @Param("status") Integer status,
                                  @Param("teacherId") Long teacherId,
                                  @Param("collegeId") Long collegeId,
                                  @Param("majorId") Long majorId);

    @Select("SELECT t.*, u.real_name AS teacher_name, c.name AS college_name, " +
            "(SELECT COUNT(*) FROM topic_selection ts WHERE ts.topic_id = t.id AND ts.status IN (0,1)) AS selected_count " +
            "FROM topic t LEFT JOIN user u ON t.teacher_id = u.id " +
            "LEFT JOIN college c ON COALESCE(t.college_id, u.college_id) = c.id " +
            "WHERE t.deleted = 0 AND t.id = #{id}")
    TopicVO selectTopicVOById(@Param("id") Long id);
}
