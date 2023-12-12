package com.zs.self.task;

import com.alibaba.fastjson.JSONObject;
import com.zs.self.pojo.Model;
import com.zs.self.pojo.ModelTask;
import com.zs.self.service.CoreService;
import lombok.AllArgsConstructor;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Slf4j
@AllArgsConstructor
public class Task implements Runnable {

    private long lastTime;
    private ModelTask modelTask;

    private StringRedisTemplate stringRedisTemplate;

    private Model model;

    private CoreService coreService;

    @Override
    public void run() {
        try {

            String[] frequencyScope = modelTask.getFrequencyScope().split(",");

            int index = 0;

            Date crnD = new Date();

            Date stopD = crnD;


            if (model == null) {
                log.info("{},{},{}, 无model停止", (new Date().getTime() / 1000), modelTask.getSymbol(), modelTask.getMarket());
                return;
            }

            log.info("{},{},{}, 开始启动", modelTask.getSymbol(), modelTask.getMarket(), model.getTime());

            while (crnD.getTime() < stopD.getTime() + (1000 * 60)) {
                log.warn("generate task 111");
                if (Objects.equals(stringRedisTemplate.opsForValue().get(modelTask.getUuid()), "1")) {
                    log.info("{},{}, 强制停止", modelTask.getSymbol(), modelTask.getMarket());
                    return;
                }
                log.warn("generate task 222");
                long last = stopD.getTime() + (1000 * 60) - crnD.getTime();

                log.warn("generate task 333");
                if (last <= lastTime) {
                    log.warn("generate task 444");
                    BigDecimal close = model.getClose();
                    coreService.generateData(close, index, model, modelTask);
                    break;
                }
                sleep(RandomUtil.randomInt(Integer.parseInt(frequencyScope[0]),
                        Integer.parseInt(frequencyScope[1])));

                log.warn("generate task 555");
                coreService.generateData(null, index, model, modelTask);

                crnD = new Date();

                ++index;
            }
            log.info("{},{}, 正常停止", modelTask.getSymbol(), modelTask.getMarket());
        } catch (Exception e) {
            log.info("error;{}", JSONObject.toJSONString(e));
        }
    }


    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
