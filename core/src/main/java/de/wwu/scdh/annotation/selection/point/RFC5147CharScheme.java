package de.wwu.scdh.annotation.selection.point;

import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.component.RFC5147CharComponent;

/**
 * A {@link Point} represented by a RFC5147 character scheme. This
 * record is usually used to reference content in a plain text
 * representation.
 */
public class RFC5147CharScheme extends PointImpl {

	public static final String RFC5147 = "http://tools.ietf.org/rfc/rfc5147";

	private final int chr;

	/**
	 * Make a {@link RFC5147CharScheme} inter-character position.
	 *
	 * @param chr - the character scheme component
	 */
	public RFC5147CharScheme(int chr) {
		super(new RFC5147CharComponent(chr));
		this.chr = chr;
	}

	public int getChar() {
		return chr;
	}
}
