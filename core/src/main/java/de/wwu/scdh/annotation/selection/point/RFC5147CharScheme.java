package de.wwu.scdh.annotation.selection.point;

import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.component.RFC5147CharComponent;


/**
 * A {@link Point} represented by a RFC5147 character scheme. This
 * record is usually used to reference content in a plain text
 * representation.
 */
public class RFC5147CharScheme extends PointImpl {

    private final int chr;

    /**
     * Make a {@link RFC5147CharScheme} from an xpath
     * and a inter-character position.
     *
     * @param xpath  the XPath component
     * @param chr    the character scheme component
     */
    public RFC5147CharScheme(int chr) {
	super(new RFC5147CharComponent(chr));
	this.chr = chr;
    }

    public int getChar() {
	return chr;
    }

}
