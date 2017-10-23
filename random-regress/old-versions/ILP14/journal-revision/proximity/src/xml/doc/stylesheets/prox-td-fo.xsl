<!DOCTYPE xsl:stylesheet>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exsl="http://exslt.org/common"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:stext="http://nwalsh.com/xslt/ext/com.nwalsh.saxon.TextFactory"
                xmlns:xtext="com.nwalsh.xalan.Text"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                version='1.0'>

<!-- Part of the open-source Proximity system (see LICENSE for
     copyright and license information).
-->

<!-- ====================================================== -->
<!-- Style sheet for KDL Technical Description Documents    -->
<!-- ====================================================== -->

<!-- ====================================================== -->
<!-- Load other style sheets -->

<!-- Set this path to point to your docbook-xsl style sheets -->
<xsl:import href="/usr/local/sgml/docbook-xsl-1.71.1/fo/docbook.xsl"/>

<!-- Load common style sheet definitions -->
<xsl:import href="prox-fo.xsl"/>

<!-- ====================================================== -->
<!-- DocBook XSL Parameters -->

<!-- Override settings from prox-fo.xsl -->
<xsl:param name="alignment">justify</xsl:param>
<xsl:param name="double.sided" select="0"></xsl:param>

<!-- Adjust this to change space between header line and title -->
<xsl:param name="body.margin.top" select="'0.45in'"/>

<!-- ====================================================== -->
<!-- ARTICLE TITLE -->

<!-- Reduce font size -->
<!-- Not sure why text-align=left isn't working -->

<xsl:template match="title" mode="article.titlepage.recto.auto.mode">
  <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format"
      keep-with-next.within-column="always"
      font-size="16pt"
      font-weight="bold"
      text-align="left"
      xsl:use-attribute-sets="article.titlepage.recto.style">
    <xsl:call-template name="component.title">
      <xsl:with-param name="node" select="ancestor-or-self::article[1]"/>
    </xsl:call-template>
  </fo:block>
</xsl:template>

<!-- Affects space between title and following text -->
<xsl:attribute-set name="article.titlepage.recto.style">
  <xsl:attribute name="space-after.optimum">4pt</xsl:attribute>
  <xsl:attribute name="space-after.minimum">3pt</xsl:attribute>
  <xsl:attribute name="space-after.maximum">6pt</xsl:attribute>
</xsl:attribute-set>


<!-- ====================================================== -->
<!-- Footer properties -->

<xsl:template name="footer.content">
  <xsl:param name="pageclass" select="''"/>
  <xsl:param name="sequence" select="''"/>
  <xsl:param name="position" select="''"/>
  <xsl:param name="gentext-key" select="''"/>
  <fo:block>
    <!-- pageclass can be front, body, back -->
    <!-- sequence can be odd, even, first, blank -->
    <!-- position can be left, center, right -->
    <xsl:choose>
      <xsl:when test="$pageclass = 'titlepage'">
        <!-- nop; no footer on title pages -->
      </xsl:when>
<!-- no page numbers in (short) technical descriptions
      <xsl:when test="$double.sided != 0 and $sequence = 'even'
                      and $position='left'">
        <fo:page-number/>
      </xsl:when>
      <xsl:when test="$double.sided != 0 and ($sequence = 'odd' or $sequence = 'first')
                      and $position='right'">
        <fo:page-number/>
      </xsl:when>
      <xsl:when test="$double.sided = 0 and $position='center'">
        <fo:page-number/>
      </xsl:when>
-->
      <xsl:when test="$sequence='blank'">
        <xsl:choose>
          <xsl:when test="$double.sided != 0 and $position = 'left'">
            <fo:page-number/>
          </xsl:when>
          <xsl:when test="$double.sided = 0 and $position = 'center'">
            <fo:page-number/>
          </xsl:when>
          <xsl:otherwise>
            <!-- nop -->
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <!-- nop -->
      </xsl:otherwise>
    </xsl:choose>
  </fo:block>
</xsl:template>

<!-- ====================================================== -->
<!-- HEADINGS -->

<!-- No outdent on headings -->

<xsl:param name="title.margin.left">
  <xsl:choose>
    <xsl:when test="$fop.extensions != 0">0pt</xsl:when>
    <xsl:when test="$passivetex.extensions != 0">0pt</xsl:when>
    <xsl:otherwise>0pt</xsl:otherwise>
  </xsl:choose>
</xsl:param>

<!-- Section headings properties -->

<!-- Reduce font size -->
<xsl:attribute-set name="section.title.level1.properties">
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.25"></xsl:value-of>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
  <xsl:attribute
  name="keep-with-next.within-column">always</xsl:attribute>
</xsl:attribute-set>

<!-- Reduce space before and after headings -->

<xsl:attribute-set name="section.title.properties">
  <xsl:attribute name="font-family">
    <xsl:value-of select="$title.font.family"/>
  </xsl:attribute>
  <xsl:attribute name="font-weight">bold</xsl:attribute>
  <!-- font size is calculated dynamically by section.heading template -->
  <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
  <xsl:attribute name="space-before.minimum">4pt</xsl:attribute>
  <xsl:attribute name="space-before.optimum">6pt</xsl:attribute>
  <xsl:attribute name="space-before.maximum">8pt</xsl:attribute>
  <xsl:attribute name="space-after.minimum">2pt</xsl:attribute>
  <xsl:attribute name="space-after.optimum">3pt</xsl:attribute>
  <xsl:attribute name="space-after.maximum">3pt</xsl:attribute>
  <xsl:attribute name="text-align">left</xsl:attribute>
  <xsl:attribute name="start-indent"><xsl:value-of select="$title.margin.left"/></xsl:attribute>
