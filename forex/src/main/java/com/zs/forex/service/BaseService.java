package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.common.pojo.Media;

import java.util.List;

public interface BaseService {

    boolean addMedia(Media media);

    Page<Media> getList(Integer pageIndex, Integer pageSize, Media media);

    List<Media> getList(Media media);

    boolean delMedia(Integer id);
}
