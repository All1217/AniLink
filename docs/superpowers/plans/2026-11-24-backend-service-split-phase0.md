# AniLink 后端微服务拆分 Phase 0 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可编译的 16 个新模块骨架，把 `AL-common` 拆成 5 个按需依赖的公共模块，修复网关身份透传并集中免登录路径，为 Phase 1 抽取 `al-user` 做好准备。

**Architecture:** Maven 多模块、包名保持 `com.video.*` 不变，只移动文件与依赖；网关用单元测试固定 `user-id`/`X-Real-IP` 注入；契约测试基座只跑通登录接口，其余域在各自切流阶段补齐。

**Tech Stack:** Java 17、Maven、Spring Boot 3.0.5、Spring Cloud 2022.0.4、Spring Cloud Gateway、JUnit 5、Mockito、AssertJ、ArchUnit 1.2.1。

**Spec:** `docs/superpowers/specs/2026-11-24-backend-service-split-design.md`（配合 `openspec/changes/refactor-backend-service-split/` 的 proposal/specs/design/tasks 一起阅读）

## Global Constraints

- JDK 17；Spring Boot 3.0.5；Spring Cloud 2022.0.4；不升级任何既有依赖版本。
- 所有命令在 `E:\Program files\AniLink\AniLink-Java` 下执行；git 根目录是 `E:\Program files\AniLink`。
- 源码 UTF-8；包名保持 `com.video.*`；新增类与方法写简短中文注释。
- 不改变对外 `/main/**` 的路径、报文与状态码；不写入真实密钥、口令或内网地址。
- 每个任务结束必须 `mvn -q -DskipTests install` 通过；带测试的任务还必须跑对应测试通过。

## File Structure

- `AL-common-core/`：`com.video.common.{constant,exception/VideoException,login,result,utils}`，无 Spring Web/Servlet。
- `AL-common-web/`：`com.video.common.{exception/GlobalExceptionHandler,utils/{CookieBuilder,WebUtils},properties}`，依赖 core 与 spring-web。
- `AL-common-mq/`：`com.video.common.config.mq`，依赖 spring-boot-starter-amqp。
- `AL-common-redis/`：`com.video.common.redis`，依赖 spring-boot-starter-data-redis。
- `AL-common-mybatis/`：`com.video.common.mybatisplus`，依赖 mybatis-plus-boot-starter。
- `AL-<service>/`：Phase 0 只有 `pom.xml` 骨架；Phase 1 起加启动类与代码。
- `AL-<service>-api/`：Feign 契约模块骨架，包名 `com.video.api.<service>`。
- `AL-architecture-test/`：ArchUnit 规则，测试作用域依赖所有服务模块。
- `AL-web/src/test/java/com/video/contract/`：契约快照工具与登录接口快照测试。

## Review Focus

1. 网关收到有效 access token 时，下游必须同时收到 `user-id` 与 `X-Real-IP`；当前实现丢弃 `build()` 返回值，不修就会全部 305。
2. 网关收到缺失或非法 token 时必须抛 305/602，且不继续调用下游。
3. 白名单匹配必须放行 `/main/public/refresh`、`/main/user/register`、`/**/public/**`，同时不能放行 `/main/user/info`。
4. 公共模块拆分后，MQ/Redis/MyBatis 的配置类仍会被服务扫描到；`@MapperScan` 不能继续写死 `com.video.web.*.mapper`。
5. `AL-communication` 拆分后的依赖树不得再出现 MinIO、MyBatis-Plus、Spring Data Redis。

---

### Task 1: 建立 5 个公共子模块骨架

**Files:**
- Modify: `pom.xml`
- Create: `AL-common-core/pom.xml`
- Create: `AL-common-web/pom.xml`
- Create: `AL-common-mq/pom.xml`
- Create: `AL-common-redis/pom.xml`
- Create: `AL-common-mybatis/pom.xml`

**Interfaces:**
- Produces: Maven artifacts `com.video:AL-common-core`、`AL-common-web`、`AL-common-mq`、`AL-common-redis`、`AL-common-mybatis`，版本 `1.0-SNAPSHOT`，均继承 `com.video:AniLink-cloud`。

