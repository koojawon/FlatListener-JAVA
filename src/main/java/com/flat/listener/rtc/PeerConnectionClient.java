package com.flat.listener.rtc;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.flat.listener.config.Configuration;
import com.flat.listener.rtc.model.Contact;
import com.flat.listener.rtc.model.PeerConnectionContext;
import com.google.gson.JsonObject;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelInit;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCPeerConnectionIceErrorEvent;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtpReceiver;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCRtpTransceiverDirection;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSource;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PeerConnectionClient implements PeerConnectionObserver {

    private final ExecutorService executor;
    private final Configuration config;
    private final Contact contact;
    private final SignalingClient signalingClient;
    private final PeerConnectionContext peerConnectionContext;
    private Timer statsTimer;
    private PeerConnectionFactory factory;
    private RTCPeerConnection peerConnection;
    private RTCDataChannel dataChannel;
    /*
     * Queued remote ICE candidates are consumed only after both local and
     * remote descriptions are set. Similarly local ICE candidates are sent to
     * remote peer after both local and remote description are set.
     */
    private List<RTCIceCandidate> queuedRemoteCandidates;


    public PeerConnectionClient(Contact contact, PeerConnectionContext context,
                                SignalingClient signalingClient,
                                ExecutorService executor) {
        this.config = new Configuration();
        this.contact = contact;
        this.peerConnectionContext = context;
        this.signalingClient = signalingClient;
        this.executor = executor;
        this.queuedRemoteCandidates = new ArrayList<>();

//        RTCIceServer rtcIceServer = new RTCIceServer();
//        rtcIceServer.urls.add("localhost:3478");
//        rtcIceServer.urls.add("localhost:5349");
//        config.getRtcConfiguration().iceServers.add(rtcIceServer);

        executeAndWait(() -> {
            factory = new PeerConnectionFactory();
            peerConnection = factory.createPeerConnection(config.getRtcConfiguration(), this);
        });
    }

    public void enableStatsEvents(boolean enable, int periodMs) {
        if (enable) {
            try {
                statsTimer = new Timer();
                statsTimer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        getStats();
                        log.info("Connection state: " + peerConnection.getConnectionState().toString());
                        log.info("Ice Connection state: " + peerConnection.getIceConnectionState().toString());
                        log.info("Ice Gathering state: " + peerConnection.getIceGatheringState().toString());
                    }
                }, 0, periodMs);
            } catch (Exception e) {
                log.error("Can not schedule statistics timer", e);
            }
        } else {
            statsTimer.cancel();
        }
    }


    public boolean hasRemoteAudioStream() {
        RTCRtpReceiver[] receivers = peerConnection.getReceivers();
        if (nonNull(receivers)) {
            for (RTCRtpReceiver receiver : receivers) {
                log.info(receiver.getTrack().getId() + ":" + receiver.getTrack().getKind());
            }
        }
        return false;
    }

    @Override
    public void onRenegotiationNeeded() {
        if (nonNull(peerConnection.getRemoteDescription())) {
            log.info("Renegotiation needed");
            createOffer();
        }
    }

    @Override
    public void onIceCandidateError(RTCPeerConnectionIceErrorEvent event) {
        PeerConnectionObserver.super.onIceCandidateError(event);
        log.error(event.getErrorText());
    }

    @Override
    public void onIceConnectionChange(RTCIceConnectionState state) {
        PeerConnectionObserver.super.onIceConnectionChange(state);
        log.info(state.toString());
    }


    @Override
    public void onConnectionChange(RTCPeerConnectionState state) {
        notify(peerConnectionContext.onPeerConnectionState, state);
    }


    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {
        if (isNull(peerConnection)) {
            log.error("PeerConnection was not initialized");
            return;
        }

        try {
            signalingClient.send(contact, candidate, peerConnectionContext.uniqueId);
        } catch (Exception e) {
            log.error("Send RTCIceCandidate failed", e);
        }
    }

    @Override
    public void onIceCandidatesRemoved(RTCIceCandidate[] candidates) {
        if (isNull(peerConnection)) {
            log.error("PeerConnection was not initialized");
            return;
        }

        try {
            signalingClient.send(contact, candidates, peerConnectionContext.uniqueId);
        } catch (Exception e) {
            log.error("Send removed RTCIceCandidates failed", e);
        }
    }

    @Override
    public void onTrack(RTCRtpTransceiver transceiver) {
        MediaStreamTrack track = transceiver.getReceiver().getTrack();
        log.info("found track");
        if (track.getKind().equals(MediaStreamTrack.AUDIO_TRACK_KIND)) {
            AudioTrack audioTrack = (AudioTrack) track;
            audioTrack.addSink((bytes, bitsPerSample, sampleRate, numberOfChannels, numberOfFrames) -> {
            });

            notify(peerConnectionContext.onRemoteVideoStream, true);
        }
    }

    @Override
    public void onRemoveTrack(RTCRtpReceiver receiver) {
        MediaStreamTrack track = receiver.getTrack();

        if (track.getKind().equals(MediaStreamTrack.AUDIO_TRACK_KIND)) {
            notify(peerConnectionContext.onRemoteVideoStream, false);
        }
    }


    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            if (nonNull(statsTimer)) {
                statsTimer.cancel();
            }
            if (nonNull(dataChannel)) {
                dataChannel.unregisterObserver();
                dataChannel.close();
                dataChannel.dispose();
                dataChannel = null;
            }
            if (nonNull(peerConnection)) {
                peerConnection.close();
                peerConnection = null;
            }
            if (nonNull(factory)) {
                factory.dispose();
                factory = null;
            }
        }, executor);
    }

    public void initCall() {
        execute(() -> {
            initMedia();
            addDataChannel();
            createOffer();
        });
    }

    public CompletableFuture<Void> sendMessage(String message) {
        return CompletableFuture.runAsync(() -> {
            if (isNull(dataChannel)) {
                throw new CompletionException("RTCDataChannel was not initialized or negotiated", null);
            }

            try {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("stamp", message);
                String encoded = jsonObject.toString();

                ByteBuffer data = ByteBuffer.wrap(encoded.getBytes(StandardCharsets.UTF_8));
                RTCDataChannelBuffer buffer = new RTCDataChannelBuffer(data, false);

                dataChannel.send(buffer);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private void initMedia() {
        addAudio(peerConnectionContext.audioDirection);
    }

    private void addAudio(RTCRtpTransceiverDirection direction) {
        if (direction == RTCRtpTransceiverDirection.INACTIVE) {
            return;
        }
        AudioOptions audioOptions = new AudioOptions();

        if (direction != RTCRtpTransceiverDirection.SEND_ONLY) {
            audioOptions.echoCancellation = true;
            audioOptions.noiseSuppression = true;
        }

        AudioTrackSource audioSource = factory.createAudioSource(audioOptions);
        AudioTrack audioTrack = factory.createAudioTrack("audioTrack", audioSource);

        peerConnection.addTrack(audioTrack, List.of("stream"));

        for (RTCRtpTransceiver transceiver : peerConnection.getTransceivers()) {
            MediaStreamTrack track = transceiver.getSender().getTrack();

            if (nonNull(track) && track.getKind().equals(MediaStreamTrack.AUDIO_TRACK_KIND)) {
                transceiver.setDirection(direction);
                ((AudioTrack) track).addSink(
                        (bytes, bitsPerSample, sampleRate, numberOfChannels, numberOfFrames) -> {
                        }); //log.info("data : " + Arrays.toString(bytes)));
                break;
            }
        }

        RTCRtpReceiver receiver = peerConnection.getReceivers()[0];
        //((AudioTrack) receiver.getTrack()).addSink(                (bytes, bitsPerSample, sampleRate, numberOfChannels, numberOfFrames) -> log.info(                        "data : " + Arrays.toString(bytes)));
    }

    private void addDataChannel() {
        RTCDataChannelInit dict = new RTCDataChannelInit();
        dict.protocol = "demo-messaging";

        dataChannel = peerConnection.createDataChannel("data", dict);
    }

    private void initDataChannel(final RTCDataChannel channel) {
        channel.registerObserver(new RTCDataChannelObserver() {

            @Override
            public void onBufferedAmountChange(long previousAmount) {
                execute(() -> {
                    log.info("RTCDataChannel \"{}\" buffered amount changed to {}",
                            channel.getLabel(),
                            previousAmount);
                });
            }

            @Override
            public void onStateChange() {
                execute(() -> {
                    log.info("RTCDataChannel \"{}\" state: {}",
                            channel.getLabel(),
                            channel.getState());
                });
            }

            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                //does nothing!! so don't send any message to me.
            }
        });
    }

    private void getStats() {
        execute(() -> {
            peerConnection.getStats(report -> {
                if (nonNull(peerConnectionContext.onStatsReport)) {
                    peerConnectionContext.onStatsReport.accept(report);
                    log.info(report.getStats().toString());
                }
            });
        });
    }

    public void setSessionDescription(RTCSessionDescription description) {
        execute(() -> {
            peerConnection.setRemoteDescription(description, new SetSDObserver() {
                @Override
                public void onSuccess() {
                    super.onSuccess();
                }
            });
        });
    }

    public void addIceCandidate(RTCIceCandidate candidate) {
        execute(() -> {
            if (nonNull(queuedRemoteCandidates)) {
                queuedRemoteCandidates.add(candidate);
            } else {
                peerConnection.addIceCandidate(candidate);
            }
        });
    }

    public void removeIceCandidates(RTCIceCandidate[] candidates) {
        execute(() -> {
            drainIceCandidates();

            peerConnection.removeIceCandidates(candidates);
        });
    }

    private void drainIceCandidates() {
        if (nonNull(queuedRemoteCandidates)) {
            log.info("Add " + queuedRemoteCandidates.size() + " remote candidates");

            queuedRemoteCandidates.forEach(peerConnection::addIceCandidate);
            queuedRemoteCandidates = null;
        }
    }

    private void createOffer() {
        RTCOfferOptions options = new RTCOfferOptions();
        peerConnection.createOffer(options, new CreateSDObserver());
    }

    private void setLocalDescription(RTCSessionDescription description) {
        peerConnection.setLocalDescription(description, new SetSDObserver() {
            @Override
            public void onSuccess() {
                //super.onSuccess();
                try {
                    signalingClient.send(contact, description, peerConnectionContext.uniqueId);
                } catch (Exception e) {
                    log.error("Send RTCSessionDescription failed", e);
                }
            }

        });
    }


    private <T> void notify(BiConsumer<Contact, T> consumer, T value) {
        if (nonNull(consumer)) {
            execute(() -> consumer.accept(contact, value));
        }
    }

    private void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    private void executeAndWait(Runnable runnable) {
        try {
            executor.submit(runnable).get();
        } catch (Exception e) {
            log.error("Execute task failed");
        }
    }


    private class CreateSDObserver implements CreateSessionDescriptionObserver {

        @Override
        public void onSuccess(RTCSessionDescription description) {
            execute(() -> setLocalDescription(description));
        }

        @Override
        public void onFailure(String error) {
            execute(() -> {
                log.error("Create RTCSessionDescription failed: " + error);
            });
        }

    }


    private class SetSDObserver implements SetSessionDescriptionObserver {

        @Override
        public void onSuccess() {
            execute(() -> {
                if (nonNull(peerConnection.getLocalDescription()) &&
                        nonNull(peerConnection.getRemoteDescription())) {
                    drainIceCandidates();
                }
            });
        }

        @Override
        public void onFailure(String error) {
            execute(() -> {
                log.error("Set RTCSessionDescription failed: " + error);
            });
        }
    }
}
