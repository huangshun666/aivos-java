package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.pojo.UserAccountTrade;
import com.zs.forex.common.web.WebException;

public interface UserAccountTradeService extends IService<UserAccountTrade> {

    void review(String id,Integer type) throws WebException;
}
