package com.zs.forex.common.dto;

import com.zs.forex.common.pojo.Symbol;
import lombok.Data;


@Data
public class SymbolDTO {

    private Symbol symbol;

    private QuoteDTO quoteDTO;

}
