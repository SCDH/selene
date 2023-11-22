package de.wwu.scdh.annotation.selection.mapping;

import java.net.URI;
import java.util.Map;
import java.net.MalformedURLException;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.core.type.TypeReference;

class XPathLookup {

    @JsonProperty("html-base-uri")
    protected URI htmlBaseUri;

    @JsonProperty("html-src-mapping")
    protected Map<String,LookupSourceNode> htmlSourceMapping;

    public static XPathLookup fromJson(URI lookupUri) throws LookupCreationException {
	ObjectMapper mapper = new ObjectMapper();
	try {
	    return mapper.readValue(lookupUri.toURL(), new TypeReference<XPathLookup>() {});
	} catch (Exception e) {
	    throw new LookupCreationException(e);
	}
    }
    
}
