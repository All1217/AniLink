package com.video.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.video", importOptions = ImportOption.DoNotIncludeTests.class)
class ServiceBoundaryTest {

    private static final String[] SERVICES = {
            "com.video.user..", "com.video.video..", "com.video.playback..",
            "com.video.social..", "com.video.comment..", "com.video.danmaku..",
            "com.video.datacenter.."
    };

    /** 迁移期仍存在的实现包；新服务不得反向依赖它们。 */
    private static final String[] LEGACY = {
            "com.video.remark..", "com.video.communication..", "com.video.gateway.."
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
        String[] others = Stream.concat(
                        Arrays.stream(SERVICES).filter(pkg -> !pkg.equals(ownPackage)),
                        Arrays.stream(LEGACY))
                .toArray(String[]::new);
        return noClasses().that().resideInAPackage(ownPackage)
                .should().dependOnClassesThat().resideInAnyPackage(others)
                .allowEmptyShould(true);
    }
}
