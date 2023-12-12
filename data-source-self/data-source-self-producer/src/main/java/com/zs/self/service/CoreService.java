package com.zs.self.service;

import com.zs.self.pojo.Model;
import com.zs.self.pojo.ModelTask;

import java.math.BigDecimal;

public interface CoreService {

    void generateData(BigDecimal price, int index, Model model, ModelTask modelTask);

    void generateMarket(ModelTask modelTask);
}
