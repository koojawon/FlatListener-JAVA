package com.flat.listener.rtc;

import com.flat.listener.rtc.model.Contact;

public interface SignalingClient {

    void joinRoom(Contact asContact) throws Exception;

    void send(Contact contact, Object obj, String uuid) throws Exception;

    void setSignalingListener(SignalingListener listener);

}
