package de.wwu.scdh.annotation.selection.rewriter;

import static org.junit.jupiter.api.Assertions.*;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.point.*;
import de.wwu.scdh.annotation.selection.resource.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltPackage;
import org.junit.jupiter.api.*;

public class TestXPathRefinedByRFC5147CharSchemeForwardMapper {

	public static final Processor PROC = new Processor(false);

	public static final File LIBTRACE_XML =
			Paths.get("src", "main", "resources", "xslt", "libtrace-xml.xsl").toFile();

	public static final File TEST_DIR = Paths.get("..", "test").toFile();

	public static final URI GESANG_XML = new File(TEST_DIR, "Gesang.tei.xml").toURI();

	public static final File ID_XSL =
			Paths.get("src", "test", "resources", "xsl", "id.xsl").toFile();

	public static final File TEXT_WITH_TOC_XHTML_XSL = Paths.get(
					"src", "test", "resources", "xsl", "text-with-toc-xhtml.xsl")
			.toFile();

	public static final File TEXT_WITH_TOC_HTML_XSL = Paths.get(
					"src", "test", "resources", "xsl", "text-with-toc-html.xsl")
			.toFile();

	public static final File TEXT_XSL =
			Paths.get("src", "test", "resources", "xsl", "text.xsl").toFile();

	public static final RewriterConfig CONFIG = new RewriterConfig(Mode.FIRST, false, null, false, false);

	public static final RewriterConfig CONFIG_NO_NS = new RewriterConfig(Mode.FIRST, false, null, true, true);

	public static XdmValue transform(DOMResource resource, File stylesheet, File pkg) throws SaxonApiException {
		XsltCompiler compiler = PROC.newXsltCompiler();
		XsltPackage xsltPackage = compiler.compilePackage(pkg);
		compiler.importPackage(xsltPackage);
		XsltExecutable executable = compiler.compile(stylesheet);
		Xslt30Transformer transformer = executable.load30();
		return transformer.applyTemplates(resource.getContents());
	}

