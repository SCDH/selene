package de.wwu.scdh.annotation.selection.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Optional;
import java.io.StringWriter;
import java.util.Collections;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

import net.sf.saxon.s9api.Processor;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFFormatVariant;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.JenaTitanium;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapZero;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.sparql.util.Context;
import com.apicatalog.jsonld.lang.Keywords;

import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.FramingApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;

import jakarta.json.JsonObject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;
import de.wwu.scdh.annotation.selection.XPathNormalizerWithXPath;
import de.wwu.scdh.annotation.selection.wadm.NormalizeAnnotation;


@Command(name = "normalize",
	 mixinStandardHelpOptions = true,
	 description = "normalize Web/Open Annotations selectors")
public class NormalizeWADM extends AbstractNormalize implements Callable<Integer> {

    @Parameters(paramLabel = "ANNOTATIONS",
		description = "The annotations given as URL or file path")
    URI selectors;

    @Option(names = { "-s", "--source" },
	    paramLabel = "SOURCE",
	    description = "The file all the ANNOTATIONS' selectors select from. This can be used to override oa:hasSource.")
    URI resource = null;

    @Option(names = { "-l", "--language" },
	    paramLabel = "LANG",
	    description = "The RDF language of the ANNOTATIONS. By default, this is guessed by the file extension.")
    String rdfLanguage = null;

    @Option(names = { "-f", "--format" },
	    paramLabel = "FORMAT",
	    description = "The output format. Defaults to ${DEFAULT-VALUE}")
    String format = "jsonld11";

    @Option(names = { "-v", "--variant" },
	    paramLabel = "VARIANT",
	    description = "The output format variant. Use 'framed' for getting JSON-LD 1.1 framing output.")
    String variant = null;

    @Option(names = { "--framing" },
	    paramLabel = "FRAMEMING-URI",
	    description = "Where to get the framing from. Defaults to ${DEFAULT-VALUE}")
    URL framingUri = NormalizeWADM.class.getResource("/wadm.jsonld");


    @Override
    public Integer call() throws Exception {

	Optional<DOMResource> dom = Optional.empty();
	if (resource != null) {
	    try {
		dom = Optional.of(parseResource(resource));
	    } catch (CliException e) {
		return 1;
	    }
	}

	XPathNormalizerWithXPath xpathNormalizer;
	try {
	    xpathNormalizer = getXPathNormalizer();
	} catch (CliException e) {
	    return 2;
	}

	// make relative paths absolute by resolving against the URI of the current working director
	URI selectorsResolved;
	if (selectors.isAbsolute()) {
	    selectorsResolved = selectors;
	} else {
	    try {
		URI currentDir = new URI("file:" + System.getProperty("user.dir") + "/");
		selectorsResolved = currentDir.resolve(selectors);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		return 1;
	    }
	}

	Optional<String> lang = Optional.empty();
	if (rdfLanguage != null) {
	    lang = Optional.of(rdfLanguage);
	}

	// do the normalization
	Model model;
	try {
	    model = NormalizeAnnotation.normalize(PROC, xpathNormalizer, selectorsResolved.toString(), lang, dom);
	} catch (Exception e) {
	    System.err.println(e.getMessage());
	    return 10;
	}
	Dataset ds = new DatasetImpl(model);


	if (format.equals("jsonld11") && variant.equals("framed")) {
	    // do the framing and serialization with Titanium

	    try {
		RdfDataset rdfds = JenaTitanium.convert(ds.asDatasetGraph());
		Document rdfdoc = RdfDocument.of(rdfds);
		// The Titanium API for framing does not allow
		// RdfDocuments.  Thus, we make a JsonDocument
		// representing the graph like for plain output
		JsonArray array = JsonLd.fromRdf(rdfdoc).get();
		JsonObject jsonStructure = Json.createObjectBuilder()
		    .add(Keywords.GRAPH, array)
		    .build();
		JsonDocument jsonDocument = JsonDocument.of(jsonStructure);
		// do the framing
		FramingApi api = JsonLd.frame(jsonDocument, JsonDocument.of(framingUri.openStream()));
		final JsonObject output = api.get();

		//JsonOutput.print(System.out, true);
		final JsonWriterFactory writerFactory = Json
		    .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
		final StringWriter stringWriter = new StringWriter();
		try (final JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
		    jsonWriter.write(output);
		}
		System.out.println(stringWriter.toString());
		return 0;
	    } catch (FileNotFoundException e) {
		System.err.println("Framing file not found: " + e.getMessage());
		return 4;
	    }
	}


	// try to make output format
	//
	// Note, that ntriples, nquads and others only work on the
	// try-catch block below!
	try {
	    WriterDatasetRIOT writer;
	    if (variant != null) {
		RDFFormatVariant outVariant = new RDFFormatVariant(variant);
		RDFFormat outFormat = new RDFFormat(RDFLanguages.nameToLang(format), outVariant);
		writer = RDFWriterRegistry.getWriterDatasetFactory(outFormat).create(outFormat);
	    } else {
		RDFFormat outFormat = new RDFFormat(RDFLanguages.nameToLang(format));
		writer = RDFWriterRegistry.getWriterDatasetFactory(outFormat).create(outFormat);
	    }
	    PrefixMap prefixMap = PrefixMapZero.empty;
	    Context ctx = new Context();
	    writer.write(System.out, ds.asDatasetGraph(), prefixMap, resource.toString(), ctx);
	    return 0; // done!
	} catch (Exception e) {
	    System.err.println(e.getMessage());
	}

	// try to use format language
	try {
	    RDFDataMgr.write(System.out, model, RDFLanguages.nameToLang(format));
	} catch (Exception err) {
	    System.err.println(err.getMessage());
	    return 10;
	}

	return 0;
    }

    public static void main(String... args) {
	System.exit(new CommandLine(new Normalize()).execute(args));
    }

}
