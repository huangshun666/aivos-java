package com.zs.self.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.self.dao.ModelDAO;
import com.zs.self.pojo.Model;
import com.zs.self.service.ModelService;
import org.springframework.stereotype.Service;

@Service
public class ModelServiceImpl extends ServiceImpl<ModelDAO, Model> implements ModelService {

}
