package com.flat.listener.rtc.model;

import dev.onvoid.webrtc.RTCConfiguration;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PeerConnectionConfiguration {
    private final RTCConfiguration rtcConfiguration;

    private final AudioConfiguration audioConfig;

    public PeerConnectionConfiguration() {
        rtcConfiguration = new RTCConfiguration();
        audioConfig = new AudioConfiguration();
    }

}
