package de.wwu.scdh.annotation.selection.resource;

import java.net.URI;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltPackage;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.QName;

import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import de.wwu.scdh.annotation.selection.Resource;
import de.wwu.scdh.annotation.selection.ResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ResourceBuilder} makes {@link Resource} from a URI.
 */
public class ResourceBuilder {

    private static Logger log = LoggerFactory.getLogger(ResourceBuilder.class);

    /**
     * There are some resource parsers available, that this class
     * offers building resources with.
     *
     */
    public enum Parser {
	XML,
	HTML
    }

    public static final String MODIFY_CONFIG_XSL = "/xslt/modify-config.xsl";

    public static final String TRACE_XSL = "/xslt/libtrace-xml.xsl";

    public static final String TRACE_PKG_NAME = "http://wwu.de/scdh/selection-engine/node-tracing";

    public static final String TRACE_PKG_PARAM = "trace-location";

    private Processor processor;

    /**
     * Makes a new {@link ResourceBuilder} instance.
     *
     */
    public ResourceBuilder(Processor processor) {
	this.processor = processor;
    }

    /**
     * Parses the contents of a given URI with a given {@link Parser}.
     */
    public Resource<?> parseResource(URI resource, Parser parser) throws ResourceException {
	// parse the resource
	if (parser.equals(Parser.XML)) {
	    try {
		return DOMResource.fromXML(resource, null, processor);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		throw new ResourceException(e);
	    }
	} else if (parser.equals(Parser.HTML)) {
	    try {
		return DOMResource.fromHTML(resource, null, processor);
	    } catch (Exception e) {
		System.err.println(e.getMessage());
		throw new ResourceException(e);
	    }
	} else {
	    System.err.printf("unknown parser %s\n", parser.toString());
	    throw new ResourceException("unknown parser " + parser.toString());
	}
    }

    /**
     * Derives a {@link MappedDOMResource} from a preimage and an XSLT
     * stylesheet.<P>
     *
     * Note: When a saxon config file with XSLT packages is used (by
     * reading it when constructing the {@link Processor}), only
     * compiling the right XSLT tracing package is not
     * enough. Transforming the config file and changing the
     * <code>resourceLocation</code> is required.<P>
     *
     * {@link ResourceException.processorFromModifiedConfig(Source)}
     * has a solution.
     *
     * @param preimage - the resource deriving from
     * @param stylesheet - {@link URI} of the XSLT stylesheet used for deriving
     * @param traceStream - an input stream made of the the XSLT package for adding the mapping information
     * @param traceSystemId - the path of the XSLT package for adding the mapping information
     * @param imagePointClass - the class of the point used for the image, or <code>null</code> for concluding from the stylesheet's default output method
     */
    public static MappedDOMResource mapWithXsltTracePackage(DOMResource preimage, URI stylesheet, InputStream traceStream, String traceSystemId, Class<? extends Point> imagePointClass)
	throws ResourceException {
	// load the XSLT stylesheet
	StreamSource xsl;
	try {
	    xsl = new StreamSource(stylesheet.toURL().openStream(), stylesheet.toString());
	} catch (MalformedURLException e) {
	    throw new ResourceException(e.getMessage());
	} catch (IOException e) {
	    throw new ResourceException(e.getMessage());
	}

	StreamSource traceSource = new StreamSource(traceStream, traceSystemId);

	try {
	    // transform preimage to image
	    Processor proc = preimage.getProcessor();
	    XsltCompiler compiler = proc.newXsltCompiler();
	    XsltPackage tracePackage = compiler.compilePackage(traceSource);
	    compiler.importPackage(tracePackage, TRACE_PKG_NAME, "1.0.0");
	    proc = compiler.getProcessor();
	    log.debug("trace package URI: {}", ResourceBuilder.class.getResource(TRACE_XSL));
	    log.debug("trace package imported");
	    XsltExecutable executable = compiler.compile(xsl);
	    Xslt30Transformer transformer = executable.load30();


	    Class<? extends Point> pointClass;
	    if (imagePointClass != null) {
		pointClass = imagePointClass;
	    } else {
		pointClass = ResourceBuilder.pointerClassFromOutputMethod(transformer);
	    }

	    // transform to XdmValue, which keeps the nodes from the source
	    XdmValue imageValue = transformer.applyTemplates(preimage.getContents());
	    XdmValueResource image = new XdmValueResource(null, imageValue, proc, pointClass);

	    // make mapped resource from preimage and image
	    MappedDOMResource mappedDOMResource = new MappedDOMResource(preimage);
	    mappedDOMResource.setImage(image);
	    return mappedDOMResource;
	} catch (SaxonApiException e) {
	    throw new ResourceException(e.getMessage());
	}
    }


    /**
     * Makes a {@link MappedDOMResource} from a preimage and an XSLT
     * stylesheet.
     *
     * @param preimage - the resource deriving from
     * @param stylesheet - {@link URI} of the XSLT stylesheet used for deriving
     * @param tracePkg - {@link URI} of the the XSLT package for adding the mapping information
     * @param imagePointClass - the class of the point used for the image, or <code>null</code> for concluding from the stylesheet's default output method
     */
    public static MappedDOMResource mapWithXsltTracePackage(DOMResource preimage, URI stylesheet, URI tracePkg, Class<? extends Point> imagePointClass)
	throws ResourceException {
	// load the trace XSLT package
	try {
	    InputStream traceStream = tracePkg.toURL().openStream();
	    return ResourceBuilder.mapWithXsltTracePackage(preimage, stylesheet, traceStream, tracePkg.toString(), imagePointClass);
	} catch (MalformedURLException e) {
	    throw new ResourceException(e.getMessage());
	} catch (IOException e) {
	    throw new ResourceException(e.getMessage());
	}
    }

