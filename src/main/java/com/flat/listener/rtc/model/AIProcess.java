package com.flat.listener.rtc.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    private OutputStream e;

    public AIProcess() {
        publisher = new SubmissionPublisher<>();
    }

    public void start(String fileName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python",
                    "C:/Users/jawon/Downloads/flat_test/argparse.py");
            process = processBuilder.start();
            if (process.isAlive()) {
                log.info("successfully started, state : {}", process.isAlive());
                e = process.getOutputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                startRead();
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("child process start failed!! : {}", e.getMessage());
        }
    }

    public void writeData(byte[] data) {
        try {
            e.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRead() {
        thread = new Thread(
                () -> {
                    String s;
                    while (true) {
                        try {
                            if ((s = bufferedReader.readLine()) == null) {
                                break;
                            }
                            publisher.submit(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
        thread.start();
    }

    public void stopRead() {
        process.destroy();
        thread.interrupt();
        publisher.close();
    }

    public Publisher<String> asPublisher() {
        return publisher;
    }
}
