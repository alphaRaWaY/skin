package org.skinAI.controller;


import org.skinAI.pojo.login.LoginResponse;
import org.skinAI.pojo.Result;
import org.skinAI.pojo.login.WxLoginRequest;
import org.skinAI.services.WXLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private WXLoginService wxLoginService;

    /**
     * 通过为刷新
     * @param req
     * @return
     */
    @PostMapping("/wxMin")
    public Result<LoginResponse> login(@RequestBody WxLoginRequest req) {
        LoginResponse loginResponse = wxLoginService.loginByWeixinMiniProgram(req);
        return Result.success(loginResponse);
    }

    @PostMapping("/phone")
    public Result<LoginResponse> loginByPhone(@RequestBody WxLoginRequest req) {
        return Result.success(null);
    }

    @PostMapping("/pwd")
    public Result<LoginResponse> loginByPwd(@RequestBody WxLoginRequest req) {
        return Result.success(null);
    }
}
