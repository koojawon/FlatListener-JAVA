package com.flat.listener.rtc.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;


@Data
@RequiredArgsConstructor
public class Contact {
    private final String id;

    public Contact() {
        id = null;
    }
}
