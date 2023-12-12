package com.zs.forex.controller;

import com.zs.forex.common.tools.MinIoTool;
import com.zs.forex.common.web.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@RestController
public class CommonController {


    @Autowired
    private MinIoTool minIoTools;

    @Autowired
    private JdbcTemplate currentJdbcTemplate;


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 通用上传请求
     */
    @PostMapping("/common/upload")
    public ResultBody upload(@RequestParam("file") MultipartFile file) {
        return ResultBody.success(minIoTools.uploadMinio(file));
    }


    /**
     * 通用上传请求
     */
    @PostMapping("/common/uploads")
    public ResultBody upload(MultipartFile[] file) {
        List<String> list = new ArrayList<>();
        for (MultipartFile multipartFile : file) {
            String uploadMinio = minIoTools.uploadMinio(multipartFile);
            list.add(uploadMinio);
        }
        return ResultBody.success(list);
    }


    @PostMapping("/common/restSetData")
    public ResultBody restSetData() {
        currentJdbcTemplate.execute("TRUNCATE `forex`.`order`;");
        currentJdbcTemplate.execute("TRUNCATE `forex`.`order_record`;");
        currentJdbcTemplate.execute("TRUNCATE `forex`.`user_account_record`;");
        currentJdbcTemplate.execute("TRUNCATE `forex`.`user_account_trade`;");
        Set<String> keys = stringRedisTemplate.keys("position_*");
        Set<String> keyso = stringRedisTemplate.keys("login_*");
        Set<String> keyst = stringRedisTemplate.keys("user_account_*");
        Set<String> keysth = stringRedisTemplate.keys("liquidation_*");
        if (keys != null) {
            stringRedisTemplate.delete(keys);
        }
        if (keyso != null) {
            stringRedisTemplate.delete(keyso);
        }
        if (keyst != null) {
            stringRedisTemplate.delete(keyst);
        }
        if (keysth != null) {
            stringRedisTemplate.delete(keysth);
        }
        return ResultBody.success();
    }


}
