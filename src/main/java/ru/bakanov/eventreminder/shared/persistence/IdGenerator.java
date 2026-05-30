package ru.bakanov.eventreminder.shared.persistence;

import io.hypersistence.tsid.TSID;

public final class IdGenerator {
    private IdGenerator() {}

    public static String generateString() {
        return TSID.Factory.getTsid().toString();
    }
}
