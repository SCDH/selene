package de.wwu.scdh.annotation.selection.mapping;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;


public class TestXPathLookup {

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI MOPS_XML  = new File(TEST_DIR, "Mops.tei.xml").toURI();
    public static final URI MOPS_XHTML  = new File(TEST_DIR, "Mops.tei.xhtml").toURI();
    public static final URI MOPS_SRCMAP  = new File(TEST_DIR, "Mops.srcmap.json").toURI();

    @Test
    public void testFromJsonMops() throws LookupCreationException {
	XPathLookup lookup = XPathLookup.fromJson(MOPS_SRCMAP);
	assertNotNull(lookup.htmlBaseUri);
	// assertEquals("", lookup.htmlBaseUri);
	assertTrue(lookup.htmlBaseUri.toString().endsWith("ops.xhtml"));
	assertEquals(37, lookup.htmlSourceMapping.size());
    }

}
