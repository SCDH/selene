package de.wwu.scdh.annotation.selection.wadm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.wwu.scdh.annotation.selection.ResourceException;
import de.wwu.scdh.annotation.selection.RewriterConfig;
import de.wwu.scdh.annotation.selection.RewriterFactory;
import de.wwu.scdh.annotation.selection.SEL;
import de.wwu.scdh.annotation.selection.resource.DOMResource;
import de.wwu.scdh.annotation.selection.resource.ResourceBuilder;
import de.wwu.scdh.annotation.selection.rewriter.BackwardMappingFactory;
import de.wwu.scdh.annotation.selection.rewriter.ForwardMappingFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;
import net.sf.saxon.s9api.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * This class tests rewriting WADM annotations with HTML images.
 */
public class TestRewriteAnnotationHtml {

	private static final Processor PROC = new Processor();

	public static final String IRI_SONG = "https://docs.org/Gesang.tei.xml";
	public static final String IRI_SONG_HTML = "https://docs.org/Gesang.html";

	public static final File TEST_DIR = Paths.get("..", "test").toFile();
	public static final File SAMPLE_DIR =
			Paths.get("src", "test", "resources", "samples").toFile();
	public static final File XSL_DIR =
			Paths.get("src", "test", "resources", "xsl").toFile();

	public static final File XPATH_XSL =
			Paths.get("src", "main", "resources", "xslt", "xpath.xsl").toFile();

	public static final String SONG_FW_IN_IMAGE_JSON = new File(SAMPLE_DIR, "gFwInImage.json").toString();
	public static final String SONG_FW_IN_IMAGE2_JSON = new File(SAMPLE_DIR, "gFwInImage2.json").toString();
	public static final String SONG_BW_TO_LEAVE_IN_PREIMAGE_JSON =
			new File(SAMPLE_DIR, "gBwHtmlToLeafInPreimage.json").toString();
	public static final String SONG_BW_TO_ELEMENT_IN_PREIMAGE_JSON =
			new File(SAMPLE_DIR, "gBwHtmlToElementInPreimage.json").toString();
	public static final String SONG_BW_TO_HIGHER_IN_PREIMAGE_JSON =
			new File(SAMPLE_DIR, "gBwHtmlToHigherInPreimage.json").toString();
	public static final String SONG_FW_NOT_IN_IMAGE_JSON = new File(SAMPLE_DIR, "gFwNotInImage.json").toString();
	public static final String SONG_BW_TO_LEAVE_NOT_IN_PREIMAGE_JSON =
			new File(SAMPLE_DIR, "gBwHtmlToLeafNotInPreimage.json").toString();
	public static final String SONG_BW_TO_ELEMENT_NOT_IN_PREIMAGE_JSON =
			new File(SAMPLE_DIR, "gBwHtmlToElementNotInPreimage.json").toString();

	public static final URI SONG_XML = new File(TEST_DIR, "Gesang.tei.xml").toURI();

	private static XPathCompiler XPATH_COMPILER;

	@BeforeAll
	public static void setupXPathCompiler() throws SaxonApiException {
		XPATH_COMPILER = PROC.newXPathCompiler();
		XsltCompiler xsltCompiler = PROC.newXsltCompiler();
		XsltPackage pkg = xsltCompiler.compilePackage(XPATH_XSL);
		XPATH_COMPILER.addXsltFunctionLibrary(pkg);
		XPATH_COMPILER.declareNamespace("sel", "http://wwu.de/scdh/selection-engine/xpaths");
	}

	private final RewriterFactory forwardFactory = new ForwardMappingFactory(XPATH_COMPILER);
	private final RewriterFactory backwardFactory = new BackwardMappingFactory(PROC.newXPathCompiler());
	private RewriterConfig normalizerConfig;

	private Model model;

	private de.wwu.scdh.annotation.selection.Resource<?> songMapped;
	private URI songIri, songIriHtml;

