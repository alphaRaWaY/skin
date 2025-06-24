package org.mypetstore.mypetstore.services.Impl;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import org.mypetstore.mypetstore.mapper.UserMapper;
import org.mypetstore.mypetstore.pojo.login.LoginResponse;
import org.mypetstore.mypetstore.pojo.User;
import org.mypetstore.mypetstore.pojo.login.UserProfile;
import org.mypetstore.mypetstore.pojo.login.WxLoginRequest;
import org.mypetstore.mypetstore.services.WXLoginService;
import org.mypetstore.mypetstore.utils.JwtUtil;
import org.mypetstore.mypetstore.utils.ThreadLocalUtil;
import org.mypetstore.mypetstore.utils.WxDataDecryptor;
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
    private StringRedisTemplate  redisTemplate;

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
            // 模拟登录逻辑
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
                // 重新查询保证有id
                user = userMapper.findByUsername("alphaRaWaY");
            }
        } else {
            // 真实登录逻辑
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
                //获取电话号码
                JSONObject phoneInfo = WxDataDecryptor.decryptPhoneNumber(
                        request.getEncryptedData(), request.getIv(), sessionKey
                );
                String phoneNumber = phoneInfo.getString("phoneNumber");
                user.setMobile(phoneNumber);
                userMapper.insertUser(user);
                //重新查询保证返回的用户携带id
                user = userMapper.findByOpenid(openid);
            }
        }
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        // 生成token
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
