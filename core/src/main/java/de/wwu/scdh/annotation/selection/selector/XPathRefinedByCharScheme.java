package de.wwu.scdh.annotation.selection.selector;

import java.net.URI;
import java.net.URISyntaxException;

import de.wwu.scdh.annotation.selection.Selector;
import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.Resource;
import de.wwu.scdh.annotation.selection.IsomorphicallyNormalizable;
import de.wwu.scdh.annotation.selection.SelectorException;

/**
 * The class {@link XPathRefinedByCharScheme} represents an XPath
 * selector from the Web Annotation Data Model which is refined by a
 * character scheme defined in <a
 * href="https://www.rfc-editor.org/rfc/rfc5147">RFC 5147</a>.<P>
 */
public class XPathRefinedByCharScheme implements Selector, Point, IsomorphicallyNormalizable<XPathRefinedByCharScheme> {

    protected final String xpath;

    protected final int position;

    protected final Resource resource;

    protected boolean hasNormalizedForm = false;

    protected boolean isNormalizedChecked = false;

    public XPathRefinedByCharScheme(String xpath, int position, Resource resource) {
	this.xpath = xpath;
	this.position = position;
	this.resource = resource;
    }

    @Override
    public Resource getResource() {
	return resource;
    }

    @Override
    public String getPath() {
	return xpath;
    }

    @Override
    public int getPosition() {
	return position;
    }

    @Override
    public XPathRefinedByCharScheme normalize() throws SelectorException {
	return this; // TODO
    }

    @Override
    public boolean isNormalized() throws SelectorException {
	if (this.isNormalizedChecked == true) {
	    return this.hasNormalizedForm;
	} else {
	    XPathRefinedByCharScheme normalized = this.normalize();
	    this.hasNormalizedForm =
		normalized.getPath() == this.getPath() && normalized.getPosition() == this.getPosition();
	    return this.hasNormalizedForm;
	}
    }

    @Override
    public URI toUri() throws SelectorException {
	try {
	    return new URI("", "", "", "", "TODO"); // TODO
	} catch (URISyntaxException e) {
	    throw new SelectorException(e);
	}
    }

}
