package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.pojo.Level;
import com.zs.forex.common.pojo.UserAccountTrade;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

public interface LevelService extends IService<Level> {

    /**
     * 获取等级奖励 是否加一用户
     *
     * @param uid 用户id
     * @param add 添加
     * @return
     */
    Level getLevel(Integer uid, boolean add);

    /**
     * 充值门槛奖励
     */
    void rechargeThresholdReward(UserAccountTrade userAccountTrade);

    /**
     * 直推详情
     */
    Map<Object, Object> directPushDetails(Integer uid);


}
