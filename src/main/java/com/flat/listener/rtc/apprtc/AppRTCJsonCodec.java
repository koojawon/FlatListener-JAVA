package com.flat.listener.rtc.apprtc;

import static java.util.Objects.nonNull;

import com.flat.listener.message.entity.RequestMessage;
import com.flat.listener.rtc.apprtc.RabbitMessage.ID;
import com.flat.listener.rtc.model.Contact;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppRTCJsonCodec {
    public RabbitMessage toJavaMessage(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        String response = null;
        if (jsonObject.has("response")) {
            response = jsonObject.get("response").getAsString();
        }

        if (nonNull(response) && response.equals("rejected")) {
            return new RabbitMessage(ID.ERROR, jsonObject.get("description").getAsJsonObject());
        }

        String type = jsonObject.get("id").getAsString();

        return switch (type) {
            case "iceCandidate" -> new RabbitMessage(ID.iceCandidate, toJavaCandidate(jsonObject));
            case "viewerResponse" -> new RabbitMessage(ID.viewerResponse, toJavaSessionDescription(jsonObject));
            case "stopCommunication" -> new RabbitMessage(ID.stopCommunication);
            case "targetInfo" -> new RabbitMessage(ID.targetInfo, toJavaTargetInfo(jsonObject));
            case "File" -> new RabbitMessage(ID.File, toJavaFileInfo(jsonObject));
            default -> new RabbitMessage(ID.ERROR, "Unexpected message: " + json);
        };
    }

    private Contact toJavaTargetInfo(JsonObject jsonObject) {
        return new Contact(jsonObject.get("targetId").getAsString());
    }


    private RequestMessage toJavaFileInfo(JsonObject json) {
        return RequestMessage.builder().fileUid(json.get("fileUid").getAsString()).build();
    }

    private RTCIceCandidate toJavaCandidate(JsonObject json) {
        JsonObject candidate = json.get("candidate").getAsJsonObject();
        return new RTCIceCandidate(
                candidate.get("sdpMid").getAsString(),
                candidate.get("sdpMLineIndex").getAsInt(),
                candidate.get("candidate").getAsString());
    }

    private RTCSessionDescription toJavaSessionDescription(JsonObject json) {
        log.info("sdp answer is :" + json.get("sdpAnswer").getAsString());
        return new RTCSessionDescription(
                RTCSdpType.ANSWER,
                json.get("sdpAnswer").getAsString());
    }

}
