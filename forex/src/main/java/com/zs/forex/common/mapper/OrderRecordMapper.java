package com.zs.forex.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zs.forex.common.pojo.OrderRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderRecordMapper extends BaseMapper<OrderRecord> {
}
