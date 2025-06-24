package org.mypetstore.mypetstore.controller;


import org.mypetstore.mypetstore.pojo.login.LoginResponse;
import org.mypetstore.mypetstore.pojo.Result;
import org.mypetstore.mypetstore.pojo.login.WxLoginRequest;
import org.mypetstore.mypetstore.services.WXLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private WXLoginService wxLoginService;

    @PostMapping("/wxMin")
    public Result<LoginResponse> login(@RequestBody WxLoginRequest req) {
        LoginResponse loginResponse = wxLoginService.loginByWeixinMiniProgram(req);
        return Result.success(loginResponse);
    }
}
