package org.skinAI.services.Impl;

import com.alibaba.fastjson.JSONObject;
import org.skinAI.mapper.UserMapper;
import org.skinAI.pojo.User;
import org.skinAI.pojo.login.LoginResponse;
import org.skinAI.pojo.login.PwdLoginRequest;
import org.skinAI.pojo.login.UserProfile;
import org.skinAI.pojo.login.WxLoginRequest;
import org.skinAI.services.WXLoginService;
import org.skinAI.utils.JwtUtil;
import org.skinAI.utils.Md5Util;
import org.skinAI.utils.WxDataDecryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WXLoginServiceImpl implements WXLoginService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Override
    public LoginResponse loginByWeixinMiniProgram(WxLoginRequest request) {
        User user;
        if ("mock_code".equals(request.getCode())) {
            user = userMapper.findByUsername("alphaRaWaY");
            if (user == null) {
                user = new User();
                user.setUsername("alphaRaWaY");
                user.setNickname("测试用户");
                user.setAvatar("https://github.com/alphaRaWaY.png");
                user.setOpenid("mock_openid_alphaRaWaY");
                user.setMobile("19070266618");
                user.setCreateTime(LocalDateTime.now());
                userMapper.insertUser(user);
                user = userMapper.findByUsername("alphaRaWaY");
            }
        } else {
            JSONObject sessionInfo = getSessionInfoFromWx(request.getCode());
            String openid = sessionInfo.getString("openid");
            String sessionKey = sessionInfo.getString("session_key");
            if (openid == null || sessionKey == null) {
                throw new RuntimeException("获取微信 openid 或 session_key 失败：" + sessionInfo.toJSONString());
            }
            JSONObject userInfo = WxDataDecryptor.decryptUserInfo(request.getEncryptedData(), request.getIv(), sessionKey);
            if (userInfo == null) {
                throw new RuntimeException("解密微信用户信息失败");
            }
            String nickname = userInfo.getString("nickName");
            String avatar = userInfo.getString("avatarUrl");

            user = userMapper.findByOpenid(openid);
            if (user == null) {
                user = new User();
                user.setOpenid(openid);
                user.setUsername("wx_" + UUID.randomUUID());
                user.setNickname(nickname);
                user.setAvatar(avatar);
                user.setCreateTime(LocalDateTime.now());
                JSONObject phoneInfo = WxDataDecryptor.decryptPhoneNumber(
                        request.getEncryptedData(), request.getIv(), sessionKey
                );
                String phoneNumber = phoneInfo.getString("phoneNumber");
                user.setMobile(phoneNumber);
                userMapper.insertUser(user);
                user = userMapper.findByOpenid(openid);
            }
        }

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse loginByPassword(PwdLoginRequest request) {
        if (request == null || request.getAccount() == null || request.getPassword() == null) {
            throw new RuntimeException("账号或密码不能为空");
        }

        String account = request.getAccount().trim();
        String password = request.getPassword();
        if (account.isEmpty() || password.isEmpty()) {
            throw new RuntimeException("账号或密码不能为空");
        }

        User user = userMapper.findByMobileOrJobNumber(account);
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new RuntimeException("账号不存在或未设置密码");
        }

        if (!Md5Util.checkPassword(password, user.getPasswordHash())) {
            throw new RuntimeException("账号或密码错误");
        }

        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        String random = UUID.randomUUID().toString();
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put("random", random);
        jwtMap.put("userid", user.getId());
        String token = JwtUtil.genToken(jwtMap);
        operations.set("TOKEN:" + token, String.valueOf(user.getId()), Duration.ofDays(1));

        UserProfile profile = new UserProfile();
        profile.setUsername(user.getUsername());
        profile.setMobile(user.getMobile());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());

        LoginResponse response = new LoginResponse();
        response.setProfile(profile);
        response.setToken(token);
        return response;
    }

    private JSONObject getSessionInfoFromWx(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appid
                + "&secret=" + secret
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        try {
            String response = restTemplate.getForObject(url, String.class);
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            throw new RuntimeException("请求微信接口失败", e);
        }
    }
}
