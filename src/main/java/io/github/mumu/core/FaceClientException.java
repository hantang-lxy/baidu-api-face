package io.github.mumu.core;

/**
 * @author mumu
 * @date 2020/2/8
 */
public class FaceClientException extends RuntimeException {

    public FaceClientException(String message) {
        super(message);
    }

    public FaceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public FaceClientException(Throwable cause) {
        super(cause);
    }
}
