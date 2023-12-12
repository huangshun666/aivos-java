package com.zs.forex.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.aop.NeedLogin;
import com.zs.forex.common.dto.CryptoChainDTO;
import com.zs.forex.common.pojo.CryptoChain;
import com.zs.forex.common.pojo.User;
import com.zs.forex.common.vcenum.UserRole;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.service.CryptoChainService;
import com.zs.forex.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
public class CryptoChanController {

    @Autowired
    private CryptoChainService cryptoChainService;

    @Autowired
    private UserService userService;

    @PostMapping("/cryptoChain/getCryptoChain")
    @NeedLogin
    public ResultBody getCryptoChain(@RequestBody JSONObject jsonObject) {
        String chain = jsonObject.getString("chain");
        String coin = jsonObject.getString("coin");
        Optional<CryptoChain> any = cryptoChainService.lambdaQuery()
                .eq(CryptoChain::getType, 0).eq(CryptoChain::getChain, chain)
                .eq(CryptoChain::getCoin, coin).list().stream().findAny();
        return ResultBody.success(any.orElse(null));
    }


    @PostMapping("/cryptoChain/bindCryptoChain")
    @NeedLogin
    public ResultBody bindCryptoChain(@RequestBody CryptoChain cryptoChain) {
        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();
        Integer id = dto.getUser().getId();
        cryptoChain.setUid(id);
        boolean exists = cryptoChainService.lambdaQuery().eq(CryptoChain::getCoin, cryptoChain.getCoin())
                .eq(CryptoChain::getUid, id).exists();
        if (exists) {
            return ResultBody.error(RespCodeEnum.repeat_binding);
        }
        return ResultBody.success(cryptoChainService.save(cryptoChain));
    }


    @PostMapping("/cryptoChain/cryptoChainList")
    @NeedLogin
    public ResultBody bindCryptoChain() {
        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();
        return ResultBody.success(cryptoChainService.lambdaQuery().eq(CryptoChain::getUid
                , dto.getUser().getId()).list());
    }


    @PostMapping("/admin/cryptoChain/changeCryptoChain")
    @AdminNeedLogin
    public ResultBody adminChangeCryptoChain(@RequestBody CryptoChain cryptoChain) {
        return ResultBody.success(cryptoChainService.saveOrUpdate(cryptoChain));
    }

    @PostMapping("/admin/cryptoChain/delCryptoChain")
    @AdminNeedLogin
    public ResultBody adminDelCryptoChain(@RequestBody CryptoChain cryptoChain) {
        return ResultBody.success(cryptoChainService.removeById(cryptoChain.getId()));
    }

    @PostMapping("/admin/cryptoChain/getCryptoChain")
    @AdminNeedLogin
    public ResultBody adminGetCryptoChain(@RequestBody JSONObject jsonObject) {
        Integer uid = jsonObject.getInteger("uid");
        String email = jsonObject.getString("email");
        Integer pageIndex = jsonObject.getInteger("pageIndex");
        Integer pageSize = jsonObject.getInteger("pageSize");
        Integer type = jsonObject.getInteger("type");
        String chain = jsonObject.getString("chain");
        User one = userService.lambdaQuery().eq(User::getEmail, email).one();
        List<Integer> coll = userService.proxyChain(RequestBodyWeb.get()
                .getAdminUser().getId());
        List<Integer> list = new ArrayList<>(coll);
        list.add(0);
        Page<CryptoChain> page = cryptoChainService.lambdaQuery()
                .in(uid == null, CryptoChain::getUid, list)
                .eq(uid != null, CryptoChain::getUid, uid)
                .eq(type != null, CryptoChain::getType, type)
                .eq(chain != null, CryptoChain::getChain, chain)
                .page(new Page<>(pageIndex, pageSize));

        List<CryptoChainDTO> collect = page.getRecords().stream().map(item -> {
            Integer id = item.getUid();
            User user = userService.getById(id);
            CryptoChainDTO dto = new CryptoChainDTO();
            dto.setCryptoChain(item);
            dto.setUser(user);
            return dto;
        }).collect(Collectors.toList());

        Page<CryptoChainDTO> cryptoChainDTOPage = new Page<>();
        cryptoChainDTOPage.setRecords(collect);
        cryptoChainDTOPage.setTotal(page.getTotal());
        return ResultBody.success(cryptoChainDTOPage);
    }

}
