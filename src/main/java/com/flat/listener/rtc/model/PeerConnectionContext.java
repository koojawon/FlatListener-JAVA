package com.flat.listener.rtc.model;

import dev.onvoid.webrtc.RTCRtpTransceiverDirection;
import java.util.UUID;

public class PeerConnectionContext {
    public final String uniqueId = UUID.randomUUID().toString();
    public RTCRtpTransceiverDirection audioDirection;

}
