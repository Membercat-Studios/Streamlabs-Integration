package me.Domplanto.streamLabs.exception;

public class UnexpectedJsonFormatException extends RuntimeException {
    public UnexpectedJsonFormatException() {
        super("The Streamlabs API returned an unexpected response");
    }
}
