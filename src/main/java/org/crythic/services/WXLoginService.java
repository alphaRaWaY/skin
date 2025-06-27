package org.crythic.services;

import org.crythic.pojo.login.LoginResponse;
import org.crythic.pojo.login.WxLoginRequest;

public interface WXLoginService {
    LoginResponse loginByWeixinMiniProgram(WxLoginRequest request);
}
