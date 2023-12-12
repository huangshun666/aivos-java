package com.zs.forex.controller;

import com.alibaba.fastjson.JSONObject;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.aop.NeedLogin;
import com.zs.forex.common.pojo.AdminUser;
import com.zs.forex.common.pojo.Media;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.service.BaseService;
import com.zs.forex.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaseController {

    @Autowired
    private BaseService baseService;

    @Autowired
    private SummaryService summaryService;

    @PostMapping("admin/base/addMedia")
    @AdminNeedLogin
    public ResultBody addMedia(@RequestBody Media media) {

        return ResultBody.success(baseService.addMedia(media));
    }

    @PostMapping("admin/base/getMedia")
    @AdminNeedLogin
    public ResultBody adminGetMedia(@RequestBody JSONObject jsonObject) {
        Media media = jsonObject.toJavaObject(Media.class);
        Integer pageIndex = jsonObject.getInteger("pageIndex");
        Integer pageSize = jsonObject.getInteger("pageSize");
        return ResultBody.success(baseService.getList(pageIndex, pageSize, media));
    }

    @PostMapping("/base/getMedia")
    public ResultBody getMedia(@RequestBody Media media) {
        return ResultBody.success(baseService.getList(media));
    }

    @PostMapping("admin/base/delMedia")
    @AdminNeedLogin
    public ResultBody delMedia(@RequestBody Media media) {

        return ResultBody.success(baseService.delMedia(media.getId()));
    }

    @PostMapping("admin/base/indexData")
    @AdminNeedLogin
    public ResultBody indexData() {
        AdminUser adminUser = RequestBodyWeb.get().getAdminUser();
        return ResultBody.success(summaryService.indexData(adminUser.getId()));
    }
}
