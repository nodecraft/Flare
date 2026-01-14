package com.nodecraft.hytale.flare.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.validator.EqualValidator;

public class MonitorConfig {
    public static final BuilderCodec<MonitorConfig> CODEC = BuilderCodec.builder(MonitorConfig.class, MonitorConfig::new)
            .append(
                    new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (config, value) -> config.enabled = value,
                    config -> config.enabled
            )
            .documentation("Should this monitor be enabled")
            .add()
            .build();

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }
}