- [ ] **Step 1: 创建 `AL-common-core/pom.xml`**

依赖只允许：`lombok`、`io.jsonwebtoken:jjwt-api`、`jjwt-impl`(runtime)、`jjwt-jackson`(runtime)、`cn.hutool:hutool-all`、`commons-codec:commons-codec`、`org.slf4j:slf4j-api`、`org.springframework:spring-core`。不引入 spring-web、mybatis、redis、amqp、minio。

- [ ] **Step 2: 创建其余四个公共模块 pom**

- `AL-common-web`：依赖 `AL-common-core`、`org.springframework:spring-web`、`org.springframework.boot:spring-boot`、`jakarta.servlet:jakarta.servlet-api`(provided)、`lombok`。
- `AL-common-mq`：依赖 `AL-common-core`、`org.springframework.boot:spring-boot-starter-amqp`、`com.fasterxml.jackson.core:jackson-databind`、`lombok`。
- `AL-common-redis`：依赖 `org.springframework.boot:spring-boot-starter-data-redis`、`lombok`。
- `AL-common-mybatis`：依赖 `com.baomidou:mybatis-plus-boot-starter`、`lombok`。

- [ ] **Step 3: 在父 `pom.xml` 的 `<modules>` 中注册 5 个模块**

放在 `AL-api` 之后、`AL-web` 之前；顺序不影响 Maven reactor，但保持“公共 → 服务”的可读性。

- [ ] **Step 4: 构建验证**

