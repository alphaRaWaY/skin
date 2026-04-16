package org.skinAI.pojo.login;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserProfile profile;
    // 省略getter/setter和构造函数
}
