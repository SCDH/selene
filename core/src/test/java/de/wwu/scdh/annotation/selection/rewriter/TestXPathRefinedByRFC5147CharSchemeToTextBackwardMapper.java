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

public class TestXPathRefinedByRFC5147CharSchemeToTextBackwardMapper {

    public static final Processor PROC = new Processor(false);

    public static final File LIBTRACE_XML = Paths.get("src", "main", "resources", "xslt", "libtrace-xml.xsl").toFile();

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();

    public static final File TEXT_XSL = Paths.get("src", "test", "resources", "xsl", "text.xsl").toFile();

    public static final File NO_HEADER_XSL = Paths.get("src", "test", "resources", "xsl", "text-no-header.xsl").toFile();

    RewriterConfig config = new RewriterConfig(Mode.FIRST, false, null);

    public static XdmValue transform(DOMResource resource, File stylesheet, File pkg) throws SaxonApiException {
	XsltCompiler compiler = PROC.newXsltCompiler();
	XsltPackage xsltPackage = compiler.compilePackage(pkg);
	compiler.importPackage(xsltPackage);
	XsltExecutable executable = compiler.compile(stylesheet);
	Xslt30Transformer transformer = executable.load30();
	return transformer.applyTemplates(resource.getContents());
    }

    public static String serialize(XdmValue xdmValue, String method) throws SaxonApiException {
	StringWriter writer = new StringWriter();
	Serializer serializer = PROC.newSerializer(writer);
	serializer.setOutputProperty(Serializer.Property.METHOD, method);
	serializer.setOutputProperty(Serializer.Property.INDENT, "true");
	serializer.serializeXdmValue(xdmValue);
	return writer.toString();
    }


    @Test
    public void testSerializedToPlainText() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML));
	preimage.setImage(image);
	String serialized = serialize(image.getContents(), "text");
	assertThat(serialized, containsString("Bey stiller nacht"));
	assertThat(serialized, not(containsString("<")));
    }

    @Test
    public void testTextToOneNodeWithXPathAndPos5() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	assertEquals(PROC, image.getProcessor());
	assertEquals(PROC, preimage.getImage().getProcessor());
	XPathRefinedByRFC5147CharSchemeToTextBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextBackwardMapper("path(.)");
	//("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]/text()[1]", 5);
	RFC5147CharScheme imagePoint = new RFC5147CharScheme(252);
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, config);
	assertEquals(1, mapped.size());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/text()[1]",
		     mapped.get(0).getXPath());
	assertEquals(5, mapped.get(0).getChar());
    }

    @Test
    public void testTextToOneNodeWithXPathAndPos0() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextBackwardMapper("path(.)");
	RFC5147CharScheme imagePoint = new RFC5147CharScheme(247); // ambivalence!
	//("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 0);
	//("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]", 16);
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, config);
	assertEquals(1, mapped.size());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/text()[1]",
		     mapped.get(0).getXPath());
	assertEquals(16, mapped.get(0).getChar());
    }

    @Test
    public void testTextToOneNodeWithXPathAndPos0WithDeepestConfig() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextBackwardMapper("path(.)");
	RFC5147CharScheme imagePoint = new RFC5147CharScheme(247); // ambivalence!
	//("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 0);
	//("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]", 16);
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, RewriterConfig.withMode(config, Mode.DEEP_NODE_STEP_OVER_END));
	assertEquals(1, mapped.size());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/text()[1]",
		     mapped.get(0).getXPath());
	assertEquals(0, mapped.get(0).getChar());
    }

    @Test
    public void testTextToOneNodeWithPos0Char() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextBackwardMapper("path(.)");
	RFC5147CharScheme imagePoint = new RFC5147CharScheme(0);
	// ("/*", 0);
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, config);
	assertEquals(1, mapped.size());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/text()[1]", mapped.get(0).getXPath());
	assertEquals(0, mapped.get(0).getChar());
    }

    @Test
    public void testTextToNoneNode() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, NO_HEADER_XSL, LIBTRACE_XML), PROC);
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextBackwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextBackwardMapper("path(.)");
	RFC5147CharScheme imagePoint = new RFC5147CharScheme(125);
	List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, imagePoint, config);
	assertEquals(1, mapped.size());
	assertEquals("/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/text()[2]",
		     mapped.get(0).getXPath());
	assertEquals(7, mapped.get(0).getChar());
    }

}