	@BeforeEach
	public void setupResource() throws URISyntaxException, ResourceException, MalformedURLException {
		songIri = new URI(IRI_SONG);
		songIriHtml = new URI(IRI_SONG_HTML);
		ResourceBuilder resourceBuilder = new ResourceBuilder(PROC);
		try (InputStream inputStream = SONG_XML.toURL().openStream()) {
			DOMResource preimageSong = (DOMResource) resourceBuilder.parseResource(
					songIri, inputStream, SONG_XML.toString(), ResourceBuilder.Parser.XML);
			songMapped = ResourceBuilder.mapWithXsltTracePackage(
					preimageSong, new File(XSL_DIR, "text-with-toc-html.xsl").toURI(), null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static RewriterConfig makeConfig(String xpath) {
		return new RewriterConfig(null, false, xpath, true, true);
	}

	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testForwardInImage() {
		normalizerConfig = makeConfig("path(.)");
		try {
			model = NormalizeAnnotation.rewrite(
					songMapped,
					songIri,
					songIriHtml,
					forwardFactory,
					normalizerConfig,
					SONG_FW_IN_IMAGE_JSON,
					Optional.of("jsonld"));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String sStackTrace = sw.toString(); // stack trace as a string
			System.err.println(sStackTrace);
			throw new RuntimeException(e);
		}
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG_HTML,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has rdf:value");
		assertEquals(
				"/html[1]/body[1]/div[2]/div[1]/p[2]/span[1]/text()[1]",
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"start selector has rdf:value");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has no extra class");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has rdf:value");
		assertEquals(
				"char=1",
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has rdf:value");
		assertEquals(
				"/html[1]/body[1]/div[2]/div[1]/p[2]/span[1]/text()[1]",
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has no extra class");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has rdf:value");
		assertEquals(
				"char=3",
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"end refinement has rdf:value");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has no extra class");
	}

	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testForwardInImageToParent() {
		normalizerConfig = makeConfig("sel:to-element(.)");
		model = NormalizeAnnotation.rewrite(
				songMapped,
				songIri,
				songIriHtml,
				forwardFactory,
				normalizerConfig,
				SONG_FW_IN_IMAGE_JSON,
				Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG_HTML,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has no extra class");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has rdf:value");
		assertEquals(
				"/html[1]/body[1]/div[2]/div[1]/p[2]/span[1]",
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"start selector has rdf:value");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has rdf:value");
		assertEquals(
				"char=1",
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has rdf:value");
		assertEquals(
				"/html[1]/body[1]/div[2]/div[1]/p[2]/span[1]",
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has no extra class");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has rdf:value");
		assertEquals(
				"char=3",
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"end refinement has rdf:value");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has no extra class");
	}

	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testForwardInImage2ToElement() {
		normalizerConfig = makeConfig("sel:to-element(.)");
		model = NormalizeAnnotation.rewrite(
				songMapped,
				songIri,
				songIriHtml,
				forwardFactory,
				normalizerConfig,
				SONG_FW_IN_IMAGE2_JSON,
				Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG_HTML,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has no extra class");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has rdf:value");
		assertEquals(
				"/html[1]/body[1]/div[2]/div[1]/p[1]",
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"start selector has rdf:value");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has rdf:value");
		assertEquals(
				"char=12",
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has rdf:value");
		assertEquals(
				"/html[1]/body[1]/div[2]/div[1]/p[1]",
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has no extra class");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has rdf:value");
		assertEquals(
				"char=22",
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"end refinement has rdf:value");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has no extra class");
	}

	// works, but not guarantied, since selector goes down to the text leaf
	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testBackwardToLeafInPreimage() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.rewrite(
				songMapped,
				songIriHtml,
				songIri,
				backwardFactory,
				normalizerConfig,
				SONG_BW_TO_LEAVE_IN_PREIMAGE_JSON,
				Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has rdf:value");
		assertEquals(
				"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}lem[1]/text()[1]",
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"start selector has rdf:value");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has no extra class");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has rdf:value");
		assertEquals(
				"char=1",
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has rdf:value");
		assertEquals(
				"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/Q{http://www.tei-c.org/ns/1.0}app[1]/Q{http://www.tei-c.org/ns/1.0}lem[1]/text()[1]",
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has no extra class");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has rdf:value");
		assertEquals(
				"char=3",
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"end refinement has rdf:value");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has no extra class");
	}

	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testBackwardToElementInPreimage() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.rewrite(
				songMapped,
				songIriHtml,
				songIri,
				backwardFactory,
				normalizerConfig,
				SONG_BW_TO_ELEMENT_IN_PREIMAGE_JSON,
				Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has rdf:value");
		assertEquals(
				"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/text()[2]",
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"start selector has rdf:value");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has no extra class");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has rdf:value");
		assertEquals(
				"char=7",
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has rdf:value");
		assertEquals(
				"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}l[2]/text()[2]",
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has no extra class");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has rdf:value");
		assertEquals(
				"char=11",
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"end refinement has rdf:value");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has no extra class");
	}

	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testBackwardToHigherInPreimage() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.rewrite(
				songMapped,
				songIriHtml,
				songIri,
				backwardFactory,
				normalizerConfig,
				SONG_BW_TO_HIGHER_IN_PREIMAGE_JSON,
				Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has rdf:value");
		assertEquals(
				"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/text()[1]",
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"start selector has rdf:value");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has no extra class");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has rdf:value");
		assertEquals(
				"char=7",
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has rdf:value");
		assertEquals(
				"/Q{http://www.tei-c.org/ns/1.0}TEI[1]/Q{http://www.tei-c.org/ns/1.0}text[1]/Q{http://www.tei-c.org/ns/1.0}body[1]/Q{http://www.tei-c.org/ns/1.0}lg[1]/Q{http://www.tei-c.org/ns/1.0}head[1]/text()[1]",
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString());
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has no extra class");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has rdf:value");
		assertEquals(
				"char=13",
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"end refinement has rdf:value");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has no extra class");
	}

	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testForwardNotInImage() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.rewrite(
				songMapped,
				songIri,
				songIriHtml,
				forwardFactory,
				normalizerConfig,
				SONG_FW_NOT_IN_IMAGE_JSON,
				Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG_HTML,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has extra class");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"start selector is a sel:Null selector");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"start refinement is a sel:Null selector");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has extra class");
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"end selector is a sel:Null selector");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has extra class");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"end refinement is a sel:Null selector");
	}

	@Disabled("could never be guarantied, since selector goes down to text leaf!")
	// '/html[1]/body[1]/div[1]/h1[1]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[1]' does not
	// select exactly one node in XdmValueResource: selects 0 nodes --- This is totally logical, since this text node
	// was never a trace:text node. And, we cannot securely use text() here, since the content could come from string
	// items in the image. Facit: No guaranty about selectors that go down to the text leaf.
	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testBackwardToLeafNotInPreimage() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.rewrite(
				songMapped,
				songIriHtml,
				songIri,
				backwardFactory,
				normalizerConfig,
				SONG_BW_TO_LEAVE_NOT_IN_PREIMAGE_JSON,
				Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has extra class");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"start selector is a sel:Null selector");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"start refinement is a sel:Null selector");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has extra class");
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"end selector is a sel:Null selector");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has extra class");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"end refinement is a sel:Null selector");
	}

