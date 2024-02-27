package com.flat.listener.rtc.apprtc;

import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCSessionDescription;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AppRTCSignalingParameters {


    public final List<RTCIceServer> iceServers;
    public final boolean initiator;
    public final String clientId;
    public final String wssUrl;
    public final String wssPostUrl;
    public final String iceServerUrl;
    public final RTCSessionDescription offer;

    public final List<RTCIceCandidate> iceCandidates;


}
