package com.ffvalley.demo.exception;

public class VideoException extends RuntimeException {

    public VideoException() {
        super();
    }

    public VideoException(String message) {
        super(message);
    }

    public VideoException(String message, Throwable cause) {
        super(message, cause);
    }

    public VideoException(Throwable cause) {
        super(cause);
    }
}
