package com.biyesheji.security;

import com.biyesheji.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + RoleEnum.of(user.getRole()).name()));
    }

    @Override public String getPassword()  { return user.getPassword(); }
    @Override public String getUsername()  { return user.getUsername(); }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return user.getStatus() == 1; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()             { return user.getStatus() == 1; }

    /**
     * 角色枚举（与数据库 role 字段一一对应，顺序不能变）
     * 1=STUDENT 2=TEACHER 3=REVIEWER 4=DEFENSE_MEMBER
     * 5=MAJOR_ADMIN 6=COLLEGE_ADMIN 7=ADMIN
     */
    public enum RoleEnum {
        STUDENT,        // 1 学生
        TEACHER,        // 2 指导教师
        REVIEWER,       // 3 评阅教师
        DEFENSE_MEMBER, // 4 答辩委员
        MAJOR_ADMIN,    // 5 专业管理员
        COLLEGE_ADMIN,  // 6 学院管理员
        ADMIN;          // 7 教务管理员（系统级）

        public static RoleEnum of(int role) {
            return values()[role - 1];
        }

        /** 是否具备管理权限（5/6/7） */
        public boolean isManager() {
            return this.ordinal() >= MAJOR_ADMIN.ordinal();
        }
    }
}
