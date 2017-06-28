<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" version="1.0" encoding="UTF-8"/>

<xsl:template match="/">
<xsl:text>
</xsl:text>
  <xsl:element name="article">

<xsl:text>
</xsl:text>

  <xsl:element name="title">
    <xsl:value-of select="'Glossary Definitions'"/>
  </xsl:element>
<xsl:text>
</xsl:text>
  <xsl:element name="para">
<xsl:text>
</xsl:text>

  <xsl:for-each select="//glossterm">
    <xsl:element name="glossterm">
      <xsl:value-of select="." />
   </xsl:element>
<xsl:text>
</xsl:text>
  </xsl:for-each>

  </xsl:element>     <!-- /para -->
<xsl:text>
</xsl:text>

<xsl:processing-instruction name="pagebreak"/>

<xsl:element name="glossary">
  <xsl:attribute name="role">
    <xsl:value-of select="'auto'"/>
  </xsl:attribute>
<xsl:text>
</xsl:text>
<xsl:processing-instruction name="dbfo">glossterm-width="3.5cm" </xsl:processing-instruction>
<xsl:text>
</xsl:text>
<!-- output the dummy glossentry -->
<xsl:element name="glossentry">
<xsl:text>
</xsl:text>
  <xsl:element name="glossterm">
    <xsl:value-of select="'dummy'"/>
  </xsl:element>
<xsl:text>
</xsl:text>
  <xsl:element name="glossdef">
    <xsl:value-of select="'irrelevent'"/>
  </xsl:element>
<xsl:text>
</xsl:text>
</xsl:element>       <!-- /glossentry -->
<xsl:text>
</xsl:text>
</xsl:element>     <!-- /glossary -->

<xsl:text>
</xsl:text>
</xsl:element>     <!-- /article -->
</xsl:template>

</xsl:stylesheet>