	@Execution(ExecutionMode.SAME_THREAD)
	@Test
	public void testBackwardToElementNotInPreimage() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.rewrite(
				songMapped,
				songIriHtml,
				songIri,
				backwardFactory,
				normalizerConfig,
				SONG_BW_TO_ELEMENT_NOT_IN_PREIMAGE_JSON,
				Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResource = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResource, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				IRI_SONG,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
		Resource rangeSelector = model.listStatements(specificResource, OA.hasSelector, (Resource) null)
				.next()
				.getObject()
				.asResource();
		// start selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasStartSelector, (Resource) null)
						.toSet()
						.size(),
				"has a start selector");
		Resource startSelector = model.listStatements(rangeSelector, OA.hasStartSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(startSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start selector does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(startSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has extra class");
		assertEquals(
				1,
				model.listStatements(startSelector, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"start selector is a sel:Null selector");
		Resource startRefinement = model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(startRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(startRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"start refinement has extra class");
		assertEquals(
				1,
				model.listStatements(startRefinement, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"start refinement is a sel:Null selector");
		// end selector
		assertEquals(
				1,
				model.listStatements(rangeSelector, OA.hasEndSelector, (Resource) null)
						.toSet()
						.size(),
				"has a end selector");
		Resource endSelector = model.listStatements(rangeSelector, OA.hasEndSelector, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end selector does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has extra class");
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"end selector is a sel:Null selector");
		Resource endRefinement = model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
				.next()
				.getObject()
				.asResource();
		assertEquals(
				0,
				model.listStatements(endRefinement, RDF.value, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement does not have rdf:value");
		assertEquals(
				2,
				model.listStatements(endRefinement, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end refinement has extra class");
		assertEquals(
				1,
				model.listStatements(endRefinement, RDF.type, SEL.BlankedSelector)
						.toSet()
						.size(),
				"end refinement is a sel:Null selector");
	}
}
