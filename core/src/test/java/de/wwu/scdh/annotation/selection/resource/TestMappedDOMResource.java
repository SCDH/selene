package de.wwu.scdh.annotation.selection.resource;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DOMDestination;
import net.sf.saxon.event.Builder;
import net.sf.saxon.dom.DocumentBuilderImpl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMException;


import de.wwu.scdh.annotation.selection.*;


public class TestMappedDOMResource {

    public static Processor PROC = TestMappedDOMResource.mkProc();

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();

    public static final File ID_XSL = Paths.get("src", "test", "resources", "xsl", "id.xsl").toFile();

    public static Processor mkProc() {
	Processor proc = new Processor(false);
	//proc.setConfigurationProperty(Feature.TREE_MODEL, Builder.LINKED_TREE);
	return proc;
    }

    public static Document transform(DOMResource resource, File stylesheet, DOMImplementation domImpl) throws SaxonApiException, DOMException, javax.xml.parsers.ParserConfigurationException {
	XsltCompiler compiler = PROC.newXsltCompiler();
	XsltExecutable executable = compiler.compile(stylesheet);
	Xslt30Transformer transformer = executable.load30();

	// DocumentBuilderImpl builderImpl = new DocumentBuilderImpl();
	// builderImpl.setConfiguration(PROC.getUnderlyingConfiguration());
	// Document result = builderImpl.newDocument();

	// trial
	//Document result = domImpl.createDocument("root", "namespace", null);

	javax.xml.parsers.DocumentBuilderFactory documentBuilderFactory = javax.xml.parsers.DocumentBuilderFactory.newDefaultInstance();
	javax.xml.parsers.DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	Document result = documentBuilder.newDocument();

	DOMDestination destination = new DOMDestination(result);
	transformer.applyTemplates(resource.getContents(), destination);
	return result;
    }

    @Disabled
    @Test
    public void testGesantParsedWithSAX() throws IOException, SaxonApiException {
	DOMResource source = DOMResource.fromXML(GESANG_XML, null, PROC);
	assertThrows(ResourceException.class, () -> new MappedDOMResource(source));
    }

    @Test
    public void testGesangParsedWithXerces() throws ResourceException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmNode doc = preimage.getContents();
	assertTrue(MappedDOMResource.getNodeTrace(doc).isPresent());
	assertEquals(Optional.of(1), MappedDOMResource.getNodeTrace(doc));
	XdmSequenceIterator<XdmNode> tree = doc.axisIterator(Axis.DESCENDANT_OR_SELF);
	while (tree.hasNext()) {
	    XdmNode node = tree.next();
	    assertTrue(MappedDOMResource.getNodeTrace(node).isPresent());
	}
    }

    @Test
    public void testGesangMappedWithIdentity() throws ResourceException, SaxonApiException, javax.xml.parsers.ParserConfigurationException {
	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
	MappedDOMResource preimage = new MappedDOMResource(source);
	W3CDOMResource image = new W3CDOMResource(GESANG_XML, transform(source, ID_XSL, preimage.getDOMImplementation()));
	preimage.setImage(image);
	// traverse image and assert that
	assertImageUserData(image.getContents(), preimage);
    }

    private void assertImageUserData(Node node, MappedDOMResource preimage) {
	System.out.println(node.getNodeType());
	if (node.getNodeType() != Document.DOCUMENT_NODE) {
	    assertNotEquals(null, node.getUserData(MappedDOMResource.NODE_ID_USER_DATA));
	    System.out.println(node.getUserData(MappedDOMResource.NODE_ID_USER_DATA));
	}
	// recursion over children
	NodeList children = node.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
	    assertImageUserData(children.item(i), preimage);
	}
    }

    // @Test
    // public void testGesangForwardMapping() throws ResourceException, SaxonApiException {
    // 	DOMResource source = DOMResource.fromXMLwithXerces(GESANG_XML, null, PROC);
    // 	MappedDOMResource preimage = new MappedDOMResource(source);
    // 	XdmValueResource image = new XdmValueResource(GESANG_XML, transform(source, ID_XSL));
    // 	preimage.setImage(image);
    // 	XdmSequenceIterator<XdmNode> tree = preimage.getContents().axisIterator(Axis.DESCENDANT_OR_SELF);
    // 	// traverse preimage and assert that we have a forward mapping of preimage nodes
    // 	while (tree.hasNext()) {
    // 	    XdmNode node = tree.next();
    // 	    System.out.println(node.getNodeKind().toString());
    // 	    assertTrue(MappedDOMResource.getNodeTrace(node).isPresent());
    // 	    if (node.getNodeKind().equals(XdmNodeKind.DOCUMENT)) {
    // 		continue;
    // 	    }
    // 	    assertNotEquals(null, preimage.getCorrespondingInImage(node)); // fails: is null
    // 	    assertEquals(1, preimage.getCorrespondingInImage(node).size());
    // 	}
    // }

}
