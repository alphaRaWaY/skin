package org.skinAI.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.skinAI.mapper.UserMapper;
import org.skinAI.pojo.Result;
import org.skinAI.pojo.User;
import org.skinAI.pojo.account.AccountProfileUpdateRequest;
import org.skinAI.pojo.account.ChangePasswordRequest;
import org.skinAI.utils.Md5Util;
import org.skinAI.utils.ThreadLocalUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public AccountController(UserMapper userMapper, StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/me")
    public Result<User> me() {
        User user = userMapper.findById(currentUserId().intValue());
        if (user == null) {
            return Result.error("user not found");
        }
        user.setPasswordHash(null);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result updateProfile(@RequestBody AccountProfileUpdateRequest req) {
        Long userId = currentUserId();
        User user = userMapper.findById(userId.intValue());
        if (user == null) {
            return Result.error("user not found");
        }
        if (req.getNickname() != null) {
            user.setNickname(trimToNull(req.getNickname()));
        }
        if (req.getMobile() != null) {
            user.setMobile(trimToNull(req.getMobile()));
        }
        if (req.getAvatar() != null) {
            user.setAvatar(trimToNull(req.getAvatar()));
        }
        if (req.getJobNumber() != null) {
            user.setJobNumber(trimToNull(req.getJobNumber()));
        }
        userMapper.updateProfileById(user);
        return Result.success();
    }

    @PostMapping("/password/verify-old")
    public Result verifyOldPassword(@RequestBody ChangePasswordRequest req) {
        String oldPassword = req.getOldPassword() == null ? "" : req.getOldPassword().trim();
        if (oldPassword.isEmpty()) {
            return Result.error("oldPassword is required");
        }
        Long userId = currentUserId();
        User user = userMapper.findById(userId.intValue());
        if (user == null) {
            return Result.error("user not found");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return Result.error("current account has no password configured");
        }
        if (!Md5Util.checkPassword(oldPassword, user.getPasswordHash())) {
            return Result.error("old password incorrect");
        }
        return Result.success();
    }

    @PutMapping("/password")
    public Result changePassword(@RequestBody ChangePasswordRequest req) {
        String oldPassword = req.getOldPassword() == null ? "" : req.getOldPassword().trim();
        String newPassword = req.getNewPassword() == null ? "" : req.getNewPassword().trim();
        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            return Result.error("oldPassword and newPassword are required");
        }
        if (newPassword.length() < 6) {
            return Result.error("newPassword must be at least 6 chars");
        }

        Long userId = currentUserId();
        User user = userMapper.findById(userId.intValue());
        if (user == null) {
            return Result.error("user not found");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return Result.error("current account has no password configured");
        }
        if (!Md5Util.checkPassword(oldPassword, user.getPasswordHash())) {
            return Result.error("old password incorrect");
        }

        userMapper.updatePasswordById(userId, Md5Util.getMD5String(newPassword));
        return Result.success();
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            stringRedisTemplate.delete("TOKEN:" + token);
        }
        return Result.success();
    }

    private Long currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Object raw = claims.get("userid");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        throw new RuntimeException("invalid login state");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
