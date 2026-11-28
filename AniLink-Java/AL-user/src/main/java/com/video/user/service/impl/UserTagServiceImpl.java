package com.video.user.service.impl;

import com.video.api.user.dto.UserTagVo;
import com.video.model.entity.UserTag;
import com.video.user.mapper.UserTagMapper;
import com.video.user.service.UserTagService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** 用户标签查询实现：只访问 user_tag 属主表。 */
@Service
public class UserTagServiceImpl implements UserTagService {

    private final UserTagMapper userTagMapper;

    public UserTagServiceImpl(UserTagMapper userTagMapper) {
        this.userTagMapper = userTagMapper;
    }

    @Override
    public List<String> commonTags(Long uid, Long otherUid) {
        if (uid == null || otherUid == null) {
            return List.of();
        }
        return userTagMapper.listCommonTagNames(uid, otherUid);
    }

    @Override
    public List<UserTagVo> batchTags(List<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return List.of();
        }
        // 去重并过滤空值，避免 IN 条件出现 NULL
        List<Long> distinct = uids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        return userTagMapper.listByUids(distinct).stream().map(this::toVo).toList();
    }

    @Override
    public List<Long> uidsByTag(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return List.of();
        }
        return userTagMapper.listUidsByTagName(tagName);
    }

    /** 实体转跨服务 DTO，只保留数据中心聚合需要的字段 */
    private UserTagVo toVo(UserTag tag) {
        UserTagVo vo = new UserTagVo();
        vo.setUid(tag.getUid());
        vo.setTagName(tag.getTagName());
        vo.setWeight(tag.getWeight());
        return vo;
    }
}
