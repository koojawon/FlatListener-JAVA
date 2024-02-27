package com.flat.listener.rtc.apprtc;

import com.flat.listener.rtc.SignalingClient;
import com.flat.listener.rtc.SignalingListener;
import com.flat.listener.rtc.model.Contact;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppRTCSignalingClient implements SignalingClient {

    private final Gson gson = new Gson();
    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private SignalingListener listener;


    @Override
    public void joinRoom(Contact asContact) throws Exception {
        listener.onRoomJoined(asContact);
    }

    @Override
    public void send(Contact contact, Object obj, String uuid) {
        if (obj instanceof RTCSessionDescription desc) {
            sendOfferSdp(contact, desc, uuid);
        } else if (obj instanceof RTCIceCandidate) {
            sendIceCandidate((RTCIceCandidate) obj, uuid);
        }
    }

    @Override
    public void setSignalingListener(SignalingListener listener) {
        this.listener = listener;
    }

    private void sendOfferSdp(Contact contact, RTCSessionDescription sdp, String uuid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "viewer");
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("targetId", contact.getId());
        jsonObject.addProperty("sdpOffer", sdp.sdp);

        rabbitTemplate.convertAndSend(exchangeName, routingKey, gson.toJson(jsonObject));
    }

    private void sendIceCandidate(final RTCIceCandidate candidate, String uuid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "iceCandidate");
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("candidate", candidate.sdp);
        jsonObject.addProperty("sdpMid", candidate.sdpMid);
        jsonObject.addProperty("sdpMLineIndex", candidate.sdpMLineIndex);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, gson.toJson(jsonObject));
    }

    public void addIceCandidate(Contact contact, RabbitMessage sigMessage) {
        RTCIceCandidate candidate = (RTCIceCandidate) sigMessage.getObject();
        listener.onRemoteIceCandidate(contact, candidate);
    }

    public void handleSdpAnswer(Contact contact, RabbitMessage sigMessage) {
        RTCSessionDescription answer = (RTCSessionDescription) sigMessage.getObject();
        listener.onRemoteSessionDescription(contact, answer);
    }

    public void stopCommunication() {
        listener.onRoomLeft();
    }
}
