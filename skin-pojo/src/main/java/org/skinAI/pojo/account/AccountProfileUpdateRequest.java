package org.skinAI.pojo.account;

import lombok.Data;

@Data
public class AccountProfileUpdateRequest {
    private String nickname;
    private String mobile;
    private String avatar;
    private String jobNumber;
}
