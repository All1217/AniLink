package com.video.user.contract;

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
