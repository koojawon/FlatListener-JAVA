package com.flat.listener.rtc.apprtc;

import com.flat.listener.rtc.model.Contact;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import org.springframework.stereotype.Component;

@Component
public class JsonObjectEncoder {

    private Gson gson;

    public String toOfferJson(Contact contact, RTCSessionDescription sdp, String uuid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "viewer");
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("targetId", contact.getId());
        jsonObject.addProperty("sdpOffer", sdp.sdp);
        return gson.toJson(jsonObject);
    }

    public String toIceCandidateJson(RTCIceCandidate candidate, String uuid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "iceCandidate");
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("candidate", candidate.sdp);
        jsonObject.addProperty("sdpMid", candidate.sdpMid);
        jsonObject.addProperty("sdpMLineIndex", candidate.sdpMLineIndex);
        return gson.toJson(jsonObject);
    }
}
