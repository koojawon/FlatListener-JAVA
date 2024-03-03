package com.flat.listener.message.entity;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class FileRequestMessage {
    private final String fileUid;
}
