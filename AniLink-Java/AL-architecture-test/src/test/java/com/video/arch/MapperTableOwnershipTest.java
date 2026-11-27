package com.video.arch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mapper XML 的表名归属检查：Phase 1 起每个服务模块只能引用自己拥有的表。
 * 服务模块尚未创建时本测试自动跳过，不会在 Phase 0 误报。
 */
class MapperTableOwnershipTest {

    /** 服务模块 → 允许出现的表 */
    private static final Map<String, Set<String>> OWNERSHIP = Map.of(
            "AL-user", Set.of("user", "user_stats", "user_tag"),
            "AL-video", Set.of("video", "video_stats", "user_video"),
            "AL-playback", Set.of("bh_video"),
            "AL-social", Set.of("follow", "favorite", "favorite_video"),
            "AL-comment", Set.of("comment"),
            "AL-danmaku", Set.of("danmu", "sub_danmu"),
            "AL-communication", Set.of("chat", "chat_message")
    );

    /** 匹配 from / join / into / update / delete from 后面的表名，兼容 `schema`.`table` 与反引号 */
    private static final Pattern TABLE_REF = Pattern.compile(
            "(?i)\\b(?:from|join|into|update)\\s+(?:`?\\w+`?\\.)?`?(\\w+)`?");

    @Test
    void mapperXmlUsesOnlyOwnedTables() throws IOException {
        Path repoRoot = javaRepoRoot();
        List<String> violations = new ArrayList<>();
        OWNERSHIP.forEach((module, owned) -> {
            Path mapperDir = repoRoot.resolve(module).resolve("src/main/resources/mapper");
            if (!Files.isDirectory(mapperDir)) {
                return;
            }
            try (Stream<Path> files = Files.list(mapperDir)) {
                files.filter(path -> path.toString().endsWith(".xml"))
                        .forEach(path -> violations.addAll(scan(path, owned)));
            } catch (IOException e) {
                violations.add(module + " 读取失败: " + e.getMessage());
            }
        });
        assertTrue(violations.isEmpty(), "Mapper XML 出现非属主表：\n" + String.join("\n", violations));
    }

    @Test
    void scannerFlagsForeignTable(@TempDir Path tempDir) throws IOException {
        Path mapper = tempDir.resolve("BadMapper.xml");
        Files.writeString(mapper,
                "<mapper><select id=\"x\">select * from `video` v join `user` u on v.uid = u.uid</select></mapper>",
                StandardCharsets.UTF_8);

        List<String> violations = scan(mapper, Set.of("video", "video_stats", "user_video"));

        assertEquals(1, violations.size(), "应只报 user 这一处越界：" + violations);
        assertTrue(violations.get(0).contains("user"));
    }

    /** 扫描单个 Mapper XML，返回非属主表引用描述 */
    private static List<String> scan(Path mapperXml, Set<String> owned) {
        List<String> violations = new ArrayList<>();
        String sql;
        try {
            sql = Files.readString(mapperXml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            violations.add(mapperXml + " 读取失败: " + e.getMessage());
            return violations;
        }
        Matcher matcher = TABLE_REF.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase();
            if (!owned.contains(table)) {
                violations.add(mapperXml.getFileName() + " 引用了非属主表 " + table);
            }
        }
        return violations;
    }

    /** 测试运行时的工作目录是 AL-architecture-test，父目录才是 Maven 根 */
    private static Path javaRepoRoot() {
        Path cwd = Paths.get("").toAbsolutePath();
        return "AL-architecture-test".equals(cwd.getFileName().toString()) ? cwd.getParent() : cwd;
    }
}