Run: `mvn -q -DskipTests install`
Expected: BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add pom.xml AL-common-core AL-common-web AL-common-mq AL-common-redis AL-common-mybatis
git commit -m "build: add common module skeletons"
```

---

### Task 2: 建立 7 个服务与 4 个契约模块骨架

**Files:**
- Modify: `pom.xml`
- Create: `AL-user/pom.xml`、`AL-video/pom.xml`、`AL-playback/pom.xml`、`AL-social/pom.xml`、`AL-comment/pom.xml`、`AL-danmaku/pom.xml`、`AL-datacenter/pom.xml`
- Create: `AL-user-api/pom.xml`、`AL-video-api/pom.xml`、`AL-playback-api/pom.xml`、`AL-social-api/pom.xml`

**Interfaces:**
- Produces: 服务 artifacts `com.video:AL-user`、`AL-video`、`AL-playback`、`AL-social`、`AL-comment`、`AL-danmaku`、`AL-datacenter`；契约 artifacts `AL-user-api`、`AL-video-api`、`AL-playback-api`、`AL-social-api`。

- [ ] **Step 1: 创建 7 个服务 pom**

每个服务 pom 依赖：`AL-common-core`、`AL-common-web`、`spring-boot-starter-web`、`spring-cloud-starter-alibaba-nacos-discovery`、`spring-cloud-starter-alibaba-nacos-config`、`spring-cloud-starter-bootstrap`、`AL-auth-resource-sdk`。Phase 0 不加 `spring-boot-maven-plugin`，也不加启动类。

- [ ] **Step 2: 创建 4 个契约 pom**

每个契约 pom 依赖：`AL-common-core`、`spring-cloud-starter-openfeign`。

- [ ] **Step 3: 在父 `pom.xml` 注册 11 个模块**

服务模块放在公共模块之后，契约模块紧跟对应服务模块。

- [ ] **Step 4: 构建验证**

Run: `mvn -q -DskipTests install`
Expected: BUILD SUCCESS，且 `mvn -q -pl AL-comment dependency:tree` 能看到 `AL-common-web`。

- [ ] **Step 5: 提交**

```bash
git add pom.xml AL-user AL-video AL-playback AL-social AL-comment AL-danmaku AL-datacenter AL-user-api AL-video-api AL-playback-api AL-social-api
git commit -m "build: add service and contract module skeletons"
```

---

### Task 3: 拆分 AL-common 到 5 个模块

**Files:**
- Move: `AL-common/src/main/java/com/video/common/**` → 对应模块（保持包名）
- Modify: `AL-web/pom.xml`、`AL-remark/pom.xml`、`AL-communication/pom.xml`、`AL-gateway/pom.xml`、`AL-auth/AL-auth-resource-sdk/pom.xml`
- Modify: `AL-web/src/main/java/com/video/VideoWebApplication.java`
- Modify: `AL-common-mybatis/src/main/java/com/video/common/mybatisplus/MybatisPlusConfiguration.java`
- Delete: `AL-common/`

**Interfaces:**
- Produces: 包名不变的 5 个公共 jar；`MybatisPlusConfiguration` 不再声明 `@MapperScan`。
- Consumes: Task 1 的模块坐标。

- [ ] **Step 1: 移动 core 类**

用 `git mv` 保留历史，保持相对包路径：

- `constant/Constant.java`、`JWTConstant.java`、`LikeBizTypeEnum.java`、`MinioConstant.java`、`MqConstant.java`
- `exception/VideoException.java`
- `login/LoginUser.java`、`LoginUserHolder.java`
- `result/Result.java`、`ResultCodeEnum.java`
- `utils/Base32.java`、`CodeUtil.java`、`CollUtils.java`、`JsonUtils.java`、`JwtUtil.java`、`NormalUtil.java`、`PasswordUtil.java`、`StringUtils.java`、`WSUrlUtil.java`、`AliOssUtil.java`

Run: `mvn -q -pl AL-common-core -am install`
Expected: BUILD SUCCESS。

- [ ] **Step 2: 移动 web 类**

移动到 `AL-common-web/src/main/java/com/video/common/`：

- `exception/GlobalExceptionHandler.java`
- `utils/CookieBuilder.java`、`WebUtils.java`
- `properties/AliOssProperties.java`、`MinIOProperties.java`

- [ ] **Step 3: 移动 mq、redis、mybatis 类**

- `config/mq/BasicIdMessageProcessor.java`、`DelayedMessageProcessor.java`、`MqConfig.java`、`RabbitMqHelper.java` → `AL-common-mq`
- `redis/RedisConfiguration.java` → `AL-common-redis`
- `mybatisplus/MybatisPlusConfiguration.java` → `AL-common-mybatis`

- [ ] **Step 4: 移除共享 Mapper 扫描，改为服务自声明**

在 `MybatisPlusConfiguration` 删除 `@MapperScan("com.video.web.*.mapper")` 与 `org.mybatis.spring.annotation.MapperScan` import；在 `VideoWebApplication` 增加 `@MapperScan("com.video.web.main.mapper")` 与对应 import。这是必须的：拆分后 remark 与未来服务不能被 web 的扫描规则覆盖。

- [ ] **Step 5: 更新各模块依赖**

- `AL-web`：用 5 个公共模块替换 `AL-common`；新增 `io.minio:minio` 与 `spring-boot-starter-websocket`（`OssConfiguration`、`MinioServiceImpl`、`WebsocketConfig` 直接使用）。
- `AL-remark`：用 `AL-common-core`、`AL-common-web`、`AL-common-mq`、`AL-common-redis`、`AL-common-mybatis` 替换 `AL-common`。
- `AL-communication`：用 `AL-common-core` 替换 `AL-common`，新增 `io.netty:netty-all`（Netty 直接使用）。
- `AL-gateway`：用 `AL-common-core` 替换 `AL-common`。
- `AL-auth-resource-sdk`：用 `AL-common-core` 替换 `AL-common`（`spring-webmvc` 保持 provided）。
- 父 `pom.xml`：移除 `AL-common` 模块并删除目录。

- [ ] **Step 6: 写 MapperScan 约束测试**

创建 `AL-web/src/test/java/com/video/web/main/config/MapperScanConfigTest.java`：

```java
package com.video.web.main.config;

import com.video.VideoWebApplication;
import com.video.common.mybatisplus.MybatisPlusConfiguration;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import static org.junit.jupiter.api.Assertions.*;

class MapperScanConfigTest {

    @Test
    void mapperScanIsDeclaredOnApplication() {
        MapperScan scan = VideoWebApplication.class.getAnnotation(MapperScan.class);
        assertNotNull(scan);
        assertArrayEquals(new String[]{"com.video.web.main.mapper"}, scan.value());
    }

    @Test
    void sharedConfigurationDoesNotHardcodeWebPackage() {
        assertNull(MybatisPlusConfiguration.class.getAnnotation(MapperScan.class));
    }
}
```

Run: `mvn -q -pl AL-web -am test -Dtest=MapperScanConfigTest`
Expected: 2 个测试 PASS。

- [ ] **Step 7: 构建与依赖验证**

Run: `mvn -q -DskipTests install`
Expected: BUILD SUCCESS。

Run: `mvn -q -pl AL-communication dependency:tree | Select-String -Pattern "minio|mybatis-plus|spring-data-redis"`
Expected: 无输出。

Run: `mvn -q -pl AL-web test-compile`
Expected: BUILD SUCCESS。

- [ ] **Step 8: 提交**

```bash
git add .
git commit -m "refactor: split AL-common into focused modules"
```

---

### Task 4: 修复网关身份透传

**Files:**
- Modify: `AL-gateway/pom.xml`（新增测试依赖）
- Modify: `AL-gateway/src/main/java/com/video/gateway/filter/AccountAuthFilter.java`
- Create: `AL-gateway/src/test/java/com/video/gateway/filter/AccountAuthFilterTest.java`

**Interfaces:**
- Consumes: `JwtUtil.init`、`JwtUtil.createAccessToken`、`JWTConstant.AUTHORIZATION_HEADER`、`JWTConstant.USER_ID`。
- Produces: `AccountAuthFilter.filter` 在放行时把带 `user-id`/`X-Real-IP` 的新 exchange 传给 `GatewayFilterChain`。

- [ ] **Step 1: 加测试依赖**

在 `AL-gateway/pom.xml` 增加 `org.springframework.boot:spring-boot-starter-test`（test scope）。

- [ ] **Step 2: 写失败测试**

```java
package com.video.gateway.filter;

import com.video.common.constant.JWTConstant;
import com.video.common.exception.VideoException;
import com.video.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AccountAuthFilterTest {

    private final AccountAuthFilter filter = new AccountAuthFilter();

    @BeforeAll
    static void initJwt() {
        JwtUtil.init("0123456789abcdef0123456789abcdef", 1_800_000, 604_800_000, true);
    }

    @Test
    void injectsUserIdAndClientIp() {
        String token = JwtUtil.createAccessToken(42L, "42");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, token)
                        .header("X-Forwarded-For", "10.0.0.8, 10.0.0.1")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst(JWTConstant.USER_ID));
        assertEquals("10.0.0.8", forwarded.get().getRequest().getHeaders().getFirst("X-Real-IP"));
    }

    @Test
    void passesPublicPathWithoutTokenAndForwardsIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/public/refresh")
                        .header("X-Forwarded-For", "10.0.0.9")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertNull(forwarded.get().getRequest().getHeaders().getFirst(JWTConstant.USER_ID));
        assertEquals("10.0.0.9", forwarded.get().getRequest().getHeaders().getFirst("X-Real-IP"));
    }

    @Test
    void rejectsMissingToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo").build());

        assertThrows(VideoException.class, () -> filter.filter(exchange, ex -> Mono.empty()));
    }

    @Test
    void rejectsRefreshTokenUsedAsAccessToken() {
        String refreshToken = JwtUtil.createRefreshToken(42L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, refreshToken)
                        .build());

        assertThrows(VideoException.class, () -> filter.filter(exchange, ex -> Mono.empty()));
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `mvn -q -pl AL-gateway test -Dtest=AccountAuthFilterTest`
Expected: `injectsUserIdAndClientIp` 与 `passesPublicPathWithoutTokenAndForwardsIp` 因 header 为 null 失败。

- [ ] **Step 4: 修改 `AccountAuthFilter`**

把两处 `exchange.mutate()...build();` 改为保存返回值，并把最终 `mutated` 传给 `chain.filter(mutated)`：

```java
ServerWebExchange mutated = exchange;
if (clientIp != null) {
    mutated = mutated.mutate().request(builder -> builder.header("X-Real-IP", clientIp)).build();
}
// ...白名单判断改用 mutated...
if (userId != null) {
    mutated = mutated.mutate().request(builder -> builder.header(USER_ID, userId.toString())).build();
}
return chain.filter(mutated);
```

注意：白名单分支也要用 `mutated` 放行，否则免登录路径拿不到 `X-Real-IP`。

- [ ] **Step 5: 再跑测试确认通过**

Run: `mvn -q -pl AL-gateway test -Dtest=AccountAuthFilterTest`
Expected: 3 个测试全部 PASS。

- [ ] **Step 6: 提交**

```bash
git add AL-gateway/pom.xml AL-gateway/src
git commit -m "fix: propagate user id and client ip from gateway"
```

---

### Task 5: 集中免登录路径

**Files:**
- Create: `AL-common-core/src/main/java/com/video/common/constant/AuthPaths.java`
- Create: `AL-common-core/src/test/java/com/video/common/constant/AuthPathsTest.java`
- Modify: `AL-gateway/src/main/java/com/video/gateway/filter/AccountAuthFilter.java`
- Modify: `AL-auth/AL-auth-resource-sdk/src/main/java/com/video/auth/resource/config/ResourceInterceptorConfiguration.java`

**Interfaces:**
- Produces: `AuthPaths.EXCLUDE_PATTERNS`（`List<String>`）与 `AuthPaths.isExcluded(String path): boolean`。
- Consumes: Task 1 的 `AL-common-core`；Task 4 的网关过滤器。

- [ ] **Step 1: 写失败测试**

```java
package com.video.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthPathsTest {

    @Test
    void excludesLoginRegisterRefreshAndPublicPaths() {
        assertTrue(AuthPaths.isExcluded("/main/login"));
        assertTrue(AuthPaths.isExcluded("/main/login/refresh"));
        assertTrue(AuthPaths.isExcluded("/main/user/register"));
        assertTrue(AuthPaths.isExcluded("/main/public/refresh"));
        assertTrue(AuthPaths.isExcluded("/main/video/public/pageVideo"));
    }

    @Test
    void keepsProtectedPathsProtected() {
        assertFalse(AuthPaths.isExcluded("/main/user/info"));
        assertFalse(AuthPaths.isExcluded("/main/video/pageVideo"));
        assertFalse(AuthPaths.isExcluded("/remark/like"));
    }
}
```

Run: `mvn -q -pl AL-common-core test -Dtest=AuthPathsTest`
Expected: 编译失败，`AuthPaths` 不存在。

- [ ] **Step 2: 实现 `AuthPaths`**

```java
package com.video.common.constant;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/** 网关与资源服务共用的免登录路径。 */
public final class AuthPaths {

