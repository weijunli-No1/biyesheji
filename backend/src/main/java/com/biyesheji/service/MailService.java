package com.biyesheji.service;

import com.biyesheji.entity.User;
import com.biyesheji.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件通知服务
 * 通过 app.notify.email-enabled 开关控制是否真正发送邮件。
 * 未配置 SMTP 时设为 false，系统不报错只打日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final UserMapper userMapper;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromAddress;

    @Value("${app.name:毕业设计管理系统}")
    private String appName;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.notify.email-enabled:false}")
    private boolean emailEnabled;

    /**
     * 异步发送通知邮件
     * 根据 userId 查询邮箱，若用户无邮箱或邮件开关关闭则跳过。
     */
    @Async
    public void sendNotificationEmail(Long userId, String subject, String content) {
        if (!emailEnabled) {
            log.debug("[Mail] 邮件通知已关闭，跳过发送: subject={}", subject);
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.debug("[Mail] 用户 {} 无邮箱，跳过发送", userId);
            return;
        }
        sendHtml(user.getEmail(), subject, buildTemplate(user.getRealName(), subject, content));
    }

    /**
     * 直接向指定邮箱发送 HTML 邮件
     */
    @Async
    public void sendHtml(String to, String subject, String htmlContent) {
        if (!emailEnabled) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress, appName);
            helper.setTo(to);
            helper.setSubject(String.format("[%s] %s", appName, subject));
            helper.setText(htmlContent, true);
            mailSender.send(msg);
            log.info("[Mail] 发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("[Mail] 发送失败: to={}, error={}", to, e.getMessage());
        }
    }

    /** 构造通知邮件 HTML 模板 */
    private String buildTemplate(String realName, String title, String content) {
        return String.format("""
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:24px;background:#f9f9f9;">
              <div style="background:#1a6af0;color:#fff;padding:16px 24px;border-radius:8px 8px 0 0;">
                <h2 style="margin:0;font-size:18px;">%s</h2>
              </div>
              <div style="background:#fff;padding:24px;border-radius:0 0 8px 8px;border:1px solid #e0e0e0;">
                <p style="color:#333;font-size:14px;">亲爱的 <b>%s</b>，您好：</p>
                <h3 style="color:#1a6af0;font-size:16px;">%s</h3>
                <p style="color:#555;font-size:14px;line-height:1.6;">%s</p>
                <div style="margin-top:24px;">
                  <a href="%s" style="background:#1a6af0;color:#fff;padding:10px 24px;border-radius:4px;text-decoration:none;font-size:14px;">
                    前往系统查看
                  </a>
                </div>
                <hr style="margin-top:24px;border:none;border-top:1px solid #eee;">
                <p style="color:#999;font-size:12px;">此邮件由系统自动发送，请勿回复。<br>%s</p>
              </div>
            </div>
            """,
                appName, realName, title, content, frontendUrl, appName);
    }
}
