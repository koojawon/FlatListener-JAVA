package com.flat.listener.service;

import com.flat.listener.message.entity.FileRequestMessage;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class FileService {

    private final WebClient webClient = WebClient.builder().build();
    @Value("${uploadPath}")
    private String path;

    @Value("${serverIp}")
    private String ip;

    @PostConstruct
    public void init() {
        File file = new File(path);
        if (file.mkdirs()) {
            if (!(file.setExecutable(true) && file.setReadable(true) && file.setWritable(true))) {
                log.error("Directory Authority Set Failed!!");
            }
        } else {
            log.error("Directory creation failed!!");
        }
    }

    public void getFile(FileRequestMessage fileRequestMessage) {

        Flux<DataBuffer> dataBuffer = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host(ip)
                        .path("/files/pdf")
                        .queryParam("fileUid", fileRequestMessage.getFileUid()).build())
                .retrieve()
                .onStatus(
                        httpStatus ->
                                httpStatus != HttpStatus.OK,
                        clientResponse -> clientResponse.createException()
                                .flatMap(
                                        it -> Mono.error(new RuntimeException(
                                                "code : " + clientResponse.statusCode() + "PDF retrieve fail!!")))
                )
                .bodyToFlux(DataBuffer.class)
                .doOnComplete(() -> {
                    log.info("file downloaded!!");
                })
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage());
                    return Mono.error(new RuntimeException(throwable));
                });
        saveFile(dataBuffer, fileRequestMessage);
    }

    private void saveFile(Flux<DataBuffer> dataBuffer, FileRequestMessage fileRequestMessage) {
        log.info("saving file");
        log.info(Paths.get(path + fileRequestMessage.getFileUid() + ".pdf").toString());
        DataBufferUtils.write(dataBuffer, Paths.get(path + fileRequestMessage.getFileUid() + ".pdf"),
                        StandardOpenOption.CREATE)
                .share()
                .doOnSuccess(unused -> handlePdf(fileRequestMessage.getFileUid()))
                .doOnError(exception -> {
                    log.error(exception.getMessage());
                    Mono.error(new RuntimeException(exception));
                })
                .subscribe();
    }

    private void handlePdf(String fileName) {
        log.info("handling file :" + fileName);
        String filePath = path + fileName;
        try {
            if (new ProcessExecutor()
                    .command("python", "C:/Users/madab/pdf-to-mxl-converter/main.py", "-data", filePath)
                    .redirectOutput(Slf4jStream.ofCaller().asInfo())
                    .start()
                    .getFuture()
                    .get(30, TimeUnit.SECONDS)
                    .getExitValue() != 0) {
                throw new RuntimeException("Execution Failed!!");
            }
            postResult(fileName);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void postResult(String filename) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("multipartFile", new FileSystemResource(path + filename + ".mxl"));
        postFile(multipartBodyBuilder);
    }

    private void postFile(MultipartBodyBuilder multipartBodyBuilder) {
        log.info("posting file");
        webClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host(ip)
                        .path("/files/upload")
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .retrieve()
                .toBodilessEntity()
                .subscribe();
    }
}
