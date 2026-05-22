package com.aiblogstudio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to modify a blog they don't own.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class BlogAccessDeniedException extends RuntimeException {

    public BlogAccessDeniedException(String message) {
        super(message);
    }

    public BlogAccessDeniedException() {
        super("You do not have permission to modify this blog");
    }
}