</xsl:attribute-set>

<!-- ====================================================== -->
<!-- Bibliolist properties -->

<!-- Reduce space before -->

<xsl:template match="bibliolist">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>
  <fo:block id="{$id}"
            space-before.minimum="0.2em"
            space-before.optimum="0.4em"
            space-before.maximum="0.6em">
    <xsl:if test="blockinfo/title|info/title|title">
      <xsl:call-template name="formal.object.heading"/>
    </xsl:if>
    <xsl:apply-templates select="*[not(self::blockinfo)
                                   and not(self::title)
                                   and not(self::titleabbrev)]"/>
  </fo:block>
</xsl:template>

<!-- Reduce space between entries (change the original normal.para.spacing
     to biblist.spacing, defined below) -->

<xsl:attribute-set name="biblist.spacing">
  <xsl:attribute name="space-before.optimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.6em</xsl:attribute>
</xsl:attribute-set>

<xsl:template match="bibliomixed">
  <xsl:param name="label">
    <xsl:call-template name="biblioentry.label"/>
  </xsl:param>
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="string(.) = ''">
      <xsl:variable name="bib" select="document($bibliography.collection,.)"/>
      <xsl:variable name="entry" select="$bib/bibliography/*[@id=$id][1]"/>
      <xsl:choose>
        <xsl:when test="$entry">
          <xsl:choose>
            <xsl:when test="$bibliography.numbered != 0">
              <xsl:apply-templates select="$entry">
                <xsl:with-param name="label" select="$label"/>
              </xsl:apply-templates>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="$entry"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>
            <xsl:text>No bibliography entry: </xsl:text>
            <xsl:value-of select="$id"/>
            <xsl:text> found in </xsl:text>
            <xsl:value-of select="$bibliography.collection"/>
          </xsl:message>
          <!-- New spacing attribute set used here -->
          <fo:block id="{$id}" xsl:use-attribute-sets="biblist.spacing">
            <xsl:text>Error: no bibliography entry: </xsl:text>
            <xsl:value-of select="$id"/>
            <xsl:text> found in </xsl:text>
            <xsl:value-of select="$bibliography.collection"/>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <fo:block id="{$id}" xsl:use-attribute-sets="biblist.spacing"
                start-indent="0.5in" text-indent="-0.5in">
        <xsl:copy-of select="$label"/>
        <xsl:apply-templates mode="bibliomixed.mode"/>
      </fo:block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ======================================================= -->
<!-- False captions -->

<!-- This duplicates a template in prox-common.xsl.  We'll leave it in
     here for now for testing, but it might be worthwhile to move this
     back to prox-common.xsl eventually -->

<xsl:template match="phrase">
  <xsl:choose>
    <xsl:when test="@role='hyphenate'">
      <xsl:attribute name="hyphenate">true</xsl:attribute>
        <xsl:call-template name="inline.charseq"/>
    </xsl:when>
    <xsl:when test="@role='edcomment'">
      <!-- comment out the following line to remove edcomments from
           the generated doc -->
      <xsl:call-template name="inline.edcomment"/>
    </xsl:when>
    <xsl:when test="@role='caption'">
      <xsl:call-template name="inline.false.caption"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="inline.charseq"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Used to generate text that looks like a caption but where it
     would be inconvenient to use a real caption -->

<xsl:template name="inline.false.caption">
  <xsl:param name="content">
    <xsl:apply-templates/>
  </xsl:param>
  <fo:inline font-family="sans-serif" font-size="9pt" font-weight="bold">
    <xsl:choose>
      <xsl:when test="@dir">
        <fo:inline>
          <xsl:attribute name="direction">
            <xsl:choose>
              <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
              <xsl:otherwise>rtl</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:copy-of select="$content"/>
        </fo:inline>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$content"/>
      </xsl:otherwise>
    </xsl:choose>
  </fo:inline>
</xsl:template>


<!-- ======================================================= -->
<!-- Change format for figure caption to not include an      -->
<!-- identifier (e.g., "Figure 1") and use same font as body -->

<xsl:param name="proxcommon.l10n.xml" select="document('')"/> 
<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0"> 
  <l:l10n language="en"> 
   <l:context name="title">
      <l:template name="figure" text="%t"/>
   </l:context>
  </l:l10n>
</l:i18n>

<!-- Override default formatting of figure titles.  We define a new
     attribute set that will be used instead of
     formal.title.properties.
-->

<xsl:attribute-set name="figure.title.properties"
                   use-attribute-sets="normal.para.spacing">
  <xsl:attribute name="font-weight">normal</xsl:attribute>
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.0"/>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
  <xsl:attribute name="hyphenate">false</xsl:attribute>
  <xsl:attribute name="text-align">center</xsl:attribute>
  <xsl:attribute name="font-family">sans-serif</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.6em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.8em</xsl:attribute>
</xsl:attribute-set>

<xsl:template name="formal.object.heading">
  <xsl:param name="object" select="."/>
  <xsl:param name="placement" select="'before'"/>
  <!-- Changed to use figure.title.properties attribute set -->
  <fo:block xsl:use-attribute-sets="figure.title.properties">
    <xsl:choose>
      <xsl:when test="$placement = 'before'">
        <xsl:attribute
               name="keep-with-next.within-column">always</xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute
               name="keep-with-previous.within-column">always</xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates select="$object" mode="object.title.markup">
      <xsl:with-param name="allow-anchors" select="1"/>
    </xsl:apply-templates>
  </fo:block>
</xsl:template>


<!-- ====================================================== -->
<!-- End stylesheet -->

</xsl:stylesheet>