package com.flat.listener.config;

import dev.onvoid.webrtc.RTCConfiguration;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Configuration {
    private final RTCConfiguration rtcConfiguration;

    private final AudioConfiguration audioConfig;


    public Configuration() {
        rtcConfiguration = new RTCConfiguration();
        audioConfig = new AudioConfiguration();
    }

}
