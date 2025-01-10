package de.wwu.scdh.annotation.selection.resource;

import java.net.URI;

import org.w3c.dom.Document;

import de.wwu.scdh.annotation.selection.Resource;


public class W3CDOMResource implements Resource<Document> {

    private final Document document;
    private final URI uri;

    public W3CDOMResource(URI uri, Document document) {
	this.uri = uri;
	this.document = document;
    }

    @Override
    public Document getContents() {
	return document;
    }

    @Override
    public URI getUri() {
	return uri;
    }

}
