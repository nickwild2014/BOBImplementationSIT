<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet version="1.0"

                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"

                exclude-result-prefixes="xsl">

  <xsl:template match="node()">

    <xsl:if
test="count(descendant::text()[string-length(normalize-space(.))>0]|@*)">

      <xsl:copy>

        <xsl:apply-templates select="@*|node()"/>

      </xsl:copy>

    </xsl:if>

  </xsl:template>

  <xsl:template match="@*">

    <xsl:copy/>

  </xsl:template>

  <xsl:template match="text()">

    <xsl:value-of select="normalize-space(.)"/>

  </xsl:template>

</xsl:stylesheet>
