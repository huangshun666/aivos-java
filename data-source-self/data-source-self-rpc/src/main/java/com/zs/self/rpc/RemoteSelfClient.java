package com.zs.self.rpc;

import com.zs.self.api.service.RemoteSelfService;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "data-source-self")
public interface RemoteSelfClient extends RemoteSelfService {
}
