package de.wwu.scdh.annotation.selection.resource;

import java.net.URI;

import net.sf.saxon.s9api.XdmValue;

import de.wwu.scdh.annotation.selection.Resource;


public class XdmValueResource implements Resource<XdmValue> {

    private final URI uri;
    private final XdmValue value;

    public XdmValueResource(URI uri, XdmValue value) {
	this.uri = uri;
	this.value = value;
    }

    @Override
    public XdmValue getContents() {
	return value;
    }

    @Override
    public URI getUri() {
	return uri;
    }

}
