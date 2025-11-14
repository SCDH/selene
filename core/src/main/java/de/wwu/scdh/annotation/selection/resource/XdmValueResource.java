package de.wwu.scdh.annotation.selection.resource;

import java.net.URI;

import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Processor;

/**
 * The {@link XdmValueResource} is suitable for all kinds of resources
 * derived from a {@link DOMResource}, even for plain text images.
 */
public class XdmValueResource implements S9ApiResource<XdmValue> {

    private final URI uri;
    private final XdmValue value;
    private final Processor processor;

    @Deprecated
    public XdmValueResource(URI uri, XdmValue value) {
	this.uri = uri;
	this.value = value;
	this.processor = null;
    }

    /**
     * Makes a new {@link XdmValueResource}.
     */
    public XdmValueResource(URI uri, XdmValue value, Processor processor) {
	this.uri = uri;
	this.value = value;
	this.processor = processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XdmValue getContents() {
	return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getUri() {
	return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Processor getProcessor() {
	return processor;
    }

}
