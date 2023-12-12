package com.zs.self.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.self.dao.ModelTaskDAO;
import com.zs.self.pojo.ModelTask;
import com.zs.self.service.ModelTaskService;
import org.springframework.stereotype.Service;

@Service
public class ModelTaskServiceImpl extends ServiceImpl<ModelTaskDAO, ModelTask> implements ModelTaskService {

}
