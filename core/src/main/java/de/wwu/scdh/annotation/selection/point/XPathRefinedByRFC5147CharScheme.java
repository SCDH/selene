package de.wwu.scdh.annotation.selection.point;

import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.component.XPathComponent;
import de.wwu.scdh.annotation.selection.component.RFC5147CharComponent;


/**
 * A {@link Point} represented by an <code>oa:XPathSelector</code>
 * which is refined by an <code>oa:FragmentSelector</code> conforming
 * to RFC5147 character scheme. This record is usually used to
 * reference content in a {@link DOMResource}.
 */
public class XPathRefinedByRFC5147CharScheme extends PointImpl {

    private final String xpath;

    private final int chr;

    /**
     * Make a {@link XPathRefinedByRFC5147CharScheme} from an xpath
     * and a inter-character position.
     *
     * @param xpath  the XPath component
     * @param chr    the character scheme component
     */
    public XPathRefinedByRFC5147CharScheme(String xpath, int chr) {
	super(new XPathComponent(xpath), new RFC5147CharComponent(chr));
	this.xpath = xpath;
	this.chr = chr;
    }

    public String getXPath() {
	return xpath;
    }

    public int getChar() {
	return chr;
    }

}
