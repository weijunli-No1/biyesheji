package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    /** 1学生 2指导教师 3评阅教师 4答辩委员 5专业管理员 6学院管理员 7教务管理员 */
    private Integer role;
    private Long collegeId;
    private Long majorId;
    private Long classId;
    private String email;
    private String phone;
    private String avatar;
    /** 职称（教师专用：讲师/副教授/教授等） */
    private String title;
    /** 院系名称快照（冗余，避免频繁 join） */
    private String department;
    private Integer status;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
