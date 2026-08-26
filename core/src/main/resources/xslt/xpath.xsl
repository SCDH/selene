<!-- A library of XPath functions for making XPath Selector components -->
<xsl:package name="http://wwu.de/scdh/selection-engine/node-tracing" package-version="1.0.0"
  version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:trace="http://wwu.de/scdh/selection-engine/node-tracing"
  xmlns:sel="http://wwu.de/scdh/selection-engine/xpaths">

  <!-- the path starting from the deepest node with an ID -->
  <xsl:function name="sel:from-deepest-id" as="xs:string" visibility="final">
    <xsl:param name="ctx" as="node()"/>
    <xsl:value-of>
      <xsl:variable name="el-steps" as="element()*" select="$ctx/ancestor-or-self::element()"/>
      <xsl:variable name="ids" as="xs:integer*">
        <xsl:for-each select="1 to count($el-steps)">
          <xsl:variable name="step" as="xs:integer" select="."/>
          <xsl:choose>
            <xsl:when test="$el-steps[$step]/@xml:id">
              <xsl:sequence select="$step"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:sequence select="-1"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </xsl:variable>
      <xsl:variable name="id-at" as="xs:integer?" select="$ids[. ne -1][last()]"/>
      <xsl:choose>
        <xsl:when test="$id-at">
          <xsl:text>id(&apos;</xsl:text>
          <xsl:value-of select="$el-steps[$id-at]/@xml:id"/>
          <xsl:text>&apos;)</xsl:text>
        </xsl:when>
        <xsl:otherwise/>
      </xsl:choose>
      <xsl:for-each select="$el-steps[position() gt $id-at or empty($id-at)]">
        <xsl:variable name="step" as="element()" select="."/>
        <xsl:variable name="ns" as="xs:string" select="namespace-uri($step)"/>
        <xsl:variable name="name" as="xs:string" select="name($step)"/>
        <xsl:variable name="pos" as="xs:integer"
          select="count($step/preceding-sibling::node()[fn:name() eq $name])+1"/>
        <xsl:value-of select="concat('/Q{', $ns, '}', $name, '[', $pos, ']')"/>
      </xsl:for-each>
      <xsl:variable name="text-step" as="text()?" select="$ctx[self::text()]"/>
      <xsl:if test="$text-step">
        <xsl:value-of
          select="concat('/text()[', count($text-step/preceding-sibling::text())+1, ']')"/>
      </xsl:if>
    </xsl:value-of>
  </xsl:function>

  <!-- the path starting from the deepest element with an ID, but not going down into a text leaf -->
  <xsl:function name="sel:from-deepest-id-to-element" as="xs:string" visibility="final">
    <xsl:param name="ctx" as="node()"/>
    <xsl:choose>
      <xsl:when test="$ctx[self::text()][parent::trace:text]">
        <xsl:value-of select="sel:from-deepest-id($ctx/parent::*/parent::*)"/>
      </xsl:when>
      <xsl:when test="$ctx[self::text()]">
        <xsl:value-of select="sel:from-deepest-id($ctx/parent::*)"/>
      </xsl:when>
      <xsl:when test="$ctx[self::trace:text]">
        <xsl:value-of select="sel:from-deepest-id($ctx/parent::*)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="sel:from-deepest-id($ctx)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- the path starting at the root element, but not going down into a text leaf -->
  <xsl:function name="sel:to-element" as="xs:string" visibility="final">
    <xsl:param name="ctx" as="node()"/>
    <xsl:choose>
      <xsl:when test="$ctx[self::text()][parent::trace:text]">
        <xsl:value-of select="path($ctx/parent::*/parent::*)"/>
      </xsl:when>
      <xsl:when test="$ctx[self::text()]">
        <xsl:value-of select="path($ctx/parent::*)"/>
      </xsl:when>
      <xsl:when test="$ctx[self::trace:text]">
        <xsl:value-of select="path($ctx/parent::*)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="path($ctx)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

</xsl:package>
