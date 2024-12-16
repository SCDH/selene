package de.wwu.scdh.annotation.selection;

import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

import org.apache.commons.lang3.tuple.Pair;

import de.wwu.scdh.annotation.selection.Mode;


public class TestXPathNormalizerWithXPath {

    public static final Processor PROC = new Processor(false);

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_HTML = new File(TEST_DIR, "Gesang.tei.html").toURI();
    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();

    @Test
    public void testWithPathFunction() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath("path(.)");
	assertEquals("path(.)", normalizer.getXPath());
	String xpath;
	Pair<String, Integer> result;

	// root element, char=0
	xpath = "/*";
	result = normalizer.normalizeXPathRefinedByCharScheme(resource, xpath, 0, Mode.DEEP_NODE_STOP_AT_END);
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", result.getLeft());
	assertEquals(0, result.getRight());

	// verse 2 //app, char=6
	xpath = "id('v2')//*:app";
	result = normalizer.normalizeXPathRefinedByCharScheme(resource, xpath, 6, Mode.DEEP_NODE_STOP_AT_END);
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}rdg[1]/text()[1]", result.getLeft());
	assertEquals(1, result.getRight());
    }

    @Test
    public void testWithDeepestIdClarkXPathOnRootElement() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	String xpath;
	Pair<String, Integer> result;

	// root element, char=0
	xpath = "/*";
	result = normalizer.normalizeXPathRefinedByCharScheme(resource, xpath, 0, Mode.DEEP_NODE_STOP_AT_END);
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", result.getLeft());
	assertEquals(0, result.getRight());
    }

    @Test
    public void testWithDeepestIdClarkXPathVerse2AppChar6() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	String xpath;
	Pair<String, Integer> result;

	// verse 2 //app, char=6
	xpath = "id('v2')//*:app";
	result = normalizer.normalizeXPathRefinedByCharScheme(resource, xpath, 6, Mode.DEEP_NODE_STOP_AT_END);
	assertEquals("id(&apos;v2&apos;)/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}rdg[1]/text()[1]", result.getLeft());
	assertEquals(1, result.getRight());
    }

    @Test
    public void testPathExpressionClarkXPathOnRootElement() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_ROOT_CLARK_XPATH);
	String xpath;
	Pair<String, Integer> result;

	// root element, char=0
	xpath = "/*";
	result = normalizer.normalizeXPathRefinedByCharScheme(resource, xpath, 0, Mode.DEEP_NODE_STOP_AT_END);
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", result.getLeft());
	assertEquals(0, result.getRight());
    }

    @Test
    public void testPathExpressionClarkXPathOnVerse2AppChar6() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_ROOT_CLARK_XPATH);
	String xpath;
	Pair<String, Integer> result;

	// verse 2 //app, char=6
	xpath = "id('v2')//*:app";
	result = normalizer.normalizeXPathRefinedByCharScheme(resource, xpath, 6, Mode.DEEP_NODE_STOP_AT_END);
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}rdg[1]/text()[1]", result.getLeft());
	assertEquals(1, result.getRight());
    }

}
