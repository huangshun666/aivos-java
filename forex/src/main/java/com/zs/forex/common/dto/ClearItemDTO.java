package com.zs.forex.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ClearItemDTO {

    private List<ClearDTO> item;

    private BigDecimal price;


}
