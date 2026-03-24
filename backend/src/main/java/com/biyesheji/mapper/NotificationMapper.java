package com.biyesheji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.Notification;
import org.apache.ibatis.annotations.Update;

public interface NotificationMapper extends BaseMapper<Notification> {

    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(Long userId);
}
