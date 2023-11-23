package de.wwu.scdh.annotation.selection.mapping;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.io.IOException;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;
import de.wwu.scdh.annotation.selection.XPathNormalizerWithXPath;


public class TestXPathMapperInMemory {

    public static final Processor PROC = new Processor();
    
    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI MOPS_XML  = new File(TEST_DIR, "Mops.tei.xml").toURI();
    public static final URI MOPS_XHTML  = new File(TEST_DIR, "Mops.tei.xhtml").toURI();
    public static final URI MOPS_SRCMAP  = new File(TEST_DIR, "Mops.srcmap.json").toURI();

    @Disabled // duplicate values error, see issue #11
    @Test
    public void testGetNormalizedPreimageSelector() throws MappingException, LookupCreationException, SaxonApiException, IOException {
	DOMResource xml = DOMResource.fromXML(MOPS_XML, null, PROC);
	DOMResource html = DOMResource.fromXML(MOPS_XHTML, xml, PROC);
	XPathNormalizer xmlNormalizer = new XPathNormalizerWithXPath(xml, XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	XPathNormalizer htmlNormalizer = new XPathNormalizerWithXPath(html, XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	XPathLookup lookup = XPathLookup.fromJson(MOPS_SRCMAP);
	XPathMapper mapper = new InMemoryXPathMapper(xmlNormalizer, htmlNormalizer, lookup);
    }
}
