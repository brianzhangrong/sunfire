package com.ihomefnt.sunfire.agent.generator;

import java.util.UUID;

public class RowNameGenerator implements Generator {

    public String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
