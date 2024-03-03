package com.flat.listener.rtc.apprtc;

import com.flat.listener.rtc.SignalingClient;
import com.flat.listener.rtc.SignalingListener;
import com.flat.listener.rtc.model.Contact;
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
public class FlatSignalingClient implements SignalingClient {

    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private SignalingListener listener;

    @Autowired
    private JsonObjectEncoder encoder;


    @Override
    public void joinRoom(Contact asContact) throws Exception {
        listener.onRoomJoined(asContact);
    }

    @Override
    public void setSignalingListener(SignalingListener listener) {
        this.listener = listener;
    }

    @Override
    public void sendSdpOffer(Contact contact, RTCSessionDescription sdp, String uuid) {
        String offer = encoder.toOfferJson(contact, sdp, uuid);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, offer);
    }

    @Override
    public void sendIceCandidate(final RTCIceCandidate candidate, String uuid) {
        String iceCandidate = encoder.toIceCandidateJson(candidate, uuid);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, iceCandidate);
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
