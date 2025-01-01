package de.wwu.scdh.annotation.selection.wadm;

import java.util.Optional;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Paths;

import net.sf.saxon.s9api.Processor;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.rdf.model.Resource;


import de.wwu.scdh.annotation.selection.XPathNormalizer;
import de.wwu.scdh.annotation.selection.XPathNormalizerWithXPath;
import de.wwu.scdh.annotation.selection.RewriterConfig;

/**
 * This class is used to test all the wadm normalizers.
 */
public class TestNormalizeAnnotation {


    public static final File TEST_DIR = Paths.get("..", "test").toFile();
    public static final File SAMPLE_DIR = Paths.get("src", "test", "resources", "samples").toFile();

    public static final String P1_1_JSON = new File(SAMPLE_DIR, "p1.1.json").toString();

    private XPathNormalizer normalizer;
    private RewriterConfig normalizerConfig = new RewriterConfig(null, false);

    private Processor processor = new Processor();

    private Model model;


    // normalizing XPathSelector refinedBy FragmentSelector conforming to RFC5147 character scheme

    @Test
    public void testAcceptP11WithPath() {
	normalizer = new XPathNormalizerWithXPath("path(.)");
	model = NormalizeAnnotation.normalize(processor, normalizer, normalizerConfig, P1_1_JSON, Optional.of("jsonld"), Optional.empty());
	assertEquals(21, model.size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "/html[1]/body[1]/p[1]/text()[1]").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "char=3").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "char=2").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}p[1]/text()[1]").toSet().size());
    }

    @Test
    public void testAcceptP11WithPathParent() {
	normalizer = new XPathNormalizerWithXPath("path(parent::*)");
	model = NormalizeAnnotation.normalize(processor, normalizer, normalizerConfig, P1_1_JSON, Optional.of("jsonld"), Optional.empty());
	assertEquals(21, model.size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "/html[1]/body[1]/p[1]/text()[1]").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "char=3").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "char=2").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]/Q{http://www.w3.org/1999/xhtml}p[1]").toSet().size());
    }

    @Test
    public void testAcceptP11WithPathParentParent() {
	normalizer = new XPathNormalizerWithXPath("path(parent::*/parent::*)");
	model = NormalizeAnnotation.normalize(processor, normalizer, normalizerConfig, P1_1_JSON, Optional.of("jsonld"), Optional.empty());
	assertEquals(21, model.size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "/html[1]/body[1]/p[1]/text()[1]").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "char=3").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "char=33").toSet().size());
	assertEquals(1, model.listStatements((Resource) null, RDF.value, "/Q{http://www.w3.org/1999/xhtml}html[1]/Q{http://www.w3.org/1999/xhtml}body[1]").toSet().size());
    }

}
