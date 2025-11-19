package de.wwu.scdh.annotation.selection.resource;

import java.net.URI;
import java.net.MalformedURLException;
import java.io.IOException;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltPackage;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Destination;

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
    protected Resource<?> parseResource(URI resource, Parser parser) throws ResourceException {
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
     * Makes a {@link MappedDOMResource} from a preimage and an XSLT
     * stylesheet.
     *
     */
    public static MappedDOMResource mapWithXsltTracePackage(DOMResource preimage, URI stylesheet, URI tracePkg)
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

	// load the trace XSLT package
	StreamSource traceSource;
	try {
	    traceSource = new StreamSource(tracePkg.toURL().openStream(), tracePkg.toString());
	} catch (MalformedURLException e) {
	    throw new ResourceException(e.getMessage());
	} catch (IOException e) {
		throw new ResourceException(e.getMessage());
	}

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
    

}
