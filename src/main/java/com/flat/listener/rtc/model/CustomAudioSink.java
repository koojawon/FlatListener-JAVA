package com.flat.listener.rtc.model;

import dev.onvoid.webrtc.media.audio.AudioSink;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomAudioSink implements AudioSink {

    private final AIProcess aiProcess;

    @Override
    public void onRecordedData(byte[] audioSamples, int nSamples, int nBytesPerSample, int nChannels, int samplesPerSec,
                               int totalDelayMS, int clockDrift) {
        aiProcess.writeData(audioSamples);
    }
}
