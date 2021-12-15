package com.mistark.data.jpa.plugin;

public class NotSupportedOperator extends Throwable{
    public NotSupportedOperator() {
    }

    public NotSupportedOperator(String message) {
        super(message);
    }

    public NotSupportedOperator(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportedOperator(Throwable cause) {
        super(cause);
    }
}
