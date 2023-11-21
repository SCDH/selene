package de.wwu.scdh.annotation.selection.cli;

import java.io.File;
import java.net.URI;
import org.apache.commons.lang3.tuple.Pair;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

import net.sf.saxon.s9api.Processor;

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;
import de.wwu.scdh.annotation.selection.XPathNormalizerWithXPath;


@Command(name = "normalize", mixinStandardHelpOptions = true)
public class Normalize implements Callable<Integer> {

    private static final Processor PROC = new Processor();



    enum DOMParser {
	XML_PARSER,
	HTML_PARSER
    }

    enum Normalizer {
	FROM_ROOT_CLARK,
	FROM_DEEPEST_ID_CLARK
    }

    public static final String DEFAULT_NORMALIZER = Normalizer.FROM_DEEPEST_ID_CLARK.name();

    @Parameters(paramLabel = "RESOURCE",
		description = "The file the selector selects from")
    URI resource;

    @Option(names = { "-p", "--parser" },
	    paramLabel = "PARSER",
	    defaultValue = "${DOM_PARSER:DOMParser.XML_PARSER}",
	    description = "The parser used for reading the RESOURCE. Valid values: ${COMPLETION-CANDIDATES}")
    DOMParser parser = DOMParser.XML_PARSER;

    @Option(names = { "-x", "--xpath" },
	    paramLabel = "XPATH",
	    description = "the XPath value of an XPath selector")
    String xpath;

    @Option(names = { "-c", "--character" },
	    paramLabel = "CHAR",
	    description = "the character-scheme position the XPath selector is refined with")
    int character;

    @Option(names = { "-n", "--normalizer" },
	    paramLabel = "NORMALIZER",
	    defaultValue = "${NORMALIZER:DEFAULT_NORMALIZER}",
	    description = "The normalizer for XPath part of the selector. Valid values: ${COMPLETION-CANDIDATES}")
    Normalizer normalizer = Normalizer.FROM_DEEPEST_ID_CLARK;

    @Override
    public Integer call() throws Exception {
	DOMResource dom;
	if (parser.equals(DOMParser.XML_PARSER)) {
	    dom = DOMResource.fromXML(resource, null, PROC);
	} else if (parser.equals(DOMParser.HTML_PARSER)) {
	    dom = DOMResource.fromHTML(resource, null, PROC);
	} else {
	    System.err.printf("unknown parser %s\n", parser.toString());
	    return 1;
	}
	System.err.printf("parsed %s\n", resource.toString());

	System.err.printf("normalizing %s refined by char=%s\n", xpath, character);

	XPathNormalizerWithXPath xpathNormalizer;
	if (normalizer.equals(Normalizer.FROM_ROOT_CLARK)) {
	    xpathNormalizer = new XPathNormalizerWithXPath(dom, XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	} else if (normalizer.equals(Normalizer.FROM_ROOT_CLARK)) {
	    xpathNormalizer = new XPathNormalizerWithXPath(dom, XPathNormalizerWithXPath.FROM_ROOT_CLARK_XPATH);
	} else {
	    System.err.printf("unknown normalizer %s\n", normalizer.name());
	    return 2;
	}
	Pair<String, Integer> normalized = xpathNormalizer.normalizeXPathRefinedByCharScheme(xpath, character);
	System.out.printf("%s,%s\n", normalized.getLeft(), normalized.getRight());
	return 0;
    }

    public static void main(String... args) {
	System.exit(new CommandLine(new Normalize()).execute(args));
    }

}
