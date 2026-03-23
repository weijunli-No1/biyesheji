package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.entity.ClassGroup;
import com.biyesheji.entity.College;
import com.biyesheji.entity.Major;
import com.biyesheji.mapper.ClassGroupMapper;
import com.biyesheji.mapper.CollegeMapper;
import com.biyesheji.mapper.MajorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrgService {

    private final CollegeMapper collegeMapper;
    private final MajorMapper majorMapper;
    private final ClassGroupMapper classGroupMapper;

    // ---- 学院 ----

    public Result<List<College>> listColleges() {
        return Result.ok(collegeMapper.selectList(null));
    }

    public Result<College> createCollege(College college) {
        collegeMapper.insert(college);
        return Result.ok(college);
    }

    public Result<College> updateCollege(Long id, College college) {
        college.setId(id);
        collegeMapper.updateById(college);
        return Result.ok(college);
    }

    public Result<?> deleteCollege(Long id) {
        // 检查是否还有专业
        long count = majorMapper.selectCount(
                new LambdaQueryWrapper<Major>().eq(Major::getCollegeId, id));
        if (count > 0) return Result.fail("该学院下还有专业，无法删除");
        collegeMapper.deleteById(id);
        return Result.ok();
    }

    // ---- 专业 ----

    public Result<List<Major>> listMajors(Long collegeId) {
        var wrapper = new LambdaQueryWrapper<Major>();
        if (collegeId != null) wrapper.eq(Major::getCollegeId, collegeId);
        return Result.ok(majorMapper.selectList(wrapper));
    }

    public Result<Major> createMajor(Major major) {
        majorMapper.insert(major);
        return Result.ok(major);
    }

    public Result<Major> updateMajor(Long id, Major major) {
        major.setId(id);
        majorMapper.updateById(major);
        return Result.ok(major);
    }

    public Result<?> deleteMajor(Long id) {
        long count = classGroupMapper.selectCount(
                new LambdaQueryWrapper<ClassGroup>().eq(ClassGroup::getMajorId, id));
        if (count > 0) return Result.fail("该专业下还有班级，无法删除");
        majorMapper.deleteById(id);
        return Result.ok();
    }

    // ---- 班级 ----

    public Result<List<ClassGroup>> listClasses(Long majorId, Long collegeId) {
        var wrapper = new LambdaQueryWrapper<ClassGroup>();
        if (majorId != null)   wrapper.eq(ClassGroup::getMajorId, majorId);
        if (collegeId != null) wrapper.eq(ClassGroup::getCollegeId, collegeId);
        return Result.ok(classGroupMapper.selectList(wrapper));
    }

    public Result<ClassGroup> createClass(ClassGroup classGroup) {
        classGroupMapper.insert(classGroup);
        return Result.ok(classGroup);
    }

    public Result<ClassGroup> updateClass(Long id, ClassGroup classGroup) {
        classGroup.setId(id);
        classGroupMapper.updateById(classGroup);
        return Result.ok(classGroup);
    }

    public Result<?> deleteClass(Long id) {
        classGroupMapper.deleteById(id);
        return Result.ok();
    }
}
