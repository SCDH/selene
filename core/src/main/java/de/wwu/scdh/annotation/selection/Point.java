package de.wwu.scdh.annotation.selection;

/**
 * A point is a triple of web resource, path to select a markup
 * language fragment from it and position confirming to RFC5147
 * char-scheme to point inside that fragment.
 */
public interface Point {

    /**
     * Returns the web resource.
     */
    Resource getResource();

    /**
     * Returns the path selecting a fragment.
     */
    String getPath();

    /**
     * Returns the position inside the fragment.
     */
    int getPosition();

}
