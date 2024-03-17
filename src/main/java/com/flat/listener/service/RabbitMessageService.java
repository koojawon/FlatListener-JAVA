package com.flat.listener.service;

import com.flat.listener.message.entity.FileRequestMessage;
import com.flat.listener.rtc.apprtc.FlatSignalingClient;
import com.flat.listener.rtc.apprtc.JsonObjectDecoder;
import com.flat.listener.rtc.model.Contact;
import com.flat.listener.rtc.model.RabbitMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitMessageService {

    private final FileService fileService;

    @Autowired
    private final FlatSignalingClient flatSignalingClient;

    private final JsonObjectDecoder codec;

    private Contact contact = new Contact();

    @RabbitListener(queues = "pdfJobQueue.listenerQueue")
    public void handleMessage(String message) throws Exception {
        log.info(message);
        RabbitMessage sigMessage = codec.toRabbitMessage(message);
        switch (sigMessage.getId()) {
            case targetInfo:
                flatSignalingClient.joinRoom((Contact) sigMessage.getObject());
                contact = (Contact) sigMessage.getObject();
                break;
            case File:
                fileService.getFile((FileRequestMessage) sigMessage.getObject());
                break;
            case iceCandidate:
                flatSignalingClient.addIceCandidate(contact, sigMessage);
                break;
            case viewerResponse:
                flatSignalingClient.handleSdpAnswer(contact, sigMessage);
                break;
            case ERROR:
            case stopCommunication:
                flatSignalingClient.stopCommunication();
                break;
        }
    }

}
