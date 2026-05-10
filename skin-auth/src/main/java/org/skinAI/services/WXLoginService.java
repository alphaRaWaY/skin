package org.skinAI.services;

import org.skinAI.pojo.login.LoginResponse;
import org.skinAI.pojo.login.PwdLoginRequest;
import org.skinAI.pojo.login.WxLoginRequest;

public interface WXLoginService {
    LoginResponse loginByWeixinMiniProgram(WxLoginRequest request);

    LoginResponse loginByPassword(PwdLoginRequest request);
}
