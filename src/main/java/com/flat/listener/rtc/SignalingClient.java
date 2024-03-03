package com.flat.listener.rtc;

import com.flat.listener.rtc.model.Contact;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;

public interface SignalingClient {

    void joinRoom(Contact asContact) throws Exception;

    void sendSdpOffer(Contact contact, RTCSessionDescription obj, String uuid) throws Exception;

    void sendIceCandidate(final RTCIceCandidate iceCandidate, String uuid);

    void setSignalingListener(SignalingListener listener);

}
