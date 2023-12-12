package com.zs.forex.common.dto;

import com.zs.forex.common.pojo.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PLDetailsDTO {

    private Object user;

    private BigDecimal balance;

    private BigDecimal sumPL;

    private BigDecimal sumInsertMoney;

    private BigDecimal sumOutMoney;


}
