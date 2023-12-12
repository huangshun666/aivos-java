package com.zs.forex.common.dto;

import com.zs.forex.common.pojo.Account;
import com.zs.forex.common.pojo.User;
import lombok.Data;

@Data
public class AdminAccountDTO {

    private Account account;

    private User user;

}
