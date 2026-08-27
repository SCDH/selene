package de.wwu.scdh.annotation.selection.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

public class TestStaticDocumentLoader {

	@Test
	public void testDefaultContextMapping() {
		assertNotNull(StaticDocumentLoader.CONTEXT_MAPPING, "context mapping exists and is accessible");
	}

	@Test
	public void testNoArgumentConstructor() throws URISyntaxException {
		StaticDocumentLoader loader = new StaticDocumentLoader();
		assertTrue(
				loader.hasLocal(new URI("https://www.w3.org/ns/anno.jsonld")),
				"has local version of Web Annotations context (https)");
		assertTrue(
				loader.hasLocal(new URI("http://www.w3.org/ns/anno.jsonld")),
				"has local version of Web Annotations context (http)");
	}
}
