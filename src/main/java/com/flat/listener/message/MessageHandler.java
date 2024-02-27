package com.flat.listener.message;

import com.flat.listener.message.entity.RequestMessage;
import com.flat.listener.rtc.apprtc.AppRTCJsonCodec;
import com.flat.listener.rtc.apprtc.AppRTCSignalingClient;
import com.flat.listener.rtc.apprtc.RabbitMessage;
import com.flat.listener.rtc.model.Contact;
import com.flat.listener.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor

public class MessageHandler {

    private final FileService fileService;

    @Autowired
    private final AppRTCSignalingClient appRTCSignalingClient;

    private final AppRTCJsonCodec codec;

    private Contact contact = new Contact();

    @RabbitListener(queues = "pdfJobQueue.listenerQueue")
    public void handleMessage(String message) throws Exception {
        log.info(message);
        RabbitMessage sigMessage = codec.toJavaMessage(message);
        switch (sigMessage.getId()) {
            case targetInfo:
                appRTCSignalingClient.joinRoom((Contact) sigMessage.getObject());
                contact = (Contact) sigMessage.getObject();
                break;
            case File:
                fileService.getFile((RequestMessage) sigMessage.getObject());
                break;
            case iceCandidate:
                appRTCSignalingClient.addIceCandidate(contact, sigMessage);
                break;
            case viewerResponse:
                appRTCSignalingClient.handleSdpAnswer(contact, sigMessage);
                break;
            case stopCommunication:
                appRTCSignalingClient.stopCommunication();
                break;
        }
    }

}