    public static final List<String> EXCLUDE_PATTERNS = List.of(
            "/main/login/**",
            "/main/user/register",
            "/main/public/refresh",
            "/**/public/**"
    );

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private AuthPaths() {
    }

    public static boolean isExcluded(String path) {
        return EXCLUDE_PATTERNS.stream().anyMatch(pattern -> MATCHER.match(pattern, path));
    }
}
```

- [ ] **Step 3: 跑测试确认通过**

Run: `mvn -q -pl AL-common-core test -Dtest=AuthPathsTest`
Expected: 2 个测试 PASS。

- [ ] **Step 4: 网关改用 `AuthPaths`**

删除 `AccountAuthFilter` 里的 `excludePath` 集合与 `isExcludePath` 方法，改为 `AuthPaths.isExcluded(path)`；保留 `AntPathMatcher` 仅在需要时使用，若不再使用则删除 import 与字段。

- [ ] **Step 5: SDK 默认使用同一份白名单**

在 `ResourceInterceptorConfiguration` 中，当 `authProperties.getExcludeLoginPaths()` 为空时使用 `AuthPaths.EXCLUDE_PATTERNS`：

```java
List<String> excludes = CollUtil.isNotEmpty(authProperties.getExcludeLoginPaths())
        ? authProperties.getExcludeLoginPaths()
        : AuthPaths.EXCLUDE_PATTERNS;
