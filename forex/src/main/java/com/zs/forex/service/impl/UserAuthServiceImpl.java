package com.zs.forex.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.UserAuthMapper;
import com.zs.forex.common.pojo.UserAuth;
import com.zs.forex.common.tools.GZipTool;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.service.UserAuthService;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
public class UserAuthServiceImpl extends ServiceImpl<UserAuthMapper, UserAuth> implements UserAuthService {

}
