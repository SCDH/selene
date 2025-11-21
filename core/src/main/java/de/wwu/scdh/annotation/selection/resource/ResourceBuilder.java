package de.wwu.scdh.annotation.selection.resource;

import java.net.URI;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltPackage;

import de.wwu.scdh.annotation.selection.Resource;
import de.wwu.scdh.annotation.selection.ResourceException;

/**
 * A {@link ResourceBuilder} makes {@link Resource} from a URI.
 */
public class ResourceBuilder {

    /**
     * There are some resource parsers available, that this class
     * offers building resources with.
     *
     */
    public enum Parser {
	XML,
	HTML
    }

    public static final String TRACE_XSL = "/xslt/libtrace.xsl";

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
     * stylesheet.
     *
     * @param preimage - the resource deriving from
     * @param stylesheet - {@link URI} of the XSLT stylesheet used for deriving
     * @param traceStream - an input stream made of the the XSLT package for adding the mapping information
     * @param traceSystemId - the path of the XSLT package for adding the mapping information
     */
    public static MappedDOMResource mapWithXsltTracePackage(DOMResource preimage, URI stylesheet, InputStream traceStream, String traceSystemId)
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
	    compiler.importPackage(tracePackage, null, null);
	    XsltExecutable executable = compiler.compile(xsl);
	    Xslt30Transformer transformer = executable.load30();
	    XdmValue imageValue = transformer.applyTemplates(preimage.getContents());
	    XdmValueResource image = new XdmValueResource(null, imageValue, proc);

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
     */
    public static MappedDOMResource mapWithXsltTracePackage(DOMResource preimage, URI stylesheet, URI tracePkg)
	throws ResourceException {
	// load the trace XSLT package
	try {
	    InputStream traceStream = tracePkg.toURL().openStream();
	    return ResourceBuilder.mapWithXsltTracePackage(preimage, stylesheet, traceStream, tracePkg.toString());
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
     */
    public static MappedDOMResource mapWithXsltTracePackage(DOMResource preimage, URI stylesheet)
	throws ResourceException {
	return ResourceBuilder.mapWithXsltTracePackage(preimage, stylesheet, ResourceBuilder.class.getResourceAsStream(TRACE_XSL), TRACE_XSL);
    }

}
