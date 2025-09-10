<!-- Implementation of the node tracing package for internal use

This implementation is used for node tracing when the output
method is XML.
-->
<xsl:package name="http://wwu.de/scdh/selection-engine/node-tracing" package-version="1.0.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:trace="http://wwu.de/scdh/selection-engine/node-tracing" exclude-result-prefixes="#all"
  version="3.0">

  <xsl:template name="trace:source-id" as="attribute()?" visibility="final">
    <xsl:context-item as="node()" use="required"/>
    <xsl:attribute name="trace:source-id" select="generate-id(.)"/>
  </xsl:template>

  <xsl:template name="trace:text" as="node()" visibility="final">
    <xsl:context-item as="text()" use="required"/>
    <xsl:element name="trace:text">
      <xsl:attribute name="trace:text-wrapper">true</xsl:attribute>
      <xsl:call-template name="trace:source-id"/>
      <xsl:copy-of select="."/>
    </xsl:element>
  </xsl:template>

  <xsl:template name="trace:root" as="item()*" visibility="final">
    <!-- A document node is required because we transform with Saxon
	 Xslt30transformer.applytemplates() which returns an XdmValue
	 which is not wrapped into a document node. The document node
	 is required for correct XPath selectors.
    -->
    <xsl:document>
      <xsl:apply-templates mode="#current"
        select="node() | attribute() | comment() | processing-instruction()"/>
    </xsl:document>
  </xsl:template>

</xsl:package>
