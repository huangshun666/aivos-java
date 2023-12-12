package com.zs.forex.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.OnlineWithdrawRecordMapper;
import com.zs.forex.common.pojo.OnlineWithdrawRecord;
import com.zs.forex.service.OnlineWithdrawRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OnlineWithdrawRecordServiceImpl extends ServiceImpl<OnlineWithdrawRecordMapper, OnlineWithdrawRecord> implements OnlineWithdrawRecordService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void execWithdraw(String address, Integer uid, String chain, BigDecimal money, String orderId) {
        int status = 0;
        String result = null;
        Date execTime = new Date();
        Map<String, String> fromMap = new HashMap<>();
        fromMap.put("order_id", orderId);
        fromMap.put("address", address);
        fromMap.put("chain", chain.equalsIgnoreCase("erc20") ? "1" : "2");
        fromMap.put("money", money.toPlainString());
        String param = JSONObject.toJSONString(fromMap);
        HttpRequest post = HttpUtil.createPost("https://tibi.apps.vin/api.php/invest/atm/index");
        post.header("Content-Type", "multipart/form-data");
        post.header("app-key", "123456789");
        post.formStr(fromMap);
        post.setReadTimeout(5000);
        try {
            HttpResponse execute = post.execute();
            String body = execute.body();
            log.info(body);
            if (body != null) {
                JSONObject jsonObject = JSONObject.parseObject(body);
                result = jsonObject.toString();
                if (jsonObject.getInteger("code") != 1) {
                    status = 1;
                }
            } else {
                status = 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            status = 1;
        }
        OnlineWithdrawRecord onlineWithdrawRecord = new OnlineWithdrawRecord();
        onlineWithdrawRecord.setStatus(status);
        onlineWithdrawRecord.setUid(uid);
        onlineWithdrawRecord.setResult(result);
        onlineWithdrawRecord.setExecTime(execTime);
        onlineWithdrawRecord.setParam(param);
        onlineWithdrawRecord.setOrderId(orderId);
        boolean save = this.save(onlineWithdrawRecord);
        log.info("添加打款记录 :{}", save);
    }


    @Scheduled(cron = "0 0/1 * * * *")
    @Override
    public void execSelectInfo() {

        List<OnlineWithdrawRecord> list = this.lambdaQuery().eq(OnlineWithdrawRecord::getStatus, 0).list();
        list.forEach(item -> {
            Map<String, String> fromMap = new HashMap<>();
            fromMap.put("order_id", item.getOrderId());
            HttpRequest post = HttpUtil.createPost("https://tibi.apps.vin/api.php/invest/atm/detail");
            post.header("Content-Type", "multipart/form-data");
            post.header("app-key", "123456789");
            post.formStr(fromMap);
            post.setReadTimeout(5000);
            try {
                HttpResponse execute = post.execute();
                String body = execute.body();
                if (body != null) {
                    JSONObject jsonObject = JSONObject.parseObject(body);
                    if (jsonObject.getInteger("code") == 1) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        Integer transStatus = data.getInteger("status");
                        if (transStatus == 1) {
                            item.setStatus(2);
                            item.setEndTime(new Date(data.getLong("add_time")));
                        }
                    }
                }
            } catch (Exception e) {
                log.info("{} 查询异常", item.getOrderId());
            }
            this.updateById(item);
        });

    }


}
