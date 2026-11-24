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
                .should().dependOnClassesThat().resideInAnyPackage(others)
                .allowEmptyShould(true);
    }
}