	@Test
	public void testGesangMappedWithIdentity() throws ResourceException, SaxonApiException, SelectorException {
		DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, PROC);
		MappedDOMResource preimage = new MappedDOMResource(source);
		XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, ID_XSL, LIBTRACE_XML));
		preimage.setImage(image);
		XPathRefinedByRFC5147CharSchemeForwardMapper mapper =
				new XPathRefinedByRFC5147CharSchemeForwardMapper("path(.)");
		XPathRefinedByRFC5147CharScheme preimagePoint =
				new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 5);
		List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, CONFIG);
		assertEquals(1, mapped.size());
		assertEquals(
				mapped.get(0).getXPath(),
				"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[1]");
		assertEquals(mapped.get(0).getChar(), 5);
	}

	@Test
	public void testForwardToMultipleNodes() throws ResourceException, SaxonApiException, SelectorException {
		// we transform a point that occurs twice in the output
		XPathRefinedByRFC5147CharScheme preimagePoint =
				new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 5);
		DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, PROC);
		MappedDOMResource preimage = new MappedDOMResource(source);
		XdmValueResource image =
				new XdmValueResource(GESANG_XML, transform(source, TEXT_WITH_TOC_XHTML_XSL, LIBTRACE_XML));
		preimage.setImage(image);
		XPathRefinedByRFC5147CharSchemeForwardMapper mapper =
				new XPathRefinedByRFC5147CharSchemeForwardMapper("path(.)");
		List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, CONFIG);
		assertEquals(2, mapped.size());

		assertEquals(
				"/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}div[1]/Q{http://www.w3.org/1999/xhtml}h2[1]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[1]",
				mapped.get(0).getXPath());
		assertTrue(
				mapped.get(0)
						.getXPath()
						.endsWith(
								"/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}div[1]/Q{http://www.w3.org/1999/xhtml}h2[1]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[1]"));
		assertEquals(mapped.get(0).getChar(), 5);

		assertEquals(
				"/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}div[2]/Q{http://www.w3.org/1999/xhtml}div[1]/Q{http://www.w3.org/1999/xhtml}h2[1]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[1]",
				mapped.get(1).getXPath());
		assertTrue(
				mapped.get(1)
						.getXPath()
						.endsWith(
								"/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}div[2]/Q{http://www.w3.org/1999/xhtml}div[1]/Q{http://www.w3.org/1999/xhtml}h2[1]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[1]"));
		assertEquals(mapped.get(1).getChar(), 5);
	}

	@Test
	public void testForwardToZeroNodes() throws ResourceException, SaxonApiException, SelectorException {
		// we transform a point that occurs twice in the output
		XPathRefinedByRFC5147CharScheme preimagePoint = new XPathRefinedByRFC5147CharScheme(
				"/*:TEI[1]/*:teiHeader[1]/*:fileDesc[1]/*:titleStmt[1]/*:author[1]", 5);
		DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, PROC);
		MappedDOMResource preimage = new MappedDOMResource(source);
		XdmValueResource image =
				new XdmValueResource(GESANG_XML, transform(source, TEXT_WITH_TOC_XHTML_XSL, LIBTRACE_XML));
		preimage.setImage(image);
		XPathRefinedByRFC5147CharSchemeForwardMapper mapper =
				new XPathRefinedByRFC5147CharSchemeForwardMapper("path(.)");
		List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, CONFIG);
		assertEquals(0, mapped.size());
	}

	@Test
	public void testHtmlForwardToOneNode() throws ResourceException, SaxonApiException, SelectorException {
		// we transform a point that occurs twice in the output
		XPathRefinedByRFC5147CharScheme preimagePoint =
				new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:l[2]/text()[2]", 7);
		DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, PROC);
		MappedDOMResource preimage = new MappedDOMResource(source);
		XdmValueResource image =
				new XdmValueResource(GESANG_XML, transform(source, TEXT_WITH_TOC_HTML_XSL, LIBTRACE_XML));
		preimage.setImage(image);
		XPathRefinedByRFC5147CharSchemeForwardMapper mapper =
				new XPathRefinedByRFC5147CharSchemeForwardMapper("path(.)");
		List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, CONFIG);
		assertEquals(1, mapped.size());
		assertEquals(
				"/Q{}html[1]/Q{}body[1]/Q{}div[2]/Q{}div[1]/Q{}p[2]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[2]",
				mapped.get(0).getXPath(),
				"fn:path returns QNames for elements on the '' namespace as Q{}*.");
	}

	@Test
	public void testHtmlNoNsForwardToOneNode() throws ResourceException, SaxonApiException, SelectorException {
		// we transform a point that occurs twice in the output
		XPathRefinedByRFC5147CharScheme preimagePoint =
				new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:l[2]/text()[2]", 7);
		DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, PROC);
		MappedDOMResource preimage = new MappedDOMResource(source);
		XdmValueResource image =
				new XdmValueResource(GESANG_XML, transform(source, TEXT_WITH_TOC_HTML_XSL, LIBTRACE_XML));
		preimage.setImage(image);
		XPathRefinedByRFC5147CharSchemeForwardMapper mapper =
				new XPathRefinedByRFC5147CharSchemeForwardMapper("path(.)");
		List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, CONFIG_NO_NS);
		assertEquals(1, mapped.size());
		assertEquals(
				"/html[1]/body[1]/div[2]/div[1]/p[2]/text()[2]",
				mapped.get(0).getXPath(),
				"configured to return nice path expressions into HTML");
	}

	@Test
	public void testTextToOneNode() throws ResourceException, SaxonApiException, SelectorException {
		DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, PROC);
		MappedDOMResource preimage = new MappedDOMResource(source);
		XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML));
		preimage.setImage(image);
		XPathRefinedByRFC5147CharSchemeForwardMapper mapper =
				new XPathRefinedByRFC5147CharSchemeForwardMapper("path(.)");
		XPathRefinedByRFC5147CharScheme preimagePoint =
				new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 5);
		List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, CONFIG);
		assertEquals(1, mapped.size());
		// the XdmValue output looks a bit weird: all text containers are directly in document node
		assertTrue(mapped.get(0).getXPath().endsWith("/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[22]"));
		assertEquals(5, mapped.get(0).getChar());
	}

	@Test
	public void testTextToOneNodeWithRootOffset() throws ResourceException, SaxonApiException, SelectorException {
		DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, PROC);
		MappedDOMResource preimage = new MappedDOMResource(source);
		XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, TEXT_XSL, LIBTRACE_XML));
		preimage.setImage(image);
		// let $ctx:=., $rt:=$ctx/ancestor-or-self::node()[parent::node()[not(parent::node())]] return path($rt)
		XPathRefinedByRFC5147CharSchemeForwardMapper mapper = new XPathRefinedByRFC5147CharSchemeForwardMapper(
				"path(ancestor-or-self::node()[ parent::node()[not(parent::node())]  ]) ");
		XPathRefinedByRFC5147CharScheme preimagePoint =
				new XPathRefinedByRFC5147CharScheme("/*:TEI[1]/*:text[1]/*:body[1]/*:lg[1]/*:head[1]", 5);
		List<XPathRefinedByRFC5147CharScheme> mapped = mapper.rewrite(preimage, preimagePoint, CONFIG);
		assertEquals(1, mapped.size());
		assertTrue(mapped.get(0).getXPath().endsWith("/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[22]"));
		assertEquals(5, mapped.get(0).getChar());
	}
}
