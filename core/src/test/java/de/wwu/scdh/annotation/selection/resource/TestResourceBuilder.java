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
import net.sf.saxon.s9api.XsltPackage;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.ResourceBuilder;

public class TestResourceBuilder {

    private static void log(String s) {
	//System.err.println(s);
    }

    public static final Processor PROC = new Processor(false);

    public static final File LIBTRACE_XML = Paths.get("src", "main", "resources", "xslt", "libtrace-xml.xsl").toFile();

    public static final File TEST_DIR = Paths.get("..", "test").toFile();

    public static final URI GESANG_XML  = new File(TEST_DIR, "Gesang.tei.xml").toURI();

    public static final URI ID_XSL = Paths.get("src", "test", "resources", "xsl", "id.xsl").toFile().toURI();

    // @Disabled
    // @Test
    // public void testGesantParsedWithSAX() throws IOException, SaxonApiException {
    // 	ResourceBuilder resourceBuilder = new ResourceBuilder(PROC);
    // 	Resource<?> source = resourceBuilder.parseResource(GESANG_XML, ResourceBuilder.Parser.XML);
    // 	assertThrows(ResourceException.class, () -> new MappedDOMResource(source));
    // }

    @Test
    public void testGesangParsedWithXerces() throws ResourceException {
	ResourceBuilder resourceBuilder = new ResourceBuilder(PROC);
	Resource<?> src = resourceBuilder.parseResource(GESANG_XML, ResourceBuilder.Parser.XML);
	DOMResource source = (DOMResource) src;
	MappedDOMResource preimage = new MappedDOMResource(source);
	XdmNode doc = preimage.getContents();
	//assertTrue(MappedDOMResource.getNodeTrace(doc, MappedDOMResource.ID_XPATH).isPresent());
	//assertEquals(Optional.of(1), MappedDOMResource.getNodeTrace(doc));
	XdmSequenceIterator<XdmNode> tree = doc.axisIterator(Axis.DESCENDANT_OR_SELF);
	// traverse preimage and assert that there are node traces in the mapping of identifiers to preimage nodes
	log("preimage");
	while (tree.hasNext()) {
	    XdmNode node = tree.next();
	    //log(node.getNodeKind().toString() + " " + node.getNodeName() + " " + String.valueOf(MappedDOMResource.getNodeTrace(node, MappedDOMResource.ID_XPATH)));
	    assertTrue(MappedDOMResource.getNodeTrace(node, MappedDOMResource.ID_XPATH).isPresent());
	    assertTrue(preimage.idToPreimageNode.containsKey(MappedDOMResource.getNodeTrace(node, MappedDOMResource.ID_XPATH).get()));
	    assertEquals(node, preimage.idToPreimageNode.get(MappedDOMResource.getNodeTrace(node, MappedDOMResource.ID_XPATH).get()));
	}
    }

    @Test
    public void testGesangMappedWithIdentity() throws ResourceException, SaxonApiException {
	ResourceBuilder resourceBuilder = new ResourceBuilder(PROC);
	Resource<?> src = resourceBuilder.parseResource(GESANG_XML, ResourceBuilder.Parser.XML);
	DOMResource source = (DOMResource) src;
	MappedDOMResource preimage = ResourceBuilder.mapWithXsltTracePackage(source, ID_XSL, null);
	// traverse image and assert that there is a trace on its nodes
	log("image");
	XdmSequenceIterator<XdmItem> items = preimage.getImage().getContents().iterator();
	while (items.hasNext()) {
	    XdmItem item = items.next();
	    if (XdmNode.class.isAssignableFrom(item.getClass())) {
		XdmNode tree = (XdmNode) item;
		XdmSequenceIterator<XdmNode> treeIterator = tree.axisIterator(Axis.DESCENDANT_OR_SELF);
		while (treeIterator.hasNext()) {
		    XdmNode node = treeIterator.next();
		    log("id: " + node.getNodeKind().toString() + " " + node.getNodeName() + " " + String.valueOf(MappedDOMResource.getNodeTrace(node, MappedDOMResource.TRACKING_XPATH)));
		    if (node.getNodeKind().equals(XdmNodeKind.DOCUMENT)) continue;
		    assertTrue(MappedDOMResource.getNodeTrace(node, MappedDOMResource.TRACKING_XPATH).isPresent());
		}
	    }
	}
    }

