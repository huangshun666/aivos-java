package com.zs.self.task;

import com.alibaba.fastjson.JSONObject;
import com.zs.self.pojo.Model;
import com.zs.self.service.ModelService;
import com.zs.self.service.ModelTaskService;
import com.zs.self.service.TradeTimeService;
import com.zs.self.pojo.ModelTask;
import com.zs.self.service.CoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component

public class ModelTaskExec {

    @Autowired
    private TradeTimeService tradeTimeService;

    @Autowired
    private ModelTaskService modelTaskService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CoreService coreService;

    @Autowired
    private ModelService modelService;


    @PostConstruct
    public void init() {
        new Thread(() -> {
            try {
                CoreHandler.start();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void scanType1() {
        SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
        try {
            log.info("ModelTaskExec: 开始扫描存活任务-----");
            new ArrayList<>(Arrays.asList("US", "HKEX", "MAS")).forEach(item -> {

                String format = dft.format(new Date());
                List<ModelTask> list = modelTaskService.lambdaQuery()
                        .eq(ModelTask::getMarket, item)
                        .eq(ModelTask::getType, 1)
                        .le(ModelTask::getStartTime, format).list();

                list.forEach(cu -> {
                    long time = (new Date().getTime() + 100) / 1000;
                    Model model = modelService.lambdaQuery().eq(Model::getRefId, cu.getId())
                            .eq(Model::getTime, time).last("limit 1").one();
                    CoreHandler.addTask(new Task(Long.parseLong(cu.getFrequencyScope().split(",")[1]),
                            cu, stringRedisTemplate, model, coreService));
                });
            });
        } catch (Exception e) {
            log.info("error;{}", JSONObject.toJSONString(e));
        }


    }

    public void scanType3() {

        List<ModelTask> modelTasks = modelTaskService.lambdaQuery()
                .lt(ModelTask::getEndTime, new Date()).list();

        modelTasks.forEach(item -> item.setType(3));
        modelTaskService.updateBatchById(modelTasks);
        List<Integer> ids = modelTasks.stream().map(ModelTask::getId).collect(Collectors.toList());
        if (!ids.isEmpty())
            modelService.lambdaUpdate().in(Model::getRefId, ids).remove();
    }


}
