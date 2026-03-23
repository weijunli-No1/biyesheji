package com.biyesheji.service;

import com.biyesheji.common.Result;
import com.biyesheji.dto.LoginRequest;
import com.biyesheji.entity.User;
import com.biyesheji.mapper.UserMapper;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public Result<?> login(LoginRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            var userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userDetails.getUser();
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("role", user.getRole());
            data.put("collegeId", user.getCollegeId());
            data.put("majorId",   user.getMajorId());
            data.put("classId",   user.getClassId());
            return Result.ok(data);
        } catch (AuthenticationException e) {
            return Result.fail(400, "用户名或密码错误");
        }
    }

    public Result<User> getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(null);
        return Result.ok(user);
    }
}
