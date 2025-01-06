package de.wwu.scdh.annotation.selection.rewriter;

import java.util.List;
import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

import de.wwu.scdh.annotation.selection.*;


public class TestXPathNormalizerWithXPath {

    public static final Processor PROC = new Processor(false);

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_HTML = new File(TEST_DIR, "Gesang.tei.html").toURI();
    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();

    RewriterConfig config = new RewriterConfig(null, false, null);

    @Test
    public void testWithPathFunction() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath("path(.)");
	assertEquals("path(.)", normalizer.getXPath());
	String xpath;
	List<XPathRefinedByRFC5147CharScheme> result;
	XPathRefinedByRFC5147CharScheme point;

	// root element, char=0
	xpath = "/*";
	point = new XPathRefinedByRFC5147CharScheme(xpath, 0);
	result = normalizer.rewrite(resource, point, RewriterConfig.withMode(config, Mode.DEEP_NODE_STOP_AT_END));
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", result.get(0).getXPath());
	assertEquals(0, result.get(0).getChar());

	// verse 2 //app, char=6
	xpath = "id('v2')//*:app";
	point = new XPathRefinedByRFC5147CharScheme(xpath, 6);
	result = normalizer.rewrite(resource, point, RewriterConfig.withMode(config, Mode.DEEP_NODE_STOP_AT_END));
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}rdg[1]/text()[1]", result.get(0).getXPath());
	assertEquals(1, result.get(0).getChar());
    }

    @Test
    public void testWithDeepestIdClarkXPathOnRootElement() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	String xpath;
	List<XPathRefinedByRFC5147CharScheme> result;
	XPathRefinedByRFC5147CharScheme point;

	// root element, char=0
	xpath = "/*";
	point = new XPathRefinedByRFC5147CharScheme(xpath, 0);
	result = normalizer.rewrite(resource, point, RewriterConfig.withMode(config, Mode.DEEP_NODE_STOP_AT_END));
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", result.get(0).getXPath());
	assertEquals(0, result.get(0).getChar());
    }

    @Test
    public void testWithDeepestIdClarkXPathVerse2AppChar6() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	String xpath;
	List<XPathRefinedByRFC5147CharScheme> result;
	XPathRefinedByRFC5147CharScheme point;

	// verse 2 //app, char=6
	xpath = "id('v2')//*:app";
	point = new XPathRefinedByRFC5147CharScheme(xpath, 6);
	result = normalizer.rewrite(resource, point, RewriterConfig.withMode(config, Mode.DEEP_NODE_STOP_AT_END));
	assertEquals("id(&apos;v2&apos;)/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}rdg[1]/text()[1]", result.get(0).getXPath());
	assertEquals(1, result.get(0).getChar());
    }

    @Test
    public void testPathExpressionClarkXPathOnRootElement() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_ROOT_CLARK_XPATH);
	String xpath;
	List<XPathRefinedByRFC5147CharScheme> result;
	XPathRefinedByRFC5147CharScheme point;

	// root element, char=0
	xpath = "/*";
	point = new XPathRefinedByRFC5147CharScheme(xpath, 0);
	result = normalizer.rewrite(resource, point, RewriterConfig.withMode(config, Mode.DEEP_NODE_STOP_AT_END));
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", result.get(0).getXPath());
	assertEquals(0, result.get(0).getChar());
    }

    @Test
    public void testPathExpressionClarkXPathOnVerse2AppChar6() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizerWithXPath normalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_ROOT_CLARK_XPATH);
	String xpath;
	List<XPathRefinedByRFC5147CharScheme> result;
	XPathRefinedByRFC5147CharScheme point;

	// verse 2 //app, char=6
	xpath = "id('v2')//*:app";
	point = new XPathRefinedByRFC5147CharScheme(xpath, 6);
	result = normalizer.rewrite(resource, point, RewriterConfig.withMode(config, Mode.DEEP_NODE_STOP_AT_END));
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}rdg[1]/text()[1]", result.get(0).getXPath());
	assertEquals(1, result.get(0).getChar());
    }

}
