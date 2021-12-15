package com.mistark.data.jpa.plugin;

public class InvalidParameter extends Throwable{
    public InvalidParameter() {
    }

    public InvalidParameter(String message) {
        super(message);
    }

    public InvalidParameter(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidParameter(Throwable cause) {
        super(cause);
    }
}
