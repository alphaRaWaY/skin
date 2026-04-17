package org.skinAI.controller;

import org.skinAI.mapper.UserMapper;
import org.skinAI.pojo.User;
import org.skinAI.pojo.Result;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/hello")
public class HelloController {
    @Autowired
    UserMapper  userMapper;
    @GetMapping
    public Result<String> hello()
    {
        return Result.success("hello world");
    }
    @GetMapping("/user")
    public Result<User> getUser(){
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("userid");
        User user = userMapper.findById(userid);
        return Result.success(user);
    }
}
