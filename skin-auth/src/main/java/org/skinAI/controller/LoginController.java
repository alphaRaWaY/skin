package org.skinAI.controller;

import org.skinAI.pojo.Result;
import org.skinAI.pojo.login.LoginResponse;
import org.skinAI.pojo.login.PwdLoginRequest;
import org.skinAI.pojo.login.WxLoginRequest;
import org.skinAI.services.WXLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private WXLoginService wxLoginService;

    @PostMapping("/wxMin")
    public Result<LoginResponse> login(@RequestBody WxLoginRequest req) {
        try {
            return Result.success(wxLoginService.loginByWeixinMiniProgram(req));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/phone")
    public Result<LoginResponse> loginByPhone(@RequestBody WxLoginRequest req) {
        return Result.error("限于平台能力暂不支持此功能");
    }

    @PostMapping("/pwd")
    public Result<LoginResponse> loginByPwd(@RequestBody PwdLoginRequest req) {
        try {
            return Result.success(wxLoginService.loginByPassword(req));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
