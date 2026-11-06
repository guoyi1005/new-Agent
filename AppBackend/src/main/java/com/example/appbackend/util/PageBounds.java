package com.example.appbackend.util;

/** Shared pagination bounds to prevent accidental full-table or oversized queries. */
public final class PageBounds {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private PageBounds() {
    }

    public static int page(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int size(Integer size, int defaultSize) {
        int safeDefault = Math.max(1, Math.min(defaultSize, MAX_SIZE));
        if (size == null || size < 1) return safeDefault;
        return Math.min(size, MAX_SIZE);
    }
}
