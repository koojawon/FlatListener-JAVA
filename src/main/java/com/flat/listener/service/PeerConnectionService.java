package com.flat.listener.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.flat.listener.rtc.SignalingClient;
import com.flat.listener.rtc.SignalingListener;
import com.flat.listener.rtc.apprtc.PeerConnectionClient;
import com.flat.listener.rtc.model.Contact;
import com.flat.listener.rtc.model.PeerConnectionConfiguration;
import com.flat.listener.rtc.model.PeerConnectionContext;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCRtpTransceiverDirection;
import dev.onvoid.webrtc.RTCSessionDescription;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PeerConnectionService implements SignalingListener {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final SignalingClient signalingClient;

    private final PeerConnectionContext peerConnectionContext;

    private final Map<Contact, PeerConnectionClient> connections;

    private Contact activeContact;


    @Autowired
    PeerConnectionService(PeerConnectionConfiguration config, SignalingClient client) {
        signalingClient = client;
        signalingClient.setSignalingListener(this);
        connections = new HashMap<>();
        peerConnectionContext = new PeerConnectionContext();
        peerConnectionContext.audioDirection = RTCRtpTransceiverDirection.RECV_ONLY;
    }

    @Override
    public void onRoomJoined(Contact contact) {
        activeContact = contact;
        CompletableFuture.runAsync(() -> {
            createPeerConnection(activeContact).initCall();
            enableStats(false);
        }).join();
    }

    @Override
    public void onRoomLeft() {
        hangup();
    }

    @Override
    public void onRemoteSessionDescription(Contact contact, RTCSessionDescription description) {
        CompletableFuture.runAsync(() -> {
            getPeerConnectionClient(contact).setSessionDescription(description);
        });
    }

    @Override
    public void onRemoteIceCandidate(Contact contact, RTCIceCandidate candidate) {
        CompletableFuture.runAsync(() -> {
            getPeerConnectionClient(contact).addIceCandidate(candidate);
        });
    }


    @Override
    public void onError(String message) {
        log.error(message);
    }

    public void enableStats(boolean active) {
        if (active) {
            var peerConnectionClient = getPeerConnectionClient(activeContact);
            if (nonNull(peerConnectionClient)) {
                peerConnectionClient.enableStatsEvents(5000);
            }
        }
    }

    public void hangup() {
        var peerConnectionClient = getPeerConnectionClient(activeContact);

        if (isNull(peerConnectionClient)) {
            CompletableFuture.completedFuture(null);
            return;
        }
        CompletableFuture<Void> close = peerConnectionClient.close();
        close.thenRun(() -> activeContact = null);
        close.join();
        removePeerConnectionClient(activeContact);
    }

    private CompletableFuture<Void> closeConnections() {
        CompletableFuture<?>[] list = new CompletableFuture[connections.values().size()];
        int index = 0;
        for (PeerConnectionClient client : connections.values()) {
            list[index++] = client.close();
        }
        return CompletableFuture.allOf(list);
    }

    private PeerConnectionClient getPeerConnectionClient(Contact contact) {
        return connections.get(contact);
    }

    private void removePeerConnectionClient(Contact contact) {
        connections.remove(contact);
    }

    private PeerConnectionClient createPeerConnection(Contact contact) {
        var peerConnectionClient = getPeerConnectionClient(contact);

        if (nonNull(peerConnectionClient)) {
            return peerConnectionClient;
        }

        peerConnectionClient = new PeerConnectionClient(contact, peerConnectionContext, signalingClient, executor);
        connections.put(contact, peerConnectionClient);
        return peerConnectionClient;
    }
}
