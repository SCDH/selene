package de.wwu.scdh.annotation.selection.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LookupSourceNode {

    @JsonProperty("length")
    protected int length;

    @JsonProperty("offset")
    protected int offset;

    @JsonProperty("xpath")
    protected String xpath;

}
