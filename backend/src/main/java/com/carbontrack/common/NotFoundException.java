package com.carbontrack.common;

/** Also used for another company's data, so existence is never leaked. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
