package com.backend.searchengine.Exception;

/**
 * Exception class for the case when a resource is not found
 */
public class NotFoundException extends RuntimeException
{

    public NotFoundException(String resourceId) {
        super("Resource with given id: %s not found" + resourceId);
    }
}
