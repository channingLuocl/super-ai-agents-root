package com.example.superaiagents.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 邮件发送工具
 */
public class EmailSendTool {

    // 邮件服务器配置（可根据实际情况调整）
    private final String SMTP_HOST = "smtp.qq.com"; // 示例：QQ邮箱SMTP服务器
    private final String SMTP_PORT = "587";
    private final String SMTP_USER = "2301155695@qq.com"; // 发件人邮箱
    private final String SMTP_PASSWORD = "htnbjoruryjbecde"; // 邮箱授权码

    @Tool(description = "Send an email to specified recipient", returnDirect = true)
    public String sendEmail(
            @ToolParam(description = "Recipient email address") String toEmail,
            @ToolParam(description = "Email subject") String subject,
            @ToolParam(description = "Email content") String content) {
        try {
            // 配置邮件属性
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            // 创建会话
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
                }
            });

            // 创建邮件消息
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(content);

            // 发送邮件
            Transport.send(message);

            return "Email sent successfully to: " + toEmail;

        } catch (Exception e) {
            return "Error sending email: " + e.getMessage();
        }
    }

}