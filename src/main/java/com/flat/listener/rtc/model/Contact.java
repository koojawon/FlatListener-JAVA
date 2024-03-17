package com.flat.listener.rtc.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;


@Data
@RequiredArgsConstructor
public class Contact {
    private final String id;
    private final String fileName;

    public Contact() {
        this.fileName = null;
        id = null;
    }
}
