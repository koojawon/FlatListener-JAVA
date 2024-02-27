package com.flat.listener.rtc.apprtc;

import lombok.Getter;

public class RabbitMessage {
    @Getter
    private final ID id;
    @Getter
    private final Object object;

    RabbitMessage(ID id) {
        this(id, null);
    }


    RabbitMessage(ID id, Object object) {
        this.id = id;
        this.object = object;
    }

    @Override
    public String toString() {
        return id.toString() + object.toString();
    }


    public enum ID {
        targetInfo,
        iceCandidate,
        viewerResponse,
        stopCommunication,
        ERROR,
        File
    }
}
