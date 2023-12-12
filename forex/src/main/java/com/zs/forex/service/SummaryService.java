package com.zs.forex.service;

import com.zs.forex.common.dto.SummaryDTO;

public interface SummaryService {

    /**
     * 后管面板交易总数据
     *
     * @param uid 用户id
     * @return 数据
     */
    SummaryDTO indexData(Integer uid);



}
