package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.OrderRecordMapper;
import com.zs.forex.common.pojo.OrderRecord;
import com.zs.forex.service.OrderRecordService;
import org.springframework.stereotype.Service;

@Service
public class OrderRecordServiceImpl extends ServiceImpl<OrderRecordMapper, OrderRecord> implements OrderRecordService {
}
