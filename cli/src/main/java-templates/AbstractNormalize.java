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


abstract class AbstractNormalize {

    protected static final Processor PROC = new Processor();

    enum DOMParser {
	XML,
	HTML
    }

    enum Normalizer {
	FROM_ROOT_CLARK,
	FROM_DEEPEST_ID_CLARK
    }

    public class CliException extends Exception {
	public CliException(String msg) {
	    super(msg);
	}
	public CliException(Throwable cause) {
	    super(cause);
	}
	public CliException(String msg, Throwable cause) {
	    super(msg, cause);
	}
    }


    @Option(names = { "--parser" },
	    paramLabel = "PARSER",
	    description = "The parser used for reading the RESOURCE. Valid values: ${COMPLETION-CANDIDATES}. Defaults to ${DEFAULT-VALUE}")
    DOMParser parser = DOMParser.XML;

    @Option(names = { "--mode" },
	    paramLabel = "MODE",
	    description = "The algorithm for descending into the DOM tree in the first normalization step. Valid values: ${COMPLETION-CANDIDATES}. Defaults to ${DEFAULT-VALUE}")
    XPathNormalizer.Mode mode = XPathNormalizer.Mode.DEEP_NODE_STOP_AT_END;

    @Option(names = { "-n", "--normalizer" },
	    paramLabel = "NORMALIZER",
	    description = "The normalizer for the XPath part of the selector in the second normalization step. Valid values: ${COMPLETION-CANDIDATES}. Defaults to ${DEFAULT-VALUE}")
    Normalizer normalizer = Normalizer.FROM_DEEPEST_ID_CLARK;

    @Option(names = { "-x", "--normalizer-xpath" },
	    paramLabel = "NORMALIZER_XPATH",
	    description = "The normalizer for the XPath part of the selector in the second normalization step. This will override NORMALIZER.")
    String normalizerXPath = null;


    protected DOMResource parseResource(URI resource) throws CliException {
	// make relative paths absolute by resolving against the URI of the current working director
	URI resourceResolved;
	if (resource.isAbsolute()) {
	    resourceResolved = resource;
	} else {
	    try {
		URI currentDir = new URI("file:" + System.getProperty("user.dir") + "/");
		resourceResolved = currentDir.resolve(resource);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		throw new CliException(e);
	    }
	}
	// parse the resource
	if (parser.equals(DOMParser.XML)) {
	    try {
		return DOMResource.fromXML(resourceResolved, null, PROC);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		throw new CliException(e);
	    }
	} else if (parser.equals(DOMParser.HTML)) {
	    try {
		return DOMResource.fromHTML(resourceResolved, null, PROC);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		throw new CliException(e);
	    }
	} else {
	    System.err.printf("unknown parser %s\n", parser.toString());
	    throw new CliException("unknown parser " + parser.toString());
	}
    }

    protected XPathNormalizerWithXPath getXPathNormalizer() throws CliException {
	if (normalizerXPath != null) {
	    try {
		return new XPathNormalizerWithXPath(normalizerXPath);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		throw new CliException(e);
	    }
	} else if (normalizer.equals(Normalizer.FROM_DEEPEST_ID_CLARK)) {
	    try {
		return new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		throw new CliException(e);
	    }
	} else if (normalizer.equals(Normalizer.FROM_ROOT_CLARK)) {
	    try {
		return new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_ROOT_CLARK_XPATH);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		throw new CliException(e);
	    }
	} else {
	    System.err.printf("unknown normalizer %s\n", normalizer.name());
	    throw new CliException("unknown normalizer " + normalizer.name());
	}
    }

}
