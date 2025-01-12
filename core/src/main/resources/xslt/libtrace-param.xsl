<xsl:package name="http://wwu.de/scdh/selection-engine/node-tracing" package-version="1.0.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:trace="http://wwu.de/scdh/selection-engine/node-tracing" exclude-result-prefixes="#all"
  version="3.0">

  <xsl:param name="trace-nodes" as="xs:boolean" select="false()" static="true"/>

  <xsl:param name="output-method" as="xs:string" select="'xml'" static="true"/>

  <xsl:template name="trace:source-id" as="attribute()?" visibility="final"
    use-when="not($trace-nodes)">
    <xsl:context-item as="element()" use="required"/>
  </xsl:template>

  <xsl:template name="trace:source-id" as="attribute()?" visibility="final"
    use-when="$trace-nodes and $output-method ne 'html'">
    <xsl:context-item as="element()" use="required"/>
    <xsl:attribute name="trace:source-id" select="generate-id(.)"/>
  </xsl:template>

  <xsl:template name="trace:source-id" as="attribute()?" visibility="final"
    use-when="$trace-nodes and $output-method eq 'html'">
    <xsl:context-item as="element()" use="required"/>
    <xsl:attribute name="data-node-tracing-source-id" select="generate-id(.)"/>
  </xsl:template>

  <xsl:template name="trace:wrap-text" as="node()" visibility="final" use-when="not($trace-nodes)">
    <xsl:context-item as="text()" use="required"/>
    <xsl:copy-of select="."/>
  </xsl:template>


  <xsl:template name="trace:wrap-text" as="node()" visibility="final"
    use-when="$trace-nodes and $output-method ne 'html'">
    <xsl:context-item as="text()" use="required"/>
    <xsl:element name="trace:text">
      <xsl:attribute name="trace:text-wrapper">true</xsl:attribute>
      <xsl:call-template name="trace:source-id"/>
      <xsl:copy-of select="."/>
    </xsl:element>
  </xsl:template>

  <xsl:template name="trace:wrap-text" as="node()" visibility="final"
    use-when="$trace-nodes and $output-method eq 'html'">
    <xsl:context-item as="text()" use="required"/>
    <xsl:element name="span">
      <xsl:attribute name="data-node-tracing-text-wrapper">true</xsl:attribute>
      <xsl:call-template name="trace:source-id"/>
      <xsl:copy-of select="."/>
    </xsl:element>
  </xsl:template>


  <xsl:template name="trace:root" as="item()*" visibility="final" use-when="not($trace-nodes)">
    <xsl:apply-templates mode="#current" select="."/>
  </xsl:template>

  <xsl:template name="trace:root" as="item()*" visibility="final" use-when="$trace-nodes">
    <xsl:element name="trace:root">
      <xsl:apply-templates mode="#current" select="."/>
    </xsl:element>
  </xsl:template>

</xsl:package>
