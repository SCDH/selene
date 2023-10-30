package de.wwu.scdh.annotation.selection;


/**
 * A range is a triple of a web {@link Resource} and a start {@link
 * Point} and an end {@link Point} in it.
 */
public interface Range {

    /**
     * Returns the start {@link Point}.
     */
    Point getStart();

    /**
     * Returns the end {@link Point}.
     */
    Point getEnd();

}
