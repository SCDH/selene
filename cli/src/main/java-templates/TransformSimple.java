package de.wwu.scdh.annotation.selection.cli;

import java.io.File;
import java.net.URI;
import org.apache.commons.lang3.tuple.Pair;
import javax.xml.transform.stream.StreamSource;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;

import net.sf.saxon.s9api.Processor;

import de.wwu.scdh.annotation.selection.Resource;
import de.wwu.scdh.annotation.selection.resource.MappedDOMResource;
import de.wwu.scdh.annotation.selection.resource.ResourceBuilder;
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

    @Option(names = { "--xsl" },
	    required = true,
	    paramLabel = "FILE",
	    description = "the XSLT stylesheet")
    URI xsl;

    @Option(names = { "-s", "--config" },
	    required = false,
	    paramLabel = "FILE",
	    description = "the Saxon configuration file for the XSL  transformations")
    URI saxonConfig;



    @Override
    public Integer call() throws Exception {

	// construct a Saxon Processor based on the config file
	Processor processor;
	if (saxonConfig == null) {
		processor = new Processor();
	} else {
	    URI saxonConfigResolved = resolveInCurrDir(saxonConfig);
	    StreamSource configStream = new StreamSource(saxonConfigResolved.toURL().openStream());
	    processor = new Processor(configStream);
	}

	// build the resource
	ResourceBuilder resourceBuilder = new ResourceBuilder(processor);
	Resource<?> parsed = resourceBuilder.parseResource(resolveInCurrDir(resource), parser);
	DOMResource dom = null;
	if (!(parsed instanceof DOMResource)) {
	    System.err.printf("resources cannot be mapped with XSLT");
	    return 2;
	}
	dom = (DOMResource) parsed;
	System.err.printf("parsed %s\n", resource.toString());

	// derive the image
	URI xslResolved = resolveInCurrDir(xsl);
	MappedDOMResource preimage = ResourceBuilder.mapWithXsltTracePackage(dom, xslResolved);
	System.err.printf("transformed with %s\n", xsl.toString());
	//System.err.printf(preimage.getImage().getContents().toString());

	System.exit(0);


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
