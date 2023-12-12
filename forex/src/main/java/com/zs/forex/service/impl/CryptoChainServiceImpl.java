package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.CryptoChainMapper;
import com.zs.forex.common.pojo.CryptoChain;
import com.zs.forex.service.CryptoChainService;
import org.springframework.stereotype.Service;

@Service
public class CryptoChainServiceImpl extends ServiceImpl<CryptoChainMapper, CryptoChain> implements CryptoChainService {
}
