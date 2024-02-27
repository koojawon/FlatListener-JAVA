package com.flat.listener.rtc.model;

import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtpTransceiverDirection;
import dev.onvoid.webrtc.RTCStatsReport;
import dev.onvoid.webrtc.media.video.VideoFrame;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PeerConnectionContext {
    public RTCRtpTransceiverDirection audioDirection;
    public String uniqueId = UUID.randomUUID().toString();

    public Consumer<RTCStatsReport> onStatsReport;

    public BiConsumer<Contact, VideoFrame> onRemoteFrame;

    public BiConsumer<Contact, Boolean> onRemoteVideoStream;

    public BiConsumer<Contact, RTCPeerConnectionState> onPeerConnectionState;

}
