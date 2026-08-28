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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class tests rewriting WADM annotations with HTML images.
 */
public class TestRewriteAnnotationTxt {

	private static final Processor PROC = new Processor();

	public static final String IRI_SONG = "https://docs.org/Gesang.tei.xml";
	public static final String IRI_SONG_IMAGE = "https://docs.org/Gesang.txt";

	public static final File TEST_DIR = Paths.get("..", "test").toFile();
	public static final File SAMPLE_DIR =
			Paths.get("src", "test", "resources", "samples").toFile();
	public static final File XSL_DIR =
			Paths.get("src", "test", "resources", "xsl").toFile();

	public static final String SONG_FW_IN_IMAGE_JSON = new File(SAMPLE_DIR, "gFwInImage.json").toString();
	public static final String SONG_BW_IN_PREIMAGE_JSON = new File(SAMPLE_DIR, "gBwTxtInPreimage.json").toString();

	public static final String SONG_FW_NOT_IN_IMAGE_JSON = new File(SAMPLE_DIR, "gFwNotInImage.json").toString();
	public static final String SONG_BW_NOT_IN_PREIMAGE_JSON =
			new File(SAMPLE_DIR, "gBwTxtNotInPreimage.json").toString();

	public static final URI SONG_XML = new File(TEST_DIR, "Gesang.tei.xml").toURI();

	private final RewriterFactory forwardFactory = new ForwardMappingFactory(PROC.newXPathCompiler());
	private final RewriterFactory backwardFactory = new BackwardMappingFactory(PROC.newXPathCompiler());
	private RewriterConfig normalizerConfig;

	private Model model;

	private de.wwu.scdh.annotation.selection.Resource<?> songImage;
	private URI songIri, songIriImage;

	@BeforeEach
	public void setupResource() throws URISyntaxException, ResourceException, MalformedURLException {
		songIri = new URI(IRI_SONG);
		songIriImage = new URI(IRI_SONG_IMAGE);
		ResourceBuilder resourceBuilder = new ResourceBuilder(PROC);
		try (InputStream inputStream = SONG_XML.toURL().openStream()) {
			DOMResource preimageSong = (DOMResource) resourceBuilder.parseResource(
					songIri, inputStream, SONG_XML.toString(), ResourceBuilder.Parser.XML);
			songImage = ResourceBuilder.mapWithXsltTracePackage(
					preimageSong, new File(XSL_DIR, "text-no-header.xsl").toURI(), null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static RewriterConfig forwardConfig(String xpath) {
		return new RewriterConfig(
				null, false, xpath, true, true, RewriterConfig.forwardDOMToTextPointClassMap(), false);
	}

	private static RewriterConfig backwardConfig(String xpath) {
		return new RewriterConfig(
				null, false, xpath, true, true, RewriterConfig.backwardDOMToTextPointClassMap(), false);
	}

	@Test
	public void testForwardInImage() {
		RewriterConfig config = new RewriterConfig(
				null, false, "path(.)", true, true, RewriterConfig.forwardDOMToTextPointClassMap(), false);
		;
		model = NormalizeAnnotation.rewrite(
				songImage, songIri, songIriImage, forwardFactory, config, SONG_FW_IN_IMAGE_JSON, Optional.of("jsonld"));
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
				IRI_SONG_IMAGE,
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
				"char=43",
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
				"start selector is a oa:FragmentSelector");
		assertEquals(
				OA.FragmentSelector,
				startSelector.getProperty(RDF.type).getObject().asResource());
		assertEquals(
				0,
				model.listStatements(startSelector, OA.refinedBy, (RDFNode) null)
						.toSet()
						.size(),
				"start selector has no refinement");
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
				"char=45",
				model.listStatements(endSelector, RDF.value, (RDFNode) null)
						.next()
						.getObject()
						.asLiteral()
						.getString(),
				"end selector has rdf:value");
		assertEquals(
				1,
				model.listStatements(endSelector, RDF.type, (RDFNode) null)
						.toSet()
						.size(),
				"end selector is a oa:FragmentSelector");
		assertEquals(
				OA.FragmentSelector,
				endSelector.getProperty(RDF.type).getObject().asResource());
		assertEquals(
				0,
				model.listStatements(endSelector, OA.refinedBy, (RDFNode) null)
						.toSet()
						.size(),
				"end selector has no refinement");
	}

	@Test
	public void testBackwardInPreimage() {
		RewriterConfig config = new RewriterConfig(
				null, false, "path(.)", true, true, RewriterConfig.backwardDOMToTextPointClassMap(), false);
		model = NormalizeAnnotation.rewrite(
				songImage,
				songIriImage,
				songIri,
				backwardFactory,
				config,
				SONG_BW_IN_PREIMAGE_JSON,
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

	@Test
	public void testForwardNotInImage() {
		RewriterConfig config = new RewriterConfig(
				null, false, "path(.)", true, true, RewriterConfig.forwardDOMToTextPointClassMap(), false);
		;
		model = NormalizeAnnotation.rewrite(
				songImage,
				songIri,
				songIriImage,
				forwardFactory,
				config,
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
				IRI_SONG_IMAGE,
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

	@Test
	public void testBackwardNotInPreimage() {
		RewriterConfig config = new RewriterConfig(
				null, false, "path(.)", true, true, RewriterConfig.backwardDOMToTextPointClassMap(), false);
		model = NormalizeAnnotation.rewrite(
				songImage,
				songIriImage,
				songIri,
				backwardFactory,
				config,
				SONG_BW_NOT_IN_PREIMAGE_JSON,
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
	}
}
