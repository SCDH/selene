package de.wwu.scdh.annotation.selection.resource;

import java.net.URI;

import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Processor;

import de.wwu.scdh.annotation.selection.Point;

/**
 * The {@link XdmValueResource} is suitable for all kinds of resources
 * derived from a {@link DOMResource}, even for plain text images.
 */
public class XdmValueResource implements S9ApiResource<XdmValue> {

    private final URI uri;
    private final XdmValue value;
    private final Processor processor;
    private final Class<? extends Point> pointClass;

    @Deprecated
    public XdmValueResource(URI uri, XdmValue value) {
	this.uri = uri;
	this.value = value;
	this.processor = null;
	this.pointClass = null;
    }

    @Deprecated
    /**
     * Makes a new {@link XdmValueResource}.
     */
    public XdmValueResource(URI uri, XdmValue value, Processor processor) {
	this.uri = uri;
	this.value = value;
	this.processor = processor;
	this.pointClass = null;
    }

    /**
     * Makes a new {@link XdmValueResource}.
     *
     * @param uri - the URI of the resource
     * @param value - the Xdm value making the resource's content
     * @param processor - Saxon {@link Processor}
     * @param pointClass - the {@link Point} class for the serialized resource
     */
    public XdmValueResource(URI uri, XdmValue value, Processor processor, Class<? extends Point> pointClass) {
	this.uri = uri;
	this.value = value;
	this.processor = processor;
	this.pointClass = pointClass;
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

    /**
     * Returns the class of the pointer for the serialized resource.
     *
     */
    public Class<? extends Point> getPointClass() {
	return pointClass;
    }
}
