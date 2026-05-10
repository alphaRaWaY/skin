package org.skinAI.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String openid;
    private String username;
    private String nickname;
    private String avatar;
    private String mobile;
    private String jobNumber;
    private String passwordHash;
    private LocalDateTime createTime;
}
