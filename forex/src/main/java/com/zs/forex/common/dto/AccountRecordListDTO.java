package com.zs.forex.common.dto;

import com.zs.forex.common.pojo.User;
import com.zs.forex.common.pojo.UserAccountRecord;
import lombok.Data;

@Data
public class AccountRecordListDTO {

    private UserAccountRecord userAccountRecord;

    private User user;
}
