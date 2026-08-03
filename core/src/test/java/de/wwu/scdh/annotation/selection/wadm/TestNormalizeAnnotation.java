package de.wwu.scdh.annotation.selection.wadm;

import static org.junit.jupiter.api.Assertions.*;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.ResourceBuilder;
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

	public static final String IRI = "https://docs.org/scdh.span.42.html";
	public static final String REWRITE_IRI = "https://docs.org/scdh.span.42.xhtml";

	public static final File TEST_DIR = Paths.get("..", "test").toFile();
	public static final File SAMPLE_DIR =
			Paths.get("src", "test", "resources", "samples").toFile();

	public static final String P1_1_JSON = new File(SAMPLE_DIR, "p1.1.json").toString();

	public static final URI SPAN_HTML = new File(SAMPLE_DIR, "scdh.span.42.html").toURI();

	private final RewriterFactory rewriterFactory = new NormalizerFactory();
	private RewriterConfig normalizerConfig;

	private Model model;

	private de.wwu.scdh.annotation.selection.Resource<?> resource;

	private URI iri, rewriteIri;

	@BeforeEach
	public void setupResource() throws URISyntaxException, ResourceException, MalformedURLException {
		iri = new URI(IRI);
		rewriteIri = new URI(REWRITE_IRI);
		ResourceBuilder resourceBuilder = new ResourceBuilder(new Processor());
		try (InputStream inputStream = SPAN_HTML.toURL().openStream()) {
			resource =
					resourceBuilder.parseResource(iri, inputStream, SPAN_HTML.toString(), ResourceBuilder.Parser.HTML);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static RewriterConfig makeConfig(String xpath) {
		return new RewriterConfig(null, false, xpath, false);
	}

	// normalizing XPathSelector refinedBy FragmentSelector conforming to RFC5147 character scheme

	@Test
	public void testAcceptP11WithPath() {
		normalizerConfig = makeConfig("path(.)");
		model = NormalizeAnnotation.normalize(
				resource, iri, rewriterFactory, normalizerConfig, P1_1_JSON, Optional.of("jsonld"));
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
				resource, iri, rewriterFactory, normalizerConfig, P1_1_JSON, Optional.of("jsonld"));
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
				resource, iri, rewriterFactory, normalizerConfig, P1_1_JSON, Optional.of("jsonld"));
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
				resource, iri, rewriteIri, rewriterFactory, normalizerConfig, P1_1_JSON, Optional.of("jsonld"));
		assertEquals(
				1,
				model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
						.toSet()
						.size());
		Resource specificResouce = model.listStatements((Resource) null, OA.hasTarget, (RDFNode) null)
				.next()
				.getResource();
		assertEquals(
				1,
				model.listStatements(specificResouce, OA.hasSource, (RDFNode) null)
						.toSet()
						.size());
		assertEquals(
				REWRITE_IRI,
				specificResouce
						.getProperty(OA.hasSource)
						.getObject()
						.asResource()
						.toString());
	}
}
