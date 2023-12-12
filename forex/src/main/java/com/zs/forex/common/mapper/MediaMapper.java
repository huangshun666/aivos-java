package com.zs.forex.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zs.forex.common.pojo.CryptoChain;
import com.zs.forex.common.pojo.Media;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MediaMapper  extends BaseMapper<Media> {
}
