package de.wwu.scdh.annotation.selection.cli;

import java.io.File;
import java.net.URI;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Optional;

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

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;
import de.wwu.scdh.annotation.selection.XPathNormalizerWithXPath;
import de.wwu.scdh.annotation.selection.wadm.NormalizeAnnotation;


@Command(name = "normalize",
	 mixinStandardHelpOptions = true,
	 description = "normalize an Open Annotations selector")
public class NormalizeWADM extends AbstractNormalize implements Callable<Integer> {

    @Parameters(paramLabel = "SELECTORS",
		description = "The selectors given as URL or file path")
    URI selectors;

    @Option(names = { "-s", "--source" },
	    paramLabel = "SOURCE",
	    description = "The file all selectors selects from, overriding oa:hasSource")
    URI resource = null;

    @Option(names = { "-l", "--language" },
	    paramLabel = "LANG",
	    description = "The RDF language of the SELECTORS. By default, this is guessed by the file extension.")
    String rdfLanguage = null;

    @Option(names = { "-f", "--format" },
	    paramLabel = "FORMAT",
	    description = "The output format. Defaults to jsonld")
    String format = "jsonld";

    @Option(names = { "-v", "--variant" },
	    paramLabel = "VARIANT",
	    description = "The output format variant.")
    String variant = null;


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

	// try to make output format
	//
	// Note, that ntriples, nquads and others only work on the
	// try-catch block below!
	try {
	    RDFFormat outFormat;
	    if (variant != null) {
		RDFFormatVariant outVariant = new RDFFormatVariant(variant);
		outFormat = new RDFFormat(RDFLanguages.nameToLang(format), outVariant);
	    } else {
		outFormat = new RDFFormat(RDFLanguages.nameToLang(format));
	    }
	    RDFDataMgr.write(System.out, model, outFormat);
	    return 0; // done!
	} catch (Exception e) {
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
