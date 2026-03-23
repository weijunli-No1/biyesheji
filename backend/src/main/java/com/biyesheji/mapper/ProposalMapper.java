package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.Proposal;
import com.biyesheji.dto.ProposalVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProposalMapper extends BaseMapper<Proposal> {

    @Select("SELECT p.*, u_s.real_name AS student_name, u_s.username AS student_no, " +
            "t.title AS topic_title, u_t.real_name AS teacher_name " +
            "FROM proposal p " +
            "LEFT JOIN user u_s ON p.student_id = u_s.id " +
            "LEFT JOIN user u_t ON p.teacher_id = u_t.id " +
            "LEFT JOIN topic_selection ts ON p.selection_id = ts.id " +
            "LEFT JOIN topic t ON ts.topic_id = t.id " +
            "WHERE p.deleted = 0 " +
            "AND (#{teacherId} IS NULL OR p.teacher_id = #{teacherId}) " +
            "AND (#{status} IS NULL OR p.status = #{status})")
    Page<ProposalVO> selectProposalList(Page<ProposalVO> page,
                                        @Param("teacherId") Long teacherId,
                                        @Param("status") Integer status);
}
