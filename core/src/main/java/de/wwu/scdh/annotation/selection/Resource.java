package de.wwu.scdh.annotation.selection;

import java.net.URI;


/**
 * A {@link Resource} is a Java representation of a web resource
 * identified by a URI. A {@link Resource} is parametrized with the
 * parsed content type, returned by the {@link Resource.getContent}
 * method.
 */
public interface Resource<T> {

    /**
     * Returns the {@link URI} identifying this resource.
     */
    URI getUri();

    /**
     * Returns the contents of the resource as parsed type.
     */
    T getContents();

}
