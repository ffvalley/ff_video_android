package com.ffvalley.demo.video;

public class VlcVideoException extends RuntimeException {

    public VlcVideoException() {
        super();
    }

    public VlcVideoException(String message) {
        super(message);
    }

    public VlcVideoException(String message, Throwable cause) {
        super(message, cause);
    }

    public VlcVideoException(Throwable cause) {
        super(cause);
    }
}
