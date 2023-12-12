package com.zs.forex.common.dto;

import com.zs.forex.common.pojo.BankCard;
import com.zs.forex.common.pojo.User;
import com.zs.forex.common.pojo.UserAccountTrade;
import lombok.Data;

@Data
public class ChuJinDTO {

    private Object bankCard;

    private UserAccountTrade userAccountTrade;

    private User user;
}
