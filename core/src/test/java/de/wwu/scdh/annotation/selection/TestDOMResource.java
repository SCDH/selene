package de.wwu.scdh.annotation.selection;

import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;


public class TestDOMResource {

    public static final Processor PROC = new Processor(false);

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_HTML = new File(TEST_DIR, "Gesang.tei.html").toURI();
    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();

    @Test
    void testFromHTMLonGesangHtml() throws IOException, SaxonApiException {
	// parsing HTML with the HTML parser does not fail
	DOMResource resource = DOMResource.fromHTML(GESANG_HTML, null, PROC);
	assertEquals(resource.getUri(), GESANG_HTML);
    }

    @Test
    void testFromHTMLonGesangXml() throws IOException, SaxonApiException {
	// HTML parser is not suitable for XML
	assertThrows(Exception.class, () -> DOMResource.fromHTML(GESANG_XML, null, PROC));
    }

}
