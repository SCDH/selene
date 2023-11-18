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


public class TestXPathNormalizer {

    public static final Processor PROC = new Processor(false);

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_HTML = new File(TEST_DIR, "Gesang.tei.html").toURI();
    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();

    private class DummyNormalizer extends XPathNormalizer {
	public DummyNormalizer(DOMResource resource) {
	    super(resource);
	}
	@Override
	protected String getNormalizedXPath(XdmNode node) throws SelectorException {
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
	NodePositionPair result = normalizer.getTextNodeAtPosition(xpath, 0);
	// this selects a text node
	assertEquals("TEXT", result.node().getNodeKind().name());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", getXPath(result.node()));
	assertEquals("TEI", result.node().getParent().getNodeName().getLocalName());
	assertEquals(0, result.position());
    }

    @Test
    public void testPathExpressionXPathRootElementPos39() throws SelectorException, SaxonApiException, IOException {
	DOMResource resource = DOMResource.fromXML(GESANG_XML, null, PROC);
	XPathNormalizer normalizer = new DummyNormalizer(resource);
	String xpath;
	xpath = "/*";
	NodePositionPair result = normalizer.getTextNodeAtPosition(xpath, 39);
	// this selects a text node
	assertEquals("TEXT", result.node().getNodeKind().name());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}teiHeader[1]/Q{http://www.tei-c.org/ns/1.0}fileDesc[1]/Q{http://www.tei-c.org/ns/1.0}titleStmt[1]/Q{http://www.tei-c.org/ns/1.0}title[1]/text()[1]", getXPath(result.node()));
	assertEquals("title", result.node().getParent().getNodeName().getLocalName());
	assertEquals(6, result.position());
    }

}