registration.excludePathPatterns(excludes.toArray(String[]::new));
```

保留原有 swagger 排除项。

- [ ] **Step 6: 回归**

Run: `mvn -q -pl AL-gateway test -Dtest=AccountAuthFilterTest`
Expected: 3 个测试 PASS。

Run: `mvn -q -DskipTests install`
Expected: BUILD SUCCESS。

- [ ] **Step 7: 提交**

```bash
git add AL-common-core AL-gateway AL-auth
git commit -m "refactor: centralize auth whitelist paths"
```

---

### Task 6: 引入 ArchUnit 边界规则

**Files:**
- Create: `AL-architecture-test/pom.xml`
- Create: `AL-architecture-test/src/test/java/com/video/arch/ServiceBoundaryTest.java`
- Modify: `pom.xml`

**Interfaces:**
- Produces: `ServiceBoundaryTest`，对 7 个服务包互相依赖的规则；后续新增服务代码时自动生效。

- [ ] **Step 1: 创建测试模块 pom**

依赖：`com.tngtech.archunit:archunit-junit5:1.2.1`、`org.junit.jupiter:junit-jupiter`，以及 7 个服务模块（test 作用域即可，ArchUnit 需要类路径）。在父 `pom.xml` 注册 `AL-architecture-test`。

- [ ] **Step 2: 写规则**

```java
package com.video.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.video", importOptions = ImportOption.DoNotIncludeTests.class)
class ServiceBoundaryTest {

