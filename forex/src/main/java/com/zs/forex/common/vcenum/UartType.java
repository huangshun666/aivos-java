package com.zs.forex.common.vcenum;

/**
 * 流水类型
 */
public enum UartType {

    Trade,          //交易

    Fee,            //手续费

    Withdraw,        //出金

    Deposit,         //入金

    Recharge,        //充值

    Cancel,        //撤单回退

    Gift,          // 赠送金

    Credit,         //信用金

    Settlement,     //平仓

    PositionChange,  //仓位变更

    LowerScore,  //下分

    DirectPromotionRewards,//直推奖励

    LevelAward, //等级奖励

    Commission_Settlement,  //佣金结算

}
