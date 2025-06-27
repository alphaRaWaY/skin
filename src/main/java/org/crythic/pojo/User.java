package org.crythic.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;             // 主键
    private String openid;       // 微信 openid
    private String username;     // 用户名
    private String nickname;     // 昵称
    private String avatar;       // 头像 URL
    private String mobile;       // 手机号
    private LocalDateTime createTime; // 创建时间
}
