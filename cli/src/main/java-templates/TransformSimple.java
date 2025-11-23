package de.wwu.scdh.annotation.selection.cli;

import java.io.File;
import java.net.URI;
import org.apache.commons.lang3.tuple.Pair;
import javax.xml.transform.stream.StreamSource;
import java.util.List;
import java.util.Iterator;

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
import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;
import de.wwu.scdh.annotation.selection.RewriterConfig;
import de.wwu.scdh.annotation.selection.RewriterFactory;
import de.wwu.scdh.annotation.selection.rewriter.ForwardMappingFactory;
import de.wwu.scdh.annotation.selection.rewriter.BackwardMappingFactory;
import de.wwu.scdh.annotation.selection.Rewriter;
import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.Component;
import de.wwu.scdh.annotation.selection.component.XPathComponent;
import de.wwu.scdh.annotation.selection.component.RFC5147CharComponent;
import de.wwu.scdh.annotation.selection.NoSuchComponentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Command(name = "transforms",
	 mixinStandardHelpOptions = true,
	 description = "transforms a *s*imple pair of XPath selector and RFC5147 character scheme selector along with a XSL transformation")
public class TransformSimple extends AbstractNormalize implements Callable<Integer> {

    private static Logger log = LoggerFactory.getLogger(TransformSimple.class);

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

    @Option(names = { "-b", "--backward" },
	    required = false,
	    description = "Switch for transforming the pointer back.")
    boolean backward = false;

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
	    log.error("resources cannot be mapped with XSLT");
	    return 2;
	}
	dom = (DOMResource) parsed;
	log.info("parsed {}", resource.toString());

	// derive the image
	URI xslResolved = resolveInCurrDir(xsl);
	MappedDOMResource preimage = ResourceBuilder.mapWithXsltTracePackage(dom, xslResolved, null);
	log.info("transformed with {}", xsl.toString());
	//System.err.printf(preimage.getImage().getContents().toString());

	// make the point from the given cli arguments
	Point point;
	if (backward && RFC5147CharScheme.class.isAssignableFrom(preimage.getImage().getPointClass())) {
	    point = new RFC5147CharScheme(character);
	} else {
	    point = new XPathRefinedByRFC5147CharScheme(xpath, character);
	}
	log.info("mapping {}, {}", point.getClass().getName(), TransformSimple.pointToString(point));

	// get the rewriter
	RewriterFactory factory;
	Rewriter rewriter;
	if (backward) {
	    factory = new BackwardMappingFactory();
	    rewriter = factory.getRewriter
		(preimage.getImage().getPointClass(),
		 XPathRefinedByRFC5147CharScheme.class,
		 getRewriterConfig());
	} else {
	    factory = new ForwardMappingFactory();
	    rewriter = factory.getRewriter
		(XPathRefinedByRFC5147CharScheme.class,
		 preimage.getImage().getPointClass(),
		 getRewriterConfig());
	}
	log.info("rewriter: {}", rewriter.getClass());

	log.info("config xpath {}", getRewriterConfig().getXPath());
	try {
	    List<Point> mapped = rewriter.rewrite(preimage, point, getRewriterConfig());
	    log.info("mapped to {} points", mapped.size());
	    for (Point p : mapped) {
		System.out.println(TransformSimple.pointToString(p));
	    }
	} catch (Exception e) {
	    log.error(e.getMessage());
	    return 3;
	}
	return 0;
    }

    public static <P extends Point> String pointToString(P point) {
	String rc = "";
	try {
	    if (point instanceof XPathRefinedByRFC5147CharScheme) {
		rc += String.valueOf(point.getComponent(XPathComponent.class));
		rc += ";char=";
		rc += String.valueOf(point.getComponent(RFC5147CharComponent.class));
	    } else if (point instanceof RFC5147CharScheme) {
		rc += "char=";
		rc += String.valueOf(point.getComponent(RFC5147CharComponent.class));
	    }
	} catch (NoSuchComponentException e) {
	    log.error(e.getMessage());
	}
	return rc;
    }
	    

    public static void main(String... args) {
	System.exit(new CommandLine(new Normalize()).execute(args));
    }

}
