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

public class TestXPathRefinedByRFC5147CharSchemeToTextForwardMapper {

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
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML));
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextForwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextForwardMapper("path(.)");
	XPathRefinedByRFC5147CharScheme preimagePoint =
	    new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 5);
	List<RFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, config);
	assertEquals(1, mapped.size());
	int charPos = mapped.get(0).getChar();
	assertEquals(5 + 248 - 1, charPos);
	String serialized = serialize(image.getContents(), "text");
	assertEquals("-", serialized.substring(charPos, charPos + 1));
    }

    @Test
    public void testTextToOneNodeWithXPathAndPos0() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML));
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextForwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextForwardMapper("path(.)");
	XPathRefinedByRFC5147CharScheme preimagePoint =
	    new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 0);
	List<RFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, config);
	assertEquals(1, mapped.size());
	int charPos = mapped.get(0).getChar();
	assertEquals(0 + 248 - 1, charPos);
	String serialized = serialize(image.getContents(), "text");
	assertEquals("T", serialized.substring(charPos, charPos + 1));
    }

    @Test
    public void testTextToOneNodeWithPos0Char() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML));
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextForwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextForwardMapper("path(.)");
	XPathRefinedByRFC5147CharScheme preimagePoint =
	    new XPathRefinedByRFC5147CharScheme("/*", 0);
	List<RFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, config);
	assertEquals(1, mapped.size());
	int charPos = mapped.get(0).getChar();
	assertEquals(0, charPos);
	String serialized = serialize(image.getContents(), "text");
	assertEquals("\n", serialized.substring(charPos, charPos + 1));
    }

    @Test
    public void testTextToNoneNode() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, NO_HEADER_XSL, LIBTRACE_XML));
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextForwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextForwardMapper("path(.)");
	XPathRefinedByRFC5147CharScheme preimagePoint =
	    new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 5);
	List<RFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, config);
	assertEquals(0, mapped.size());
    }

    @Test
    public void testTextToOptionalNodeWithXPathAndPos6() throws ResourceException, SaxonApiException, SelectorException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, NO_HEADER_XSL, LIBTRACE_XML));
	preimage.setImage(image);
	XPathRefinedByRFC5147CharSchemeToTextForwardMapper mapper = new XPathRefinedByRFC5147CharSchemeToTextForwardMapper("path(.)");
	XPathRefinedByRFC5147CharScheme preimagePoint =
	    new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:l[2]/text()[2]", 6);
	List<RFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, config);
	assertEquals(1, mapped.size());
	int charPos = mapped.get(0).getChar();
	assertEquals(125 - 1, charPos);
	String serialized = serialize(image.getContents(), "text");
	assertEquals("g", serialized.substring(charPos, charPos + 1));
    }

}