    private static final String[] SERVICES = {
            "com.video.user..", "com.video.video..", "com.video.playback..",
            "com.video.social..", "com.video.comment..", "com.video.danmaku..",
            "com.video.datacenter.."
    };

    @ArchTest
    static final ArchRule user_is_independent = ruleFor("com.video.user..");
    @ArchTest
    static final ArchRule video_is_independent = ruleFor("com.video.video..");
    @ArchTest
    static final ArchRule playback_is_independent = ruleFor("com.video.playback..");
    @ArchTest
    static final ArchRule social_is_independent = ruleFor("com.video.social..");
    @ArchTest
    static final ArchRule comment_is_independent = ruleFor("com.video.comment..");
    @ArchTest
    static final ArchRule danmaku_is_independent = ruleFor("com.video.danmaku..");
    @ArchTest
    static final ArchRule datacenter_is_independent = ruleFor("com.video.datacenter..");

    private static ArchRule ruleFor(String ownPackage) {
        String[] others = Arrays.stream(SERVICES)
                .filter(pkg -> !pkg.equals(ownPackage))
                .toArray(String[]::new);
        return noClasses().that().resideInAPackage(ownPackage)
                .should().dependOnClassesThat().resideInAnyPackage(others);
    }
}
```

- [ ] **Step 3: 运行规则**

Run: `mvn -q -pl AL-architecture-test test`
Expected: BUILD SUCCESS（Phase 0 服务模块为空，规则应通过）。

- [ ] **Step 4: 提交**

```bash
git add pom.xml AL-architecture-test
git commit -m "test: add ArchUnit service boundary rules"
```

---

### Task 7: 契约测试基座与登录快照

**Files:**
- Create: `AL-web/src/test/java/com/video/contract/ContractSnapshot.java`
- Create: `AL-web/src/test/java/com/video/contract/LoginContractTest.java`
- Create: `AL-web/src/test/resources/contracts/login-success.json`

**Interfaces:**
- Produces: `ContractSnapshot.read(String name): String`，从 `src/test/resources/contracts/` 读取快照文本。
- Consumes: `LoginController` 的 `POST /main/login` 与 `LoginService.login(LoginVo)`。

- [ ] **Step 1: 写快照工具**

```java
package com.video.contract;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 读取 src/test/resources/contracts 下的接口快照。 */
public final class ContractSnapshot {

    private ContractSnapshot() {
    }

