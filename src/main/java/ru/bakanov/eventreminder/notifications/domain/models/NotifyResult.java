package ru.bakanov.eventreminder.notifications.domain.models;

public record NotifyResult(boolean success, String errorMessage) {

    public static NotifyResult ok() {
        return new NotifyResult(true, null);
    }

    public static NotifyResult failed(String error) {
        return new NotifyResult(false, error);
    }
}
