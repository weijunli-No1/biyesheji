package com.biyesheji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.common.PageResult;
import com.biyesheji.common.Result;
import com.biyesheji.entity.User;
import com.biyesheji.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.biyesheji.security.UserDetailsImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** 分页查询用户列表 */
    @GetMapping
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "用户列表")
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        int myRole = userDetails.getUser().getRole();
        var wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(keyword), User::getRealName, keyword)
                .or(StringUtils.hasText(keyword), w -> w.like(User::getUsername, keyword))
                .eq(role != null, User::getRole, role)
                // 学院管理员只看本院用户，专业管理员只看本专业用户
                .eq(myRole == 6, User::getCollegeId, userDetails.getUser().getCollegeId())
                .eq(myRole == 5, User::getMajorId,   userDetails.getUser().getMajorId())
                .orderByAsc(User::getRole)
                .orderByAsc(User::getId);

        Page<User> p = userMapper.selectPage(new Page<>(page, size), wrapper);
        p.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(PageResult.of(p));
    }

    /** 修改用户状态（启用/禁用） */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "修改用户状态")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.fail("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.ok();
    }

    /** 重置密码为默认密码 password123 */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "重置密码")
    public Result<?> resetPassword(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(passwordEncoder.encode("password123"));
        userMapper.updateById(user);
        return Result.ok();
    }
}
