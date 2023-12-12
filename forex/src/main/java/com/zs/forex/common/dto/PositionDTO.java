package com.zs.forex.common.dto;

import com.zs.forex.common.pojo.Order;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.pojo.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PositionDTO {

    private ClearDTO clearDTO;

    private Order order;

    private Symbol symbol;

    private User user;

}