    public static String read(String name) throws IOException {
        try (InputStream in = ContractSnapshot.class.getResourceAsStream("/contracts/" + name)) {
            if (in == null) {
                throw new IllegalStateException("missing contract snapshot: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 2: 写失败测试**

```java
package com.video.contract;

import com.video.web.main.controller.login.LoginController;
import com.video.web.main.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
class LoginContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginService loginService;

    @Test
    void loginReturnsWrappedAccessToken() throws Exception {
        when(loginService.login(any())).thenReturn("access-token-123");

        mockMvc.perform(post("/main/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("login-success.json"), true));
    }
}
```

Run: `mvn -q -pl AL-web test -Dtest=LoginContractTest`
Expected: 失败，`contracts/login-success.json` 不存在。

- [ ] **Step 3: 写快照文件**

`AL-web/src/test/resources/contracts/login-success.json`：

```json
{
  "code": 200,
  "message": "成功",
  "data": "access-token-123"
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn -q -pl AL-web test -Dtest=LoginContractTest`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add AL-web/src/test/java/com/video/contract AL-web/src/test/resources/contracts
git commit -m "test: add contract snapshot base and login snapshot"
```

---

### Task 8: 更新文档

**Files:**
- Modify: `AniLink-Java/README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: 与新模块、端口、启动顺序一致的文档；无代码接口变化。

- [ ] **Step 1: 更新 `AniLink-Java/README.md`**

补充模块列表（5 个公共模块、7 个服务、4 个契约模块）、端口表（8021/8023/8024/8025–8032/8090）、推荐启动顺序：gateway → communication → remark → user → video → playback → social → comment → danmaku → datacenter。

- [ ] **Step 2: 更新根 `AGENTS.md`**

在“仓库结构”表增加新模块行，并在“主链路”中把 `/main/**` 的路由描述改为按子路径分流（Phase 0 仍全部指向 AL-web，需注明“迁移中”）。

- [ ] **Step 3: 文档验证**

Run: `Select-String -Path README.md,..\AGENTS.md -Pattern "AL-common-core","8025","userservice"`
Expected: 三处均能命中。

- [ ] **Step 4: 提交**

```bash
git add README.md ../AGENTS.md
git commit -m "docs: update module layout and startup order"
```

---

### Task 9: 网关路由外置

**Files:**
- Create: `AL-gateway/src/main/resources/routes.yml`
- Modify: `AL-gateway/src/main/resources/bootstrap.yml`
- Modify: `AL-gateway/src/main/resources/application.yml`

**Interfaces:**
- Produces: 两个路由定义 `remark-service`（`lb://remarkservice`，`Path=/remark/**`）与 `web-service`（`lb://webservice`，`Path=/main/**`），从 `bootstrap.yml` 移到独立配置文件。

- [ ] **Step 1: 创建 `routes.yml`**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: remark-service
          uri: lb://remarkservice
          predicates:
            - Path=/remark/**
        - id: web-service
          uri: lb://webservice
          predicates:
            - Path=/main/**
```

- [ ] **Step 2: 在 `application.yml` 引入并删除旧配置**

在 `AL-gateway/src/main/resources/application.yml` 顶部增加：

```yaml
spring:
  config:
    import: optional:classpath:routes.yml
```

同时从 `bootstrap.yml` 删除 `spring.cloud.gateway.routes` 整段，保留 Nacos 与 `spring.application.name`。

- [ ] **Step 3: 验证**

Run: `Select-String -Path src/main/resources/routes.yml -Pattern "lb://remarkservice","lb://webservice"`
Expected: 两行命中。

Run: `mvn -q -pl AL-gateway test`
Expected: BUILD SUCCESS（Phase 0 不做真实转发，转发联调留到 Phase 1 切流时验证）。

- [ ] **Step 4: 提交**

```bash
git add AL-gateway/src/main/resources
git commit -m "refactor: externalize gateway routes"
```

---

## Self-Review

- **Spec coverage:** Phase 0 覆盖 OpenSpec 任务 1.1–1.8、1.10、1.11；1.6 由 Task 9 外置路由配置，1.9 只搭基座与登录示例，其余域的契约快照按设计在各自切流阶段补齐。Phase 1–4 的任务由后续独立计划覆盖。
- **Step scan:** 每个步骤都有单一动作与可检查结果；模块移动与依赖清单给定，测试代码给定断言与命令。
- **Type consistency:** `AuthPaths.isExcluded`、`ContractSnapshot.read`、`AccountAuthFilter.filter` 的签名在任务间一致；`JWTConstant.USER_ID` 与 `JWTConstant.AUTHORIZATION_HEADER` 为现有常量。
- **Review Focus:** 5 条都对应到 Task 4/5/3 的测试或依赖检查。
