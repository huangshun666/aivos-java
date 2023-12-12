package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.BankCardMapper;
import com.zs.forex.common.pojo.BankCard;
import com.zs.forex.service.BankCardService;
import org.springframework.stereotype.Service;

@Service
public class BankCardServiceImpl extends ServiceImpl<BankCardMapper, BankCard> implements BankCardService {
}
