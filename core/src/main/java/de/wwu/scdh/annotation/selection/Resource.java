package de.wwu.scdh.annotation.selection;

import java.net.URI;

// import net.sf.saxon.s9api.XdmNode;


/**
 * A {@link Resource} is a Java representation of a web resource
 * identified by a URI. It may be a projection of a preimage. And it
 * it might be parsed into a DOM.
 */
public interface Resource {

    /**
     * Returns the preimage.
     */
    Resource getPreImage();

    /**
     * Returns the {@link URI} identifying this resource.
     */
    URI getUri();

    // /**
    //  * Returns the parsed DOM.
    //  */
    // XdmNode getDom();

}