    @Test
    public void testGesangForwardMapping() throws ResourceException, SaxonApiException {
	ResourceBuilder resourceBuilder = new ResourceBuilder(PROC);
	Resource<?> src = resourceBuilder.parseResource(GESANG_XML, ResourceBuilder.Parser.XML);
	DOMResource source = (DOMResource) src;
	MappedDOMResource preimage = ResourceBuilder.mapWithXsltTracePackage(source, ID_XSL, null);
	XdmSequenceIterator<XdmNode> tree = preimage.getContents().axisIterator(Axis.DESCENDANT_OR_SELF);
	// traverse preimage and assert that we have a forward mapping of preimage nodes
	while (tree.hasNext()) {
	    XdmNode node = tree.next();
	    log("forward: " + node.getNodeKind().toString() + " " + String.valueOf(MappedDOMResource.getNodeTrace(node, MappedDOMResource.ID_XPATH)));
	    assertTrue(MappedDOMResource.getNodeTrace(node, MappedDOMResource.ID_XPATH).isPresent());
	    if (!node.getNodeKind().equals(XdmNodeKind.ELEMENT)) {
		continue;
	    }
	    assertNotEquals(null, preimage.getCorrespondingInImage(node)); // fails: is null
	    assertEquals(1, preimage.getCorrespondingInImage(node).size(), "preimage node: " + node.getNodeName().toString());
	}
    }

    @Test
	public void testGesangBackwardMapping() throws ResourceException, SaxonApiException {
	ResourceBuilder resourceBuilder = new ResourceBuilder(PROC);
	Resource<?> src = resourceBuilder.parseResource(GESANG_XML, ResourceBuilder.Parser.XML);
	DOMResource source = (DOMResource) src;
	MappedDOMResource preimage = ResourceBuilder.mapWithXsltTracePackage(source, ID_XSL, null);
	XdmValueResource image = preimage.getImage();
	assertEquals(1, image.getContents().size());
	XdmSequenceIterator<XdmItem> seq = image.getContents().iterator();
	while (seq.hasNext()) {
	    XdmItem item = seq.next();
	    if (item.isNode()) {
		XdmNode nodeItem = (XdmNode) item;
		XdmSequenceIterator<XdmNode> tree = nodeItem.axisIterator(Axis.DESCENDANT_OR_SELF);
		// traverse preimage and assert that we have a forward mapping of preimage nodes
		while (tree.hasNext()) {
		    XdmNode node = tree.next();
		    log("backward: " + node.getNodeKind().toString() + " "
			+ String.valueOf(MappedDOMResource.getNodeTrace(node, MappedDOMResource.ID_XPATH)));
		    assertTrue(MappedDOMResource.getNodeTrace(node, MappedDOMResource.ID_XPATH).isPresent());
		    if (node.getNodeKind().equals((XdmNodeKind.TEXT))) {
			
		    }
		    if (!node.getNodeKind().equals(XdmNodeKind.ELEMENT)) {
			continue;
		    }
		    assertNotEquals(null, preimage.getCorrespondingInPreimage(node), "image node: " + node.getNodeName()); // fails: is null
		    assertTrue(preimage.getCorrespondingInPreimage(node).isPresent(), "image node: " + node.getNodeName());
		    if (!node.getNodeName().equals("trace:text")) {
			//assertEquals(node.getNodeName(), preimage.getCorrespondingInPreimage(node).get().getNodeName());
		    }
		    log("backward image node: " + node.getNodeName());
		}
	    }
	}
    }

}
