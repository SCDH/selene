<?xml version="1.0" encoding="UTF-8"?>
<!-- identity transformation -->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:tracking="http://wwu.scdh.de/selection"
    version="3.0">

  <xsl:mode on-no-match="shallow-copy"/>

  <xsl:template match="text() | *">
    <xsl:copy>
      <xsl:attribute name="tracking:track" select="generate-id(.)"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
