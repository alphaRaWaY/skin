package org.mypetstore.mypetstore.services;

import org.mypetstore.mypetstore.pojo.login.LoginResponse;
import org.mypetstore.mypetstore.pojo.login.WxLoginRequest;

public interface WXLoginService {
    LoginResponse loginByWeixinMiniProgram(WxLoginRequest request);
}
