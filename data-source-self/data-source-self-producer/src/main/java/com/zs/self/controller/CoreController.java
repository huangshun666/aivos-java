package com.zs.self.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.market.api.web.ResultBody;
import com.zs.self.api.dto.ModelDTO;
import com.zs.self.api.dto.ModelTaskDTO;
import com.zs.self.api.service.RemoteSelfService;
import com.zs.self.pojo.Model;
import com.zs.self.pojo.ModelTask;
import com.zs.self.service.CoreService;
import com.zs.self.service.ModelService;
import com.zs.self.service.ModelTaskService;
import com.zs.self.task.ModelTaskExec;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class CoreController implements RemoteSelfService {

    @Autowired
    private ModelService modelService;

    @Autowired
    private ModelTaskService modelTaskService;

    @Autowired
    private CoreService coreService;

    @Autowired
    private ModelTaskExec modelTaskExec;

    @Override
    @Transactional
    public ResultBody saveModel(List<ModelDTO> list) {

        List<Model> models = list.stream().map(item -> {
            Model model = new Model();
            BeanUtils.copyProperties(item, model);
            return model;
        }).collect(Collectors.toList());
        models.forEach(item -> {
            Model model = modelService.lambdaQuery().eq(Model::getTime, item.getTime())
                    .eq(Model::getRefId, item.getRefId()).one();
            if (model == null) {
                modelService.save(item);
            } else {
                modelService.lambdaUpdate().set(Model::getOpen, item.getOpen())
                        .set(Model::getClose, item.getClose())
                        .set(Model::getLow, item.getLow())
                        .set(Model::getHigh, item.getHigh())
                        .eq(Model::getTime, item.getTime())
                        .eq(Model::getRefId, item.getRefId());
            }
        });
        return ResultBody.success();
    }

    @Override
    @Transactional
    public ResultBody saveModelTask(ModelTaskDTO modelTaskDTO) {
        ModelTask modelTask = new ModelTask();
        BeanUtils.copyProperties(modelTaskDTO, modelTask);
        modelTask.setUuid(UUID.randomUUID().toString());
        if (modelTaskDTO.getId() == null) coreService.generateMarket(modelTask);

        return ResultBody.success(modelTaskService.saveOrUpdate(modelTask));
    }


    @Override
    public ResultBody overtime() {
        modelTaskExec.scanType3();
        return ResultBody.success();
    }

    @Override
    public ResultBody execModel() {
        modelTaskExec.scanType1();
        return ResultBody.success();

    }

    @Override
    public ResultBody modelTaskList(JSONObject jsonObject) {
        Integer type = jsonObject.getInteger("type");
        Integer pageIndex = jsonObject.getInteger("pageIndex");
        Integer pageSize = jsonObject.getInteger("pageSize");
        Integer systemId = jsonObject.getInteger("systemId");

        return ResultBody.success(modelTaskService.lambdaQuery()
                .eq(Objects.nonNull(type), ModelTask::getType, type)
                .eq(Objects.nonNull(systemId), ModelTask::getSystemId, systemId)
                .page(new Page<>(pageIndex, pageSize)));
    }

    @Override
    public ResultBody modelList(JSONObject jsonObject) {
        Integer pageIndex = jsonObject.getInteger("pageIndex");
        Integer pageSize = jsonObject.getInteger("pageSize");
        Long startTime = jsonObject.getLong("sTime");
        Long endTime = jsonObject.getLong("eTime");
        Integer refId = jsonObject.getInteger("refId");
        return ResultBody.success(modelService.lambdaQuery().eq(Model::getRefId, refId)

                .between(Objects.nonNull(startTime) && Objects.nonNull(endTime)
                        , Model::getTime, startTime, endTime)
                .page(new Page<>(pageIndex, pageSize)));
    }
}
