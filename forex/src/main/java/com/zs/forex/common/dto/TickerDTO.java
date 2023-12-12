package com.zs.forex.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TickerDTO {

    private String p;

    private long t;
}
