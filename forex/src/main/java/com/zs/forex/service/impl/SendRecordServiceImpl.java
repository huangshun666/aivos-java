package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.SendRecordMapper;
import com.zs.forex.common.pojo.SendRecord;
import com.zs.forex.service.SendRecordService;
import org.springframework.stereotype.Service;

@Service
public class SendRecordServiceImpl extends ServiceImpl<SendRecordMapper, SendRecord> implements SendRecordService {
}
