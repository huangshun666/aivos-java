package com.zs.self.api.service;

import com.alibaba.fastjson.JSONObject;
import com.zs.market.api.web.ResultBody;
import com.zs.self.api.dto.ModelDTO;
import com.zs.self.api.dto.ModelTaskDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface RemoteSelfService {

    @PostMapping("/self/saveOrUpdateModel")
    ResultBody saveModel(@RequestBody List<ModelDTO> list);

    @PostMapping("/self/saveOrUpdateModelTask")
    ResultBody saveModelTask(@RequestBody ModelTaskDTO modelTaskDTO);

    @PostMapping("/self/modelTaskList")
    ResultBody modelTaskList(@RequestBody JSONObject jsonObject);

    @PostMapping("/self/modelList")
    ResultBody modelList(@RequestBody JSONObject jsonObject);

    @PostMapping("/self/execModel")
    ResultBody execModel();

    @PostMapping("/self/overtime")
    ResultBody overtime();
}
