package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.common.mapper.MediaMapper;
import com.zs.forex.common.pojo.Media;
import com.zs.forex.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseServiceImpl implements BaseService {
    @Autowired
    private MediaMapper mediaMapper;

    @Override
    public boolean addMedia(Media media) {
        return mediaMapper.insert(media) > 0;
    }

    @Override
    public List<Media> getList(Media media) {
        return mediaMapper.selectList(new LambdaQueryWrapper<Media>().eq(media.getType() != null,
                        Media::getType, media.getType())
                .eq(media.getGroup() != null, Media::getGroup, media.getGroup()));
    }

    public Page<Media> getList(Integer pageIndex, Integer pageSize, Media media) {
        return mediaMapper.selectPage(new Page<>(pageIndex, pageSize), new LambdaQueryWrapper<Media>().eq(media.getType() != null,
                        Media::getType, media.getType())
                .eq(media.getGroup() != null, Media::getGroup, media.getGroup()));
    }


    @Override
    public boolean delMedia(Integer id) {
        return mediaMapper.deleteById(id) > 0;
    }
}
