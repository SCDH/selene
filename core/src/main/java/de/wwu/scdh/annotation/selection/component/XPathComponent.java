package de.wwu.scdh.annotation.selection.component;

import de.wwu.scdh.annotation.selection.Component;


/**
 * An {@link XPathComponent} is a {@link Component} of a {@link Point}
 * with a String value that represents an XPath into the resource.
 */
public class XPathComponent implements Component<String> {

    private final String value;

    public XPathComponent(String value) {
	this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<String> getType() {
	return String.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue() {
	return value;
    }

}
