package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.CommissionConfigMapper;
import com.zs.forex.common.pojo.CommissionConfig;
import com.zs.forex.service.CommissionConfigService;
import org.springframework.stereotype.Service;

@Service
public class CommissionConfigServiceImpl extends ServiceImpl<CommissionConfigMapper, CommissionConfig> implements CommissionConfigService {
}
