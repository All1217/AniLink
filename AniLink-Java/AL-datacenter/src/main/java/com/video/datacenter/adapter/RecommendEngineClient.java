package com.video.datacenter.adapter;

import com.video.datacenter.config.EngineProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

/**
 * Flask 算法引擎客户端：实时推荐与相似用户。
 *
 * <p>Flask 不可达、返回错误或返回结构异常时统一降级为空列表，接口返回一致的空结果而不是 500。
 * 引擎自身异常时也会返回 []，因此这里只需处理网络/协议错误。</p>
 */
@Slf4j
@Component
public class RecommendEngineClient {

    /** Flask 推荐接口未传 count 时的默认值，与 /recommendations/<uid> 的默认一致 */
    private static final int DEFAULT_RECOMMEND_COUNT = 3;

    private final RestTemplate restTemplate;
    private final EngineProperties engineProperties;

    public RecommendEngineClient(RestTemplate restTemplate, EngineProperties engineProperties) {
        this.restTemplate = restTemplate;
        this.engineProperties = engineProperties;
    }

    /** 取实时推荐视频 id 列表；引擎不可用返回空列表 */
    public List<Long> recommendVideoIds(Long uid, Integer count) {
        if (uid == null) {
            return List.of();
        }
        int size = count == null || count <= 0 ? DEFAULT_RECOMMEND_COUNT : count;
        String url = engineProperties.getEngineUrl() + "/recommendations/" + uid + "?count=" + size;
        return getLongList(url, "实时推荐");
    }

    /** 取与指定用户相似的用户 id 列表；引擎不可用返回空列表 */
    public List<Long> similarUserIds(Long vid, Long uid) {
        if (vid == null || uid == null) {
            return List.of();
        }
        String url = engineProperties.getEngineUrl() + "/findSimilar/" + vid + "/" + uid;
        return getLongList(url, "相似用户");
    }

    /** GET 数字数组并统一转 Long；网络/协议异常降级为空列表 */
    private List<Long> getLongList(String url, String scene) {
        try {
            List<?> raw = restTemplate.getForObject(url, List.class);
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            // JSON 数字默认反序列化成 Integer，统一转 Long
            return raw.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> item instanceof Number)
                    .map(item -> ((Number) item).longValue())
                    .toList();
        } catch (RestClientException e) {
            log.warn("算法引擎不可达，{}降级为空结果：url={}", scene, url, e);
            return List.of();
        }
    }
}
