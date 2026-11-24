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
