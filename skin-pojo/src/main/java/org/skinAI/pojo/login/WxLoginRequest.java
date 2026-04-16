package org.skinAI.pojo.login;

import lombok.Data;

@Data
public class WxLoginRequest {
    private String code;
    private String encryptedData;
    private String iv;
}
