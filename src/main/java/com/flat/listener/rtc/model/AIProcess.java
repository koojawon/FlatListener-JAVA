package com.flat.listener.rtc.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.SubmissionPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AIProcess {

    private final SubmissionPublisher<String> publisher;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    public AIProcess() {
        publisher = new SubmissionPublisher<>();
    }

    public void start(String fileName) {
        try {
            Process process = Runtime.getRuntime()
                    .exec("python3 ./flat/flat.py --score_midi_path ./flat/data/" + fileName
                            + ".mid --mode stream --backend timestamp --backend_output stdout");
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            startRead();
        } catch (IOException e) {
            log.error("child process start failed!! : {}", e.getMessage());
        }
    }

    public void writeData(byte[] data) {
        try {
            char[] buf = new char[data.length];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (char) data[i];
            }
            bufferedWriter.write(buf);
        } catch (IOException e) {
            log.error("Write error!! : {}", e.getMessage());
        }
    }

    private void startRead() {
        Thread thread = new Thread(
                () -> {
                    String s;
                    while (true) {
                        try {
                            if ((s = bufferedReader.readLine()) == null) {
                                break;
                            }
                            if (!s.isEmpty()) {
                                publisher.submit(s);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
        thread.start();
    }

    public void stopRead() {
        publisher.close();
    }

    public Publisher<String> asPublisher() {
        return publisher;
    }
}
