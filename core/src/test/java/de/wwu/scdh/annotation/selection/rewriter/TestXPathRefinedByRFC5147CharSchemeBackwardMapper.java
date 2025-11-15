package de.wwu.scdh.annotation.selection.rewriter;


import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltPackage;
import net.sf.saxon.s9api.Serializer;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.*;
import de.wwu.scdh.annotation.selection.point.*;

public class TestXPathRefinedByRFC5147CharSchemeBackwardMapper {

    public static final Processor PROC = new Processor(false);

    public static final File LIBTRACE_XML = Paths.get("src", "main", "resources", "xslt", "libtrace-xml.xsl").toFile();

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();

    public static final File ID_XSL = Paths.get("src", "test", "resources", "xsl", "id.xsl").toFile();

    public static final File TEXT_WITH_TOC_XSL = Paths.get("src", "test", "resources", "xsl", "text-with-toc.xsl").toFile();

    RewriterConfig config = new RewriterConfig(Mode.FIRST, false, null);

    public static XdmValue transform(DOMResource resource, File stylesheet, File pkg) throws SaxonApiException {
	XsltCompiler compiler = PROC.newXsltCompiler();
	XsltPackage xsltPackage = compiler.compilePackage(pkg);
	compiler.importPackage(xsltPackage);
	XsltExecutable executable = compiler.compile(stylesheet);
	Xslt30Transformer transformer = executable.load30();
	return transformer.applyTemplates(resource.getContents());
    }

    @Test
    public void testTextToOneNodeWithXPathAndPos5() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, ID_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	assertEquals(PROC, image.getProcessor());
	assertEquals(PROC, preimage.getImage().getProcessor());
	XPathRefinedByRFC5147CharSchemeBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeBackwardMapper("path(.)");
	//("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]/text()[1]", 5);
	XPathRefinedByRFC5147CharScheme imagePoint =
	    new XPathRefinedByRFC5147CharScheme("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]", 5);
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, config);
	assertEquals(1, mapped.size());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/text()[1]",
		     mapped.get(0).getXPath());
	assertEquals(5, mapped.get(0).getChar());
    }

    @Test
    public void testAmbiguityWithModeFirst() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, ID_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	assertEquals(PROC, image.getProcessor());
	assertEquals(PROC, preimage.getImage().getProcessor());
	XPathRefinedByRFC5147CharSchemeBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeBackwardMapper("path(.)");
	//("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]/text()[1]", 5);
	XPathRefinedByRFC5147CharScheme imagePoint =
	    new XPathRefinedByRFC5147CharScheme("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]", 0);
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, config);
	assertEquals(1, mapped.size());
	// position at end of lg/text()[1]
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/text()[1]",
		     mapped.get(0).getXPath());
	assertEquals(16, mapped.get(0).getChar());
    }

    @Test
    public void testAmbiguityWithModeDeepNodeStepOverEnd() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, ID_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	assertEquals(PROC, image.getProcessor());
	assertEquals(PROC, preimage.getImage().getProcessor());
	XPathRefinedByRFC5147CharSchemeBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeBackwardMapper("path(.)");
	//("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]/text()[1]", 5);
	XPathRefinedByRFC5147CharScheme imagePoint =
	    new XPathRefinedByRFC5147CharScheme("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]", 0);
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, RewriterConfig.withMode(config, Mode.DEEP_NODE_STEP_OVER_END));
	assertEquals(1, mapped.size());
	// position at end of lg/text()[1]
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/text()[1]",
		     mapped.get(0).getXPath());
	assertEquals(0, mapped.get(0).getChar());
    }

    @Test
    public void testBackwardToCNode() throws ResourceException, SaxonApiException, SelectorException {
	// we transform a point from the TOC, originating from a preimage node that occurs twice in the image
	XPathRefinedByRFC5147CharScheme imagePoint =
	    new XPathRefinedByRFC5147CharScheme("/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}div[1]/Q{http://www.w3.org/1999/xhtml}h2[1]", 5);
	// ("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 5);
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_WITH_TOC_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeBackwardMapper("path(.)");
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, config);
	assertEquals(1, mapped.size());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/text()[1]",
		     mapped.get(0).getXPath());
	assertEquals(5, mapped.get(0).getChar());
    }

    @Test
    public void testBackwardHeadNode() throws ResourceException, SaxonApiException, SelectorException {
	// we transform a point from the H2, originating from a preimage node that occurs twice in the image
	XPathRefinedByRFC5147CharScheme imagePoint =
	    new XPathRefinedByRFC5147CharScheme("/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}div[2]/Q{http://www.w3.org/1999/xhtml}div[1]/Q{http://www.w3.org/1999/xhtml}h2[1]", 5);
	// ("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 5);
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_WITH_TOC_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeBackwardMapper("path(.)");
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, config);
	assertEquals(1, mapped.size());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/text()[1]",
		     mapped.get(0).getXPath());
	assertEquals(5, mapped.get(0).getChar());
    }

}
