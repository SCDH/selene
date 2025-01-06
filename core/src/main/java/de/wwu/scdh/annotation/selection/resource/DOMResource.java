package de.wwu.scdh.annotation.selection.resource;

import java.net.URI;
import java.io.InputStream;
import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import javax.xml.transform.sax.SAXSource;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

import nu.validator.htmlparser.sax.HtmlParser;

import de.wwu.scdh.annotation.selection.Resource;

/**
 * A {@link DOMResource} is a web {@link Resource} that can be parsed
 * to a DOM representation, e.g., an HTML or an XML document.
 */
public class DOMResource implements Resource<XdmNode> {

    private final URI uri;

    private final XdmNode document;

    private final Processor processor;

    /**
     * Create a new {@link DOMResource}.
     *
     * @param uri a {@link URI} identifying the resource
     * @param document  the document node (root node)
     * @param processor a saxon {@link Processor} that was used by the document builder
     */
    public DOMResource(URI uri, XdmNode document, Processor processor) {
	this.uri = uri;
	this.document = document;
	this.processor = processor;
    }

    /**
     * Create a new {@link DOMResource} from a parsed JAXP
     *
     * {@link Source}. The source is fed to Saxon's
     *
     * {@link DocumentBuilder}. Please notice the information on JAXP
     * source types in the <a
     * href="https://www.saxonica.com/html/documentation10/sourcedocs/jaxpsources.html">Saxon
     * documentation</a>.
     *
     * @param uri a {@link URI} identifying the resource
     * @param source  the document as a JAXP {@link Source}
     * @param processor a saxon {@link Processor} to be used by the document builder
     *
     * @throws SaxonApiException when the document builder fails
     */
    public DOMResource(URI uri, Source source, Processor processor) throws SaxonApiException {
	this.uri = uri;
	source.setSystemId(uri.toString()); // assert that systemId is set
	this.processor = processor;
	DocumentBuilder documentBuilder = processor.newDocumentBuilder();
	XdmNode docNode = documentBuilder.build(source);
	this.document = docNode;
    }

    /**
     * Make a {@link DOMResource} from HTML input given as an {@link
     * InputStream}. This uses the {@link HtmlParser} for parsing the
     * HTML input, which may be tag soup.
     *
     * @param uri  a {@link URI} identifying the resource
     * @param inputStream  {@link InputStream} with the HTML document
     * @param processor  a saxon {@link Processor} to be used by the document builder
     *
     * @throws SaxonApiException when the document builder fails
     */
    public static DOMResource fromHTML(URI uri, InputStream inputStream, Processor processor) throws SaxonApiException {
	// encapsulate input stream in InputSource
	InputSource inputSource = new InputSource(inputStream);
	inputSource.setSystemId(uri.toString());
	// use HTML parser
	Source source = new SAXSource(new HtmlParser(), inputSource);
	// hand over to just DOM handling
	return new DOMResource(uri, source, processor);
    }

    /**
     * Same as
     *
     * {@link DOMResource#fromHTML(URI, InputStream, Resource, Processor)},
     *
     * but gets the input from the URI.
     *
     * @param uri  a {@link URI} identifying the resource
     * @param processor  a saxon {@link Processor} to be used by the document builder
     *
     * @throws SaxonApiException when the document builder fails
     */
    public static DOMResource fromHTML(URI uri, Processor processor) throws IOException, SaxonApiException {
	InputStream in = uri.toURL().openStream();
	return fromHTML(uri, in, processor);
    }


    /**
     * Make a {@link DOMResource} from XML input given as an
     *
     * {@link InputStream}.
     *
     * @param uri  a {@link URI} identifying the resource
     * @param inputStream  {@link InputStream} with the XML document
     * @param processor  a saxon {@link Processor} to be used by the document builder
     *
     * @throws SaxonApiException when the document builder fails
     */
    public static DOMResource fromXML(URI uri, InputStream inputStream, Processor processor) throws SaxonApiException {
	Source source = new StreamSource(inputStream);
	source.setSystemId(uri.toString());
	return new DOMResource(uri, source, processor);
    }

    /**
     * Same as
     *
     * {@link DOMResource#fromXML(URI, InputStream, Resource, Processor)}, but
     * gets the input from the URI.
     *
     * @param uri  a {@link URI} identifying the resource
     * @param processor  a saxon {@link Processor} to be used by the document builder
     *
     * @throws SaxonApiException when the document builder fails
     */
    public static DOMResource fromXML(URI uri, Processor processor) throws IOException, SaxonApiException {
	InputStream in = uri.toURL().openStream();
	return fromXML(uri, in, processor);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public URI getUri() {
	return this.uri;
    }

    /**
     * Returns the document node of the DOM resource.
     * @return an {@link XdmNode}
     */
    @Override
    public XdmNode getContents() {
	return this.document;
    }

    /**
     * Returns the Saxon {@link Processor} the resource was parsed
     * with.
     * @return a Saxon {@link Processor}
     */
    public Processor getProcessor() {
	return this.processor;
    }

}
