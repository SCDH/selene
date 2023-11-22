package de.wwu.scdh.annotation.selection;

import java.io.IOException;
import java.util.Iterator;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Axis;

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

    @Test
    void testFromXMLonGesangHtml() throws IOException, SaxonApiException {
	// parsing XML with the XML parser does not fail
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	assertEquals(resource.getUri(), GESANG_XML);
    }

    @Test
    void testFromXMLonGesangXml() throws IOException, SaxonApiException {
	// XML parser is not suitable for HTML
	assertThrows(Exception.class, () -> DOMResource.fromXML(GESANG_HTML, null, PROC));
    }

    @Test
    void testGetPreImage() throws IOException, SaxonApiException {
	// without any validation: test the getPreImage() getter
	DOMResource preimage = DOMResource.fromXML(GESANG_XML, null, PROC);
	DOMResource resource = DOMResource.fromHTML(GESANG_HTML, preimage, PROC);
	assertEquals(preimage, resource.getPreImage());
    }

    @Test
    void testHTMLParserOnGesangHtml() throws IOException, SaxonApiException {
	// assert that the HTML parser provides a correct DOM
	DOMResource resource = DOMResource.fromHTML(GESANG_HTML, null, PROC);
	assertEquals(resource.getUri(), GESANG_HTML);
	// correct document node
	assertEquals("DOCUMENT", resource.getDOM().getNodeKind().name());
	// test other nodes
	Iterator<XdmNode> nodes = resource.getDOM().axisIterator(Axis.DESCENDANT);
	XdmNode node;
	// html root node
	node = nodes.next();
	assertEquals("ELEMENT", node.getNodeKind().name());
	assertEquals("html", node.getNodeName().getLocalName());
    }

    @Test
    void testBeforeHeadInsertionModeOnGesangHtml() throws IOException, SaxonApiException {
	// assert that the HTML parser ignores white space nodes before <head>
	// see https://html.spec.whatwg.org/multipage/parsing.html#the-before-head-insertion-mode
	// see https://stackoverflow.com/questions/26800626/what-whitespaces-in-html-are-considered-whitespace-nodes
	DOMResource resource = DOMResource.fromHTML(GESANG_HTML, null, PROC);
	assertEquals(resource.getUri(), GESANG_HTML);
	// correct document node
	assertEquals("DOCUMENT", resource.getDOM().getNodeKind().name());
	// test other nodes
	Iterator<XdmNode> nodes = resource.getDOM().axisIterator(Axis.DESCENDANT);
	XdmNode node;
	node = nodes.next(); // html root node
	node = nodes.next(); // ignore white space node before head
	assertEquals("ELEMENT", node.getNodeKind().name());
	assertEquals("head", node.getNodeName().getLocalName());
    }

}
