package de.wwu.scdh.annotation.selection.selector;

import java.net.URI;
import java.net.URISyntaxException;

import de.wwu.scdh.annotation.selection.Selector;
import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.Range;
import de.wwu.scdh.annotation.selection.Resource;
import de.wwu.scdh.annotation.selection.IsomorphicallyNormalizable;
import de.wwu.scdh.annotation.selection.SelectorException;


/**
 * The {@link RangeSelector<S1,S2>} class is for representing
 * RangeSelectors from the Web Annotation Data Model.
 *
 */
public class RangeSelector<S1 extends Selector & Point & IsomorphicallyNormalizable<S1>, S2 extends Selector & Point & IsomorphicallyNormalizable<S2>>
    implements Selector, Range, IsomorphicallyNormalizable<RangeSelector<S1, S2>> {

    protected final S1 start;

    protected final S2 end;

    protected final Resource resource;

    protected boolean hasNormalizedForm = false;

    protected boolean isNormalizedChecked = false;

    protected RangeSelector<S1, S2> normalizedRange = null;

    public RangeSelector(S1 start, S2 end, Resource resource) {
	this.resource = resource;
	this.start = start;
	this.end = end;
    }

    @Override
    public Resource getResource() {
	return resource;
    }

    @Override
    public S1 getStart() {
	return start;
    }

    @Override
    public S2 getEnd() {
	return end;
    }

    @Override
    public RangeSelector<S1, S2> normalize() throws SelectorException {
	S1 normalizedStart = this.start.normalize();
	S2 normalizedEnd = this.end.normalize();
	this.normalizedRange = new RangeSelector<S1, S2>
	    (normalizedStart, normalizedEnd, this.resource);
	return this.normalizedRange;
    }

    @Override
    public boolean isNormalized() throws SelectorException {
	if (this.isNormalizedChecked == true) {
	    return this.hasNormalizedForm;
	} else {
	    RangeSelector<S1, S2> normalized = this.normalize();
	    this.hasNormalizedForm =
		normalized.start.getPath() == this.start.getPath() &&
		normalized.start.getPosition() == this.start.getPosition() &&
		normalized.end.getPath() == this.end.getPath() &&
		normalized.end.getPosition() == this.end.getPosition();
	    return this.hasNormalizedForm;
	}
    }

    @Override
    public URI toUri() throws SelectorException {
	try {
	    URI startUri = this.normalizedRange.getStart().toUri();
	    URI endUri = this.normalizedRange.getEnd().toUri();
	    String fragment = "range(" + startUri + "," + endUri + ")";
	    return new URI("", "", "", "", fragment);
	} catch (URISyntaxException e) {
	    throw new SelectorException(e);
	}
    }

}
