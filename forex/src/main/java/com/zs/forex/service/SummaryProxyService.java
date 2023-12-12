package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.pojo.SummaryProxy;

public interface SummaryProxyService extends IService<SummaryProxy> {

    void execSummary();



    void execSettlement();
}