    /**
     * Makes a {@link MappedDOMResource} from a preimage and an XSLT
     * stylesheet.
     *
     * This method uses the internal XSLT package for adding mapping information.
     *
     * @param preimage - the resource deriving from
     * @param stylesheet - {@link URI} of the XSLT stylesheet used for deriving
     * @param imagePointClass - the class of the point used for the image, or <code>null</code> for concluding from the stylesheet's default output method
     */
    public static MappedDOMResource mapWithXsltTracePackage(DOMResource preimage, URI stylesheet, Class<? extends Point> imagePointClass)
	throws ResourceException {
	return ResourceBuilder.mapWithXsltTracePackage(preimage, stylesheet, ResourceBuilder.class.getResourceAsStream(TRACE_XSL), TRACE_XSL, imagePointClass);
    }


    /**
     * Returns the point class for the output method, i.e. the
     * serialization method. In XSLT, the output method is set by
     * <code>xsl:output/@method</code>.
     */
    public static Class<? extends Point> pointerClassFromOutputMethod(String method) throws ResourceException {
	switch (method) {
	case "text":
	    return RFC5147CharScheme.class;
	case "xhtml":
	    return XPathRefinedByRFC5147CharScheme.class;
	case "xml":
	    return XPathRefinedByRFC5147CharScheme.class;
	default:
	    log.error("no point for output method {}", method);
	    throw new ResourceException("unsupported output method" + method);
	}
    }

    /**
     * 	Gets the default output method from the stylesheet and returns
     * 	the point class for it.  The output method is set by
     * 	<code>xsl:output/@method</code> or is <code>xml</code> if this
     * 	declaration is missing.
     */
    public static Class<? extends Point> pointerClassFromOutputMethod(Xslt30Transformer transformer) throws ResourceException {
	String outputMethod = transformer.newSerializer().getOutputProperty(Serializer.Property.METHOD);
	// method may be null, when the stylesheet does not declare xsl:output/@method
	if (outputMethod == null) {
	    return ResourceBuilder.pointerClassFromOutputMethod("xml");
	} else {
	    return ResourceBuilder.pointerClassFromOutputMethod(outputMethod);
	}
    }

    /**
     * Returns a {@link Processor} made with the Saxon config given in
     * as the second argument after modifying it following the
     * internal requirements for the tracing package.
     *
     * @param config - a saxon config file as {@link Source}
     */
    public static Processor processorFromModifiedConfig(Source config)
	throws ResourceException {
	try {
	    Processor processor = new Processor();
	    XsltCompiler compiler = processor.newXsltCompiler();
	    compiler.setParameter(new QName(TRACE_PKG_PARAM), XdmValue.makeValue(ResourceBuilder.class.getResource(TRACE_XSL).toURI()));
	    StreamSource source = new StreamSource(ResourceBuilder.class.getResourceAsStream(MODIFY_CONFIG_XSL));
	    XsltExecutable executable = compiler.compile(source);
	    Xslt30Transformer transformer = executable.load30();
	    // transform the config file and make the processor

	    // approach plug byte array streams
	    ByteArrayOutputStream modifiedStream = new ByteArrayOutputStream();
	    Serializer serializer = transformer.newSerializer(modifiedStream);
	    transformer.transform(config, serializer);
	    log.debug("modified Saxon config file: {}", modifiedStream.toByteArray());
	    InputStream modified = new ByteArrayInputStream(modifiedStream.toByteArray());
	    return new Processor(new StreamSource(modified));

	    // // piped stream approach: java.io.IOException: Pipe broken
	    // Source modifiedSource;
	    // PipedInputStream in = new PipedInputStream();
	    // PipedOutputStream out = new PipedOutputStream(in);
	    // modifiedSource = new StreamSource(in);
	    // new Thread(new Runnable() {
	    // 	    public void run() {
	    // 		try {
	    // 		    Serializer serializer = transformer.newSerializer(out);
	    // 		    transformer.transform(config, serializer);
	    // 		} catch (SaxonApiException e) {
	    // 		    log.error("{}", e);
	    // 		}
	    // 	    }
	    // 	}).start();
	    // log.info("{}", in.readAllBytes());
	    // return new Processor(modifiedSource);

	    // // tempfile approach: Problem: resolving relative paths
	    // Path tempPath = Files.createTempFile("modified", ".xml");
	    // File tempFile = tempPath.toFile();
	    // Serializer serializer = transformer.newSerializer(tempFile);
	    // transformer.transform(config, serializer);
	    // log.info("modified config {}", Files.readString(tempPath));
	    //return new Processor(new StreamSource(tempFile));

	} catch (SaxonApiException e) {
	    log.error("failed to modify Saxon config file {} with {}",
		      config.getSystemId(),
		      ResourceBuilder.class.getResource(MODIFY_CONFIG_XSL));
	    throw new ResourceException(e);
	} catch (URISyntaxException e) {
	    log.error("failed to load XSLT for modifying Saxon config {}",
		      ResourceBuilder.class.getResource(MODIFY_CONFIG_XSL));
	    throw new ResourceException(e);
	// } catch (IOException e) {
	//     log.error("failed to transform for Saxon config");
	//     throw new ResourceException(e);
	}
    }

}
