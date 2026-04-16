package org.skinAI.controller;

import org.skinAI.pojo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    @GetMapping
    public Result<String> hello()
    {
        return Result.success("public hello world");
    }
}
