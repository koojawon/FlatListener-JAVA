package com.flat.listener.rtc.model;

import dev.onvoid.webrtc.RTCIceServer;
import java.util.List;
import lombok.Getter;

@Getter
public class RoomParameters {
    private final boolean initiator;

    private final List<RTCIceServer> iceServers;


    public RoomParameters(boolean initiator, List<RTCIceServer> iceServers) {
        this.initiator = initiator;
        this.iceServers = iceServers;
    }

}
