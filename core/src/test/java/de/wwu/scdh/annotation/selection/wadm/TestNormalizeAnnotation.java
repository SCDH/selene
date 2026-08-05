package de.wwu.scdh.annotation.selection.wadm;

import static org.junit.jupiter.api.Assertions.*;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.DOMResource;
import de.wwu.scdh.annotation.selection.resource.ResourceBuilder;
import de.wwu.scdh.annotation.selection.rewriter.ForwardMappingFactory;
import de.wwu.scdh.annotation.selection.rewriter.NormalizerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;
import net.sf.saxon.s9api.Processor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.*;

/**
 * This class is used to test all the wadm normalizers.
 */
public class TestNormalizeAnnotation {

	private static final Processor PROC = new Processor();

	public static final String IRI_42 = "https://docs.org/scdh.span.42.html";
	public static final String REWRITE_IRI_42 = "https://docs.org/scdh.span.42.xhtml";

	public static final String IRI_GESANG = "https://docs.org/Gesang.tei.xml";
	public static final String REWRITE_IRI_GESANG = "https://docs.org/Gesang.html";

	public static final File TEST_DIR = Paths.get("..", "test").toFile();
	public static final File SAMPLE_DIR =
			Paths.get("src", "test", "resources", "samples").toFile();

	public static final File XSL_DIR =
			Paths.get("src", "test", "resources", "xsl").toFile();

	public static final String P1_1_JSON = new File(SAMPLE_DIR, "p1.1.json").toString();
	public static final String GESANG_NOT_IN_IMAGE_JSON = new File(SAMPLE_DIR, "gNotInImage.json").toString();

	public static final URI SPAN_HTML = new File(SAMPLE_DIR, "scdh.span.42.html").toURI();
	public static final URI GESANG_XML = new File(TEST_DIR, "Gesang.tei.xml").toURI();

	private final RewriterFactory normalizerFactory = new NormalizerFactory(PROC.newXPathCompiler());
	private final RewriterFactory forwardFactory = new ForwardMappingFactory(PROC.newXPathCompiler());
	private RewriterConfig normalizerConfig;

	private Model model;

	private de.wwu.scdh.annotation.selection.Resource<?> resource42, resourceSong;
	private URI iri42, rewriteIri42;
	private URI iriGesang, rewriteIriGesang;

	@BeforeEach
	public void setupResource() throws URISyntaxException, ResourceException, MalformedURLException {
		iri42 = new URI(IRI_42);
		rewriteIri42 = new URI(REWRITE_IRI_42);
		ResourceBuilder resourceBuilder = new ResourceBuilder(PROC);
		try (InputStream inputStream = SPAN_HTML.toURL().openStream()) {
			resource42 = resourceBuilder.parseResource(
					iri42, inputStream, SPAN_HTML.toString(), ResourceBuilder.Parser.HTML);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		iriGesang = new URI(IRI_GESANG);
		rewriteIriGesang = new URI(REWRITE_IRI_GESANG);
		resourceBuilder = new ResourceBuilder(PROC);
		try (InputStream inputStream = GESANG_XML.toURL().openStream()) {
			DOMResource preimageSong = (DOMResource) resourceBuilder.parseResource(
					iriGesang, inputStream, GESANG_XML.toString(), ResourceBuilder.Parser.XML);
			resourceSong = ResourceBuilder.mapWithXsltTracePackage(
					preimageSong, new File(XSL_DIR, "text-with-toc-html.xsl").toURI(), null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static RewriterConfig makeConfig(String xpath) {
		return new RewriterConfig(null, false, xpath, false, true);
	}

	// normalizing XPathSelector refinedBy FragmentSelector conforming to RFC5147 character scheme

	@Test
	public void testAcceptP11WithPath() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.normalize(
				resource42, iri42, normalizerFactory, normalizerConfig, P1_1_JSON, Optional.of("jsonld"));
		assertEquals(21, model.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "/html[1]/body[1]/p[1]/text()[1]")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "char=3")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "char=2")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements(
								(Resource) null,
								RDF.value,
								"/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}p[1]/text()[1]")
						.toSet()
						.size());
	}

	@Test
	public void testAcceptP11WithPathParent() {
		normalizerConfig = makeConfig("path(parent::*)");
		model = NormalizeAnnotation.normalize(
				resource42, iri42, normalizerFactory, normalizerConfig, P1_1_JSON, Optional.of("jsonld"));
		assertEquals(21, model.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "/html[1]/body[1]/p[1]/text()[1]")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "char=3")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "char=2")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements(
								(Resource) null,
								RDF.value,
								"/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}p[1]")
						.toSet()
						.size());
	}

	@Test
	public void testAcceptP11WithPathParentParent() {
		normalizerConfig = makeConfig("path(parent::*/parent::*)");
		model = NormalizeAnnotation.normalize(
				resource42, iri42, normalizerFactory, normalizerConfig, P1_1_JSON, Optional.of("jsonld"));
		assertEquals(21, model.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "/html[1]/body[1]/p[1]/text()[1]")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "char=3")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements((Resource) null, RDF.value, "char=33")
						.toSet()
						.size());
		assertEquals(
				1,
				model.listStatements(
								(Resource) null,
								RDF.value,
								"/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]")
						.toSet()
						.size());
	}

	@Test
	public void testRewriteP11WithPath() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.rewrite(
				resource42, iri42, rewriteIri42, normalizerFactory, normalizerConfig, P1_1_JSON, Optional.of("jsonld"));
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
				REWRITE_IRI_42,
				specificResource
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
	}

	@Test
	public void testRewriteForwardNotInImage() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.rewrite(
				resourceSong,
				iriGesang,
				rewriteIriGesang,
				forwardFactory,
				normalizerConfig,
				GESANG_NOT_IN_IMAGE_JSON,
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
				REWRITE_IRI_GESANG,
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
