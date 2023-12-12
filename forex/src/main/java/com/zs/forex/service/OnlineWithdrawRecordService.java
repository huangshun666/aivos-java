package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.pojo.OnlineWithdrawRecord;

import java.math.BigDecimal;

public interface OnlineWithdrawRecordService extends IService<OnlineWithdrawRecord> {

    void execWithdraw(String address, Integer uid, String chain, BigDecimal money, String orderId);

    void  execSelectInfo();
}
