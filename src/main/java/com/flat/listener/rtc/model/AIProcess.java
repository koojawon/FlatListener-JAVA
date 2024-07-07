package com.flat.listener.rtc.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.SubmissionPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AIProcess {

    private final SubmissionPublisher<String> publisher;
    public Thread thread;
    private BufferedReader bufferedReader;
    private Process process;

    public AIProcess() {
        publisher = new SubmissionPublisher<>();
    }

    public void start(String fileName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python3",
                    "//wsl.localhost/Ubuntu-20.04/home/blue/flat/flat.py", "--score_midi_path",
                    "//wsl.localhost/Ubuntu-20.04/home/blue/flat/data/25-12/25-12.mid", "--mode", "stream", "--backend",
                    "timestamp", "--backend_output", "stdout");
            processBuilder.redirectError(Redirect.INHERIT);
            process = processBuilder.start();
            if (process.isAlive()) {
                log.info("successfully started, state : {}", process.isAlive());
                startRead();
            }
        } catch (Exception e) {
            log.error("child process start failed!! : {}", e.getMessage());
        }
    }

    public synchronized void writeData(byte[] data) {
        try {
            OutputStream e = process.getOutputStream();
            e.write(data);
            e.flush();
        } catch (IOException ex) {
            log.error("Failed to write data: {}", ex.getMessage(), ex);
        }
    }

    private void startRead() {
        thread = new Thread(
                () -> {
                    String s;
                    bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    while (true) {
                        try {
                            s = bufferedReader.readLine();
                            if (s != null) {
                                this.publisher.submit(s);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
        thread.start();
        log.info(String.valueOf(thread.isAlive()));
    }

    public void stopRead() {

        process.destroy();
        publisher.close();
    }

    public Publisher<String> asPublisher() {
        return publisher;
    }
}
