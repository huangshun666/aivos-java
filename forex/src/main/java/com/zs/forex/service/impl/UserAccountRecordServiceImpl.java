package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.UserAccountRecordMapper;
import com.zs.forex.common.pojo.UserAccountRecord;
import com.zs.forex.service.UserAccountRecordService;
import org.springframework.stereotype.Service;

@Service
public class UserAccountRecordServiceImpl extends ServiceImpl<UserAccountRecordMapper, UserAccountRecord> implements UserAccountRecordService {
}
