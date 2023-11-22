package de.wwu.scdh.annotation.selection;

import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XPathSelector;

import org.apache.commons.lang3.tuple.Pair;


public class TestXPathNormalizer {

    public static final Processor PROC = new Processor(false);

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_HTML = new File(TEST_DIR, "Gesang.tei.html").toURI();
    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();
    public static final URI SOLAR_XML  = new File(TEST_DIR, "solar.xml").toURI();

    private class DummyNormalizer extends XPathNormalizer {
	public DummyNormalizer(DOMResource resource) {
	    super(resource);
	}
	@Override
	protected String getNormalizedXPath(XdmNode node, boolean escaped) throws SelectorException {
	    return "";
	}
    }

    private String getXPath(XdmNode node) throws SaxonApiException {
	XPathCompiler compiler = PROC.newXPathCompiler();
	XPathExecutable executable = compiler.compile("path(.)");
	XPathSelector selector = executable.load();
	selector.setContextItem(node);
	XdmValue result = selector.evaluate();
	return result.itemAt(0).getStringValue();
    }


    @Test
    public void testPathExpressionXPathRootElement() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "/*";
	Pair<XdmNode, Integer> result = normalizer.getTextNodeAtPosition(xpath, 0, false);
	// this selects a text node
	assertEquals("TEXT", result.getLeft().getNodeKind().name());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", getXPath(result.getLeft()));
	assertEquals("TEI", result.getLeft().getParent().getNodeName().getLocalName());
	assertEquals(0, result.getRight());
    }

    @Disabled
    @Test
    public void testPathExpressionXPathRootElementChar4() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "/*";
	Pair<XdmNode, Integer> result = normalizer.getTextNodeAtPosition(xpath, 3, false);
	// this selects a text node
	assertEquals("TEXT", result.getLeft().getNodeKind().name());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}teiHeader[1]/text()[1]", getXPath(result.getLeft()));
	assertEquals(0, result.getRight());
    }

    @Test
    public void testPathExpressionXPathRootElementPos39() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "/*";
	Pair<XdmNode, Integer> result = normalizer.getTextNodeAtPosition(xpath, 39, false);
	// this selects a text node
	assertEquals("TEXT", result.getLeft().getNodeKind().name());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}teiHeader[1]/Q{http://www.tei-c.org/ns/1.0}fileDesc[1]/Q{http://www.tei-c.org/ns/1.0}titleStmt[1]/Q{http://www.tei-c.org/ns/1.0}title[1]/text()[1]", getXPath(result.getLeft()));
	assertEquals("title", result.getLeft().getParent().getNodeName().getLocalName());
	assertEquals(6, result.getRight());
    }

    @Test
    public void testPathExpressionXPathVerse1() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "id('v2')/text()[2]";
	Pair<XdmNode, Integer> result = normalizer.getTextNodeAtPosition(xpath, 6, false);
	// this selects a text node
	assertEquals("TEXT", result.getLeft().getNodeKind().name());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/text()[2]", getXPath(result.getLeft()));
	assertEquals("l", result.getLeft().getParent().getNodeName().getLocalName());
	assertEquals(6, result.getRight());
    }

    @Test
    public void testPathExpressionXPathVerse1Char10() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "id('v2')";
	Pair<XdmNode, Integer> result = normalizer.getTextNodeAtPosition(xpath, 10, false);
	// this selects a text node
	assertEquals("TEXT", result.getLeft().getNodeKind().name());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}rdg[1]/text()[1]", getXPath(result.getLeft()));
	assertEquals("rdg", result.getLeft().getParent().getNodeName().getLocalName());
	assertEquals(1, result.getRight());
    }

    @Test
    public void testPathExpressionXPathVerse1Char16() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "id('v2')";
	Pair<XdmNode, Integer> result = normalizer.getTextNodeAtPosition(xpath, 16, false);
	// this selects a text node
	assertEquals("TEXT", result.getLeft().getNodeKind().name());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/text()[2]", getXPath(result.getLeft()));
	assertEquals("l", result.getLeft().getParent().getNodeName().getLocalName());
	assertEquals(3, result.getRight());
    }

    @Test
    public void testPathExpressionXPathVerse1CaesuraThrows() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "id('v1')/*:caesura[1]";
	assertThrows(SelectorException.class, () -> normalizer.getTextNodeAtPosition(xpath, 0, false));
    }

    @Test
    public void testPathExpressionXPathVerse1Char100Throws() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "id('v1')";
	// Pair<XdmNode, Integer> result = normalizer.getTextNodeAtPosition(xpath, 100, false);
	assertThrows(SelectorException.class, () -> normalizer.getTextNodeAtPosition(xpath, 100, false));
    }

    @Test
    public void testAmbiguityOnSolarXML() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(SOLAR_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	Pair<XdmNode, Integer> result;

	result = normalizer.getTextNodeAtPosition("/*:r", 0, false);
	assertEquals("Sol", result.getLeft().toString());
	assertEquals(0, result.getRight());

	result = normalizer.getTextNodeAtPosition("/*:r", 3, false);
	assertEquals("Sol", result.getLeft().toString());
	assertEquals(3, result.getRight());

	result = normalizer.getTextNodeAtPosition("/*:r", 3, true);
	assertEquals("ar", result.getLeft().toString());
	assertEquals(0, result.getRight());

	result = normalizer.getTextNodeAtPosition("/*:r", 4, false);
	assertEquals("ar", result.getLeft().toString());
	assertEquals(1, result.getRight());

	result = normalizer.getTextNodeAtPosition("/*:r", 4, true);
	assertEquals("ar", result.getLeft().toString());
	assertEquals(1, result.getRight());

	result = normalizer.getTextNodeAtPosition("/*:r", 5, false);
	assertEquals("ar", result.getLeft().toString());
	assertEquals(2, result.getRight());

	result = normalizer.getTextNodeAtPosition("/*:r", 5, true);
	assertEquals("!", result.getLeft().toString());
	assertEquals(0, result.getRight());

	result = normalizer.getTextNodeAtPosition("/*:r", 6, false);
	assertEquals("!", result.getLeft().toString());
	assertEquals(1, result.getRight());

    	result = normalizer.getTextNodeAtPosition("/*:r", 6, true);
	assertEquals("!", result.getLeft().toString());
	assertEquals(1, result.getRight());
    }

}
