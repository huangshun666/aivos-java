package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.RewardRecordMapper;
import com.zs.forex.common.pojo.RewardRecord;
import com.zs.forex.service.RewardRecordService;
import org.springframework.stereotype.Service;

@Service
public class RewardRecordServiceImpl extends ServiceImpl<RewardRecordMapper, RewardRecord> implements RewardRecordService {
}
