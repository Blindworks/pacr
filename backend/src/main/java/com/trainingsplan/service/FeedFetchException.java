package com.trainingsplan.service;

/**
 * Thrown by {@link RssFeedParser} when fetching or parsing an RSS/Atom feed fails.
 * Caught by the importer service so a single broken feed does not abort the whole run.
 */
public class FeedFetchException extends RuntimeException {
    public FeedFetchException(String message) { super(message); }
    public FeedFetchException(String message, Throwable cause) { super(message, cause); }
}
