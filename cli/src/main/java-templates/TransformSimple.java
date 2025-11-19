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

import de.wwu.scdh.annotation.selection.resource.DOMResource;
import de.wwu.scdh.annotation.selection.rewriter.XPathNormalizer;
import de.wwu.scdh.annotation.selection.rewriter.XPathNormalizerWithXPath;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import de.wwu.scdh.annotation.selection.RewriterConfig;


@Command(name = "transforms",
	 mixinStandardHelpOptions = true,
	 description = "transforms a *s*imple pair of XPath selector and RFC5147 character scheme selector along with a XSL transformation")
public class TransformSimple extends AbstractNormalize implements Callable<Integer> {


    @Parameters(paramLabel = "RESOURCE",
		description = "The file (preimage) the selector selects from")
    URI resource;

    @Option(names = { "-p", "--xpath" },
	    required = true,
	    paramLabel = "XPATH",
	    description = "the XPath value of an XPath selector")
    String xpath;

    @Option(names = { "-c", "--character" },
	    required = true,
	    paramLabel = "POSITION",
	    description = "the position the XPath selector is refined with using the character scheme of RFC5147")
    int character;

    @Option(names = { "--xsl:" },
	    required = true,
	    paramLabel = "FILE",
	    description = "the XSLT stylesheet")
    URI xsl;

    @Option(names = { "-s", "--config" },
	    required = true,
	    paramLabel = "FILE",
	    description = "the Saxon configuration file for the XSL  transformations")
    URI saxonConfig;



    @Override
    public Integer call() throws Exception {
	// make relative paths absolute by resolving against the URI of the current working director
	DOMResource dom = parseResource(resource);
	System.err.printf("parsed %s\n", resource.toString());

	URI saxonConfigResolved = resolveInCurrDir(saxonConfig);
	URI xslResolved = resolveInCurrDir(xsl);


	System.err.printf("normalizing %s refined by char=%s\n", xpath, character);

	XPathNormalizerWithXPath xpathNormalizer;
	if (normalizer.equals(Normalizer.FROM_DEEPEST_ID_CLARK)) {
	    try {
		xpathNormalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_DEEPEST_ID_CLARK_XPATH);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		return 2;
	    }
	} else if (normalizer.equals(Normalizer.FROM_ROOT_CLARK)) {
	    try {
		xpathNormalizer = new XPathNormalizerWithXPath(XPathNormalizerWithXPath.FROM_ROOT_CLARK_XPATH);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		return 2;
	    }
	} else {
	    System.err.printf("unknown normalizer %s\n", normalizer.name());
	    return 2;
	}
	try {
	    XPathRefinedByRFC5147CharScheme input, normalized;
	    input = new XPathRefinedByRFC5147CharScheme(xpath, character);
	    normalized = xpathNormalizer.rewrite(dom, input, getRewriterConfig()).get(0);
	    System.out.printf("%s,%s\n", normalized.getXPath(), normalized.getChar());
	} catch (Exception e) {
	    System.err.println(e.getMessage());
	    return 3;
	}
	return 0;
    }

    public static void main(String... args) {
	System.exit(new CommandLine(new Normalize()).execute(args));
    }

}
