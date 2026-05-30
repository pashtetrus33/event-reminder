package ru.bakanov.eventreminder.shared.exception;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
