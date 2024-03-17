package com.flat.listener.rtc.apprtc;

import static java.util.Objects.nonNull;

import com.flat.listener.message.entity.FileRequestMessage;
import com.flat.listener.rtc.model.Contact;
import com.flat.listener.rtc.model.RabbitMessage;
import com.flat.listener.rtc.model.RabbitMessage.ID;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JsonObjectDecoder {

    public RabbitMessage toRabbitMessage(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        String response = null;
        if (jsonObject.has("response")) {
            response = jsonObject.get("response").getAsString();
        }
        if (nonNull(response) && response.equals("rejected")) {
            log.error(response + " : " + jsonObject.get("message").getAsString());
            return new RabbitMessage(ID.ERROR, jsonObject.get("message").getAsString());
        }
        String type = jsonObject.get("id").getAsString();
        return switch (type) {
            case "iceCandidate" -> new RabbitMessage(ID.iceCandidate, toIceCandidate(jsonObject));
            case "viewerResponse" -> new RabbitMessage(ID.viewerResponse, toSessionDescription(jsonObject));
            case "stopCommunication" -> new RabbitMessage(ID.stopCommunication);
            case "targetInfo" -> new RabbitMessage(ID.targetInfo, toContact(jsonObject));
            case "File" -> new RabbitMessage(ID.File, toFileRequestMessage(jsonObject));
            default -> new RabbitMessage(ID.ERROR, "Unexpected message: " + json);
        };
    }

    private Contact toContact(JsonObject jsonObject) {
        return new Contact(jsonObject.get("targetId").getAsString(), jsonObject.get("fileName").getAsString());
    }


    private FileRequestMessage toFileRequestMessage(JsonObject json) {
        return FileRequestMessage.builder().fileUid(json.get("fileUid").getAsString()).build();
    }

    private RTCIceCandidate toIceCandidate(JsonObject json) {
        JsonObject candidate = json.get("candidate").getAsJsonObject();
        return new RTCIceCandidate(
                candidate.get("sdpMid").getAsString(),
                candidate.get("sdpMLineIndex").getAsInt(),
                candidate.get("candidate").getAsString());
    }

    private RTCSessionDescription toSessionDescription(JsonObject json) {
        return new RTCSessionDescription(RTCSdpType.ANSWER, json.get("sdpAnswer").getAsString());
    }

}
