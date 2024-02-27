package com.flat.listener.rtc;

import com.flat.listener.rtc.model.Contact;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;

public interface SignalingListener {

    void onRoomJoined(Contact contact);

    void onRoomLeft();

    void onRemoteSessionDescription(Contact contact,
                                    RTCSessionDescription description);

    void onRemoteIceCandidate(Contact contact, RTCIceCandidate candidate);

    void onRemoteIceCandidatesRemoved(Contact contact,
                                      RTCIceCandidate[] candidates);


    void onError(String message);
}
