<!DOCTYPE xsl:stylesheet [
<!ENTITY primary   'normalize-space(concat(primary/@sortas, primary[not(@sortas)]))'>
<!ENTITY secondary 'normalize-space(concat(secondary/@sortas, secondary[not(@sortas)]))'>
<!ENTITY tertiary  'normalize-space(concat(tertiary/@sortas, tertiary[not(@sortas)]))'>
]>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exsl="http://exslt.org/common"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:stext="http://nwalsh.com/xslt/ext/com.nwalsh.saxon.TextFactory"
                xmlns:xtext="com.nwalsh.xalan.Text"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:date="http://exslt.org/dates-and-times"  
                exclude-result-prefixes="date"  
                version='1.0'>

<!-- Part of the open-source Proximity system (see LICENSE for
     copyright and license information).
-->

<!-- ====================================================== -->
<!-- Style sheet for PDF version of Proximity Tutorial      -->
<!-- ====================================================== -->

<!-- ====================================================== -->
<!-- Load other style sheets -->

<!-- Set this path to point to your docbook-xsl style sheets -->
<xsl:import href="/usr/local/sgml/docbook-xsl-1.71.1/fo/docbook.xsl"/>

<!-- Load common style sheet definitions -->
<xsl:import href="prox-common.xsl"/>

<!-- ====================================================== -->
<!-- DocBook XSL Parameters -->

<!-- FOP uses proportional table column widths with
     tablecolumns.extensions turned off -->
<xsl:param name="tablecolumns.extension" select="'0'"></xsl:param>

<!-- No watermark image; we just use draft mode to change header -->
<xsl:param name="draft.watermark.image"></xsl:param>

<xsl:param name="alignment">left</xsl:param>
<xsl:param name="ulink.show" select="0"></xsl:param>
<xsl:param name="insert.xref.page.number">maybe</xsl:param>
<xsl:param name="toc.section.depth">1</xsl:param>
<xsl:param name="preferred.mediaobject.role" select="'fo'"></xsl:param>

<!--  Use for FOP 0.20.5 -->
  <xsl:param name="fop.extensions" select="1"></xsl:param>
<!--  Use for FOP 0.93
<xsl:param name="fop1.extensions" select="1"></xsl:param>
-->

<xsl:param name="hyphenate">false</xsl:param>
<xsl:param name="double.sided" select="1"></xsl:param>
<xsl:param name="shade.verbatim" select="1"></xsl:param>

<!-- give more space to running header -->
<xsl:param name="header.column.widths" select="'1 2 1'"/>

<!-- ====================================================== -->
<!-- Additional Proximity parameters -->

<!-- Default value is 'distrib'; can be changed by genproxdoc.sh
     script -->
<xsl:param name="prox.destination" select="distrib"></xsl:param>

<xsl:param name="prox.docname" select="test"></xsl:param>
<xsl:param name="prox.review" select="0"></xsl:param>
<xsl:param name="prox.imagelist" select="0"></xsl:param>

<!-- ====================================================== -->
<!-- Customization layer -->
<!-- Generated text -->

<!-- Some comments about xref styles:
     @ The order of alternative matters; the default must be
       listed last. 
     @ Make sure that you update BOTH the prox-fo.xsl and
       prox-html.xsl stylesheets when generated text should be
       different for printed and HTML versions.
     @ Only include gentext items in prox-fo.xsl and prox-html.xsl
       when they differ for print and PDF, otherwise  place them in
       prox-common.xsl.
     @ Place default templates (no xrefstyle) in prox-common.xsl.
-->

<xsl:param name="proxfo.l10n.xml" select="document('')"/> 
<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0"> 
  <l:l10n language="en"> 
     <l:context name="xref">
     <!-- These defns are used for crossrefs to non-numbered elements -->
        <l:template name="appendix" style="pageref" 
                    text="%t&#160;(p.&#160;%p)"/>
        <l:template name="chapter" style="pageref"
                    text="%t&#160;(p.&#160;%p)"/>
<!-- Not sure why, but this item seems to cause a "circular reference
     to attribute" error (included for completeness although not
     actually used in the document, so commenting out shouldn't be an
     issue.
        <l:template name="bridgehead" style="pageref"
                    text="%t&#160;(p.&#160;%p)"/>
-->
        <l:template name="example" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="figure" style="pageref"
                    text="Figure&#160;%n&#160;(p.&#160;%p)"/>
        <l:template name="procedure" style="pageref"
                    text="Exercise&#160;%n&#160;(p.&#160;%p)"/>
        <l:template name="sect1" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="sect2" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="sect3" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
     </l:context>
     <l:context name="xref-number">
     <!-- These defns are used when xref.with.number.and.title = 0 -->
        <l:template name="appendix" style="pageref"
                    text="Appendix&#160;%n&#160;(p.&#160;%p)"/>
        <l:template name="chapter" style="pageref"
                    text="Chapter&#160;%n&#160;(p.&#160;%p)"/>
        <l:template name="bridgehead" style="pageref"
                    text="%t&#160;(p.&#160;%p)"/>
        <l:template name="example" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="figure" style="pageref"
                    text="Figure&#160;%n&#160;(p.&#160;%p)"/>
        <l:template name="procedure" style="pageref"
                    text="Exercise&#160;%n&#160;(p.&#160;%p)"/>
        <l:template name="sect1" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="sect2" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="sect3" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
     </l:context>
     <l:context name="xref-number-and-title">
     <!-- These defns are used when xref.with.number.and.title = 1 -->
        <l:template name="appendix" style="pageref"
                    text="Appendix&#160;%n, %t&#160;(p.&#160;%p)"/>
        <l:template name="appendix" style="brief"
                    text="Appendix&#160;%n"/>
        <l:template name="chapter" style="pageref"
                    text="Chapter&#160;%n, %t&#160;(p.&#160;%p)"/>
        <l:template name="chapter" style="brief"
                    text="Chapter&#160;%n"/>
        <l:template name="bridgehead" style="pageref"
                    text="%t&#160;(p.&#160;%p)"/>
        <l:template name="example" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="figure" style="pageref"
                    text="Figure&#160;%n&#160;(p.&#160;%p)"/>
        <l:template name="procedure" style="pageref"
                    text="Exercise&#160;%n&#160;(p.&#160;%p)"/>
        <l:template name="sect1" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="sect2" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
        <l:template name="sect3" style="pageref"
                    text="&#8220;%t&#8221;&#160;(p.&#160;%p)"/>
     </l:context>
  </l:l10n>
</l:i18n>

<xsl:param name="local.l10n.merged">
  <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
    <l:l10n language="en">
      <xsl:copy-of
           select="$proxfo.l10n.xml//l:i18n/l:l10n[@language='en']/*"/>
      <xsl:copy-of
           select="$proxcommon.l10n.xml//l:i18n/l:l10n[@language='en']/*"/>
    </l:l10n>
  </l:i18n>
</xsl:param>

<xsl:param name="local.l10n.xml"
           select="exsl:node-set($local.l10n.merged)"/>

<!-- Specify front matter to include -->

<xsl:param name="generate.toc">
book      toc,title,procedure
article   nop
</xsl:param>

<!-- ========================================================= -->
<!-- PROCESSING INSTRUCTIONS -->
<!-- Page and line breaks; include <?pagebreak?> or <?linebreak?> in
     the source document -->

<!-- Page breaks are ignored if we're generating the review copy.  All
     the page breaks will be in different positions, anyway, due to
     printing the index and glossary terms -->

<xsl:template match="processing-instruction('pagebreak')">
  <xsl:if test="$prox.review = 0">
   <fo:block break-after="page"/>
  </xsl:if>
</xsl:template>

<xsl:template match="processing-instruction('linebreak')">
  <fo:block></fo:block>
</xsl:template>

<!-- Keep together -->

<!-- 
  Got these from the DocBook-apps mailing list.  The underlying
  functionality is not yet implemented by the main FOP trunk
  implementation, but these might be useful for when FOP releases a
  new version.

  To use (for example):
  <para><?pxfo keep-together?>blah, blah, blah</para>

  Note that the processing element cannot be used at any arbitrary
  spot in the document.  For example, it can follow a <para> element,
  but not directly follow a <listitem> element.
-->

<!--
<xsl:template match="*[processing-instruction('pxfo')='keep-together']">
  <fo:block keep-together.within-column="always">
    <xsl:apply-imports/>
  </fo:block>
</xsl:template>

<xsl:template match="*[./processing-instruction('pxfo')='keep-with-next']">
  <fo:block keep-together.within-column="always"
            keep-with-next.within-column="always">
    <xsl:apply-imports/>
  </fo:block>
</xsl:template>

<xsl:template match="*[./processing-instruction('pxfo')='keep-with-previous']">
  <fo:block keep-together.within-column="always"
            keep-with-previous.within-column="always">
    <xsl:apply-imports/>
  </fo:block>
</xsl:template>
-->

<!-- ========================================================= -->
<!-- Section headings properties -->

<xsl:attribute-set name="section.title.level1.properties">
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.8"></xsl:value-of>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="section.title.level2.properties">
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.44"></xsl:value-of>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<!-- Defines the outdent for section titles -->
<xsl:param name="title.margin.left">
  <xsl:choose>
    <xsl:when test="$passivetex.extensions != 0">0pt</xsl:when>
    <xsl:otherwise>-4pc</xsl:otherwise>
  </xsl:choose>
</xsl:param>

<!-- Change the outdent for level 3 and 4 titles -->
<xsl:param name="title.margin.sect3.left" select="'0pt'"></xsl:param>
<xsl:param name="title.margin.sect4.left" select="'0pt'"></xsl:param>

<xsl:attribute-set name="section.title.level3.properties">
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.0"></xsl:value-of>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="section.title.level4.properties">
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.0"/>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.6em</xsl:attribute>
</xsl:attribute-set>

<!-- Modified to use title.margin.sect3.left outdent (0pt) for
     section level 3 (sect3) headings [ditto for sect4 headings] -->

<xsl:template name="section.heading">
  <xsl:param name="level" select="1"/>
  <xsl:param name="marker" select="1"/>
  <xsl:param name="title"/>
  <xsl:param name="titleabbrev"/>
  <fo:block xsl:use-attribute-sets="section.title.properties">
    <xsl:if test="$marker != 0">
      <fo:marker marker-class-name="section.head.marker">
        <xsl:choose>
          <xsl:when test="$titleabbrev = ''">
            <xsl:value-of select="$title"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$titleabbrev"/>
          </xsl:otherwise>
        </xsl:choose>
      </fo:marker>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="$level=1">
        <fo:block xsl:use-attribute-sets="section.title.level1.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:when test="$level=2">
        <fo:block xsl:use-attribute-sets="section.title.level2.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:when test="$level=3">
        <xsl:attribute name="margin-left">
          <xsl:value-of select="$title.margin.sect3.left"/>
        </xsl:attribute>
        <fo:block xsl:use-attribute-sets="section.title.level3.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:when test="$level=4">
        <xsl:attribute name="margin-left">
          <xsl:value-of select="$title.margin.sect3.left"/>
        </xsl:attribute>
        <fo:block xsl:use-attribute-sets="section.title.level4.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:when test="$level=5">
        <fo:block xsl:use-attribute-sets="section.title.level5.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:otherwise>
        <fo:block xsl:use-attribute-sets="section.title.level6.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:otherwise>
    </xsl:choose>
  </fo:block>
</xsl:template>

<!-- The Proximity Cookbook uses <section> elements instead of
     <sect1>, <sect2>, etc.  This template adds a pagebreak before a
     <section> that's at level 1, inserting a page break between
     individual recipes.
-->

<xsl:template match="section">
  <xsl:choose>
    <xsl:when test="$rootid = @id">
      <xsl:call-template name="section.page.sequence"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="id">
        <xsl:call-template name="object.id"/>
      </xsl:variable>
      <xsl:variable name="renderas">
        <xsl:choose>
          <xsl:when test="@renderas = 'sect1'">1</xsl:when>
          <xsl:when test="@renderas = 'sect2'">2</xsl:when>
          <xsl:when test="@renderas = 'sect3'">3</xsl:when>
          <xsl:when test="@renderas = 'sect4'">4</xsl:when>
          <xsl:when test="@renderas = 'sect5'">5</xsl:when>
          <xsl:otherwise><xsl:value-of select="''"/></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="level">
        <xsl:choose>
          <xsl:when test="$renderas != ''">
            <xsl:value-of select="$renderas"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="section.level"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <!-- xsl:use-attribute-sets takes only a Qname, not a variable -->
      <xsl:choose>
        <xsl:when test="$level = 1">
          <!-- pagebreak before new recipe -->
          <xsl:if test="$prox.docname = 'cookbook'">
            <fo:block break-after="page"/>
          </xsl:if>
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="section.level1.properties">
            <xsl:call-template name="section.content"/>
          </fo:block>
        </xsl:when>
        <xsl:when test="$level = 2">
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="section.level2.properties">
            <xsl:call-template name="section.content"/>
          </fo:block>
        </xsl:when>
        <xsl:when test="$level = 3">
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="section.level3.properties">
            <xsl:call-template name="section.content"/>
          </fo:block>
        </xsl:when>
        <xsl:when test="$level = 4">
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="section.level4.properties">
            <xsl:call-template name="section.content"/>
          </fo:block>
        </xsl:when>
        <xsl:when test="$level = 5">
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="section.level5.properties">
            <xsl:call-template name="section.content"/>
          </fo:block>
        </xsl:when>
        <xsl:otherwise>
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="section.level6.properties">
            <xsl:call-template name="section.content"/>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- ====================================================== -->
<!-- Formal object properties -->
<!-- Titles -->

<xsl:attribute-set name="formal.title.properties"
                   use-attribute-sets="normal.para.spacing">
  <xsl:attribute name="font-weight">bold</xsl:attribute>
  <xsl:attribute name="font-family">
    <xsl:value-of select="$title.font.family"></xsl:value-of>
  </xsl:attribute>
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 1.2"/>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
  <xsl:attribute name="hyphenate">false</xsl:attribute>
  <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0.0em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.6em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="figure.caption.properties"
                   use-attribute-sets="normal.para.spacing">
  <xsl:attribute name="font-family">
    <xsl:value-of select="$title.font.family"></xsl:value-of>
  </xsl:attribute>
  <xsl:attribute name="font-size">9pt</xsl:attribute>
  <xsl:attribute name="hyphenate">false</xsl:attribute>
  <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0.0em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.6em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.8em</xsl:attribute>
</xsl:attribute-set>

<!-- Added an xsl:choose statement to distinguish between figure
     captions and other formal titles as they are formatted
     differently -->
<xsl:template name="formal.object.heading">
  <xsl:param name="object" select="."/>
  <xsl:param name="placement" select="'before'"/>
  <xsl:choose>
    <xsl:when test="local-name(.) = 'figure'">
      <fo:block xsl:use-attribute-sets="figure.caption.properties">
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
    </xsl:when>
    <xsl:otherwise>
      <fo:block xsl:use-attribute-sets="formal.title.properties">
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
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Spacing -->

<xsl:attribute-set name="formal.object.properties">
  <xsl:attribute name="space-before.minimum">0.5em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0.8em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">1.0em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.5em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.8em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">1.0em</xsl:attribute>
  <xsl:attribute
  name="keep-together.within-column">always</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="informal.object.properties">
  <xsl:attribute name="space-before.minimum">0.5em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0.8em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">1.0em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.5em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.8em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">1.0em</xsl:attribute>
</xsl:attribute-set>

<!-- ====================================================== -->
<!-- Paragraph spacing -->

<xsl:attribute-set name="normal.para.spacing">
  <xsl:attribute name="space-before.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.6em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.6em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="tight.para.spacing">
  <xsl:attribute name="space-before.optimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.4em</xsl:attribute>
</xsl:attribute-set>

<!-- ====================================================== -->
<!-- List spacing -->

<xsl:attribute-set name="list.block.spacing">
  <xsl:attribute name="space-before.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.6em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.6em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="list.item.spacing">
  <xsl:attribute name="space-before.optimum">0.3em</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.4em</xsl:attribute>
</xsl:attribute-set>

<!-- ====================================================== -->
<!-- Modified listitem element to allow no bullet or symbol -->
<!-- Done to correct for spurious duplicated bullet in nested lists
-->

<xsl:template match="itemizedlist/listitem">
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>
  <xsl:variable name="itemsymbol">
    <xsl:call-template name="list.itemsymbol">
      <xsl:with-param name="node" select="parent::itemizedlist"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="item.contents">
    <fo:list-item-label end-indent="label-end()">
      <fo:block>
        <xsl:choose>
          <xsl:when test="$itemsymbol='disc'">&#x2022;</xsl:when>
          <xsl:when test="$itemsymbol='bullet'">&#x2022;</xsl:when>
          <xsl:when test="$itemsymbol='none'"> </xsl:when>
          <!-- why do these symbols not work? -->
          <!--
          <xsl:when test="$itemsymbol='circle'">&#x2218;</xsl:when>
          <xsl:when test="$itemsymbol='round'">&#x2218;</xsl:when>
          <xsl:when test="$itemsymbol='square'">&#x2610;</xsl:when>
          <xsl:when test="$itemsymbol='box'">&#x2610;</xsl:when>
          -->
          <xsl:otherwise>&#x2022;</xsl:otherwise>
        </xsl:choose>
      </fo:block>
    </fo:list-item-label>
    <fo:list-item-body start-indent="body-start()">
      <fo:block>
	<xsl:apply-templates/>
      </fo:block>
    </fo:list-item-body>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="parent::*/@spacing = 'compact'">
      <fo:list-item id="{$id}" xsl:use-attribute-sets="compact.list.item.spacing">
        <xsl:copy-of select="$item.contents"/>
      </fo:list-item>
    </xsl:when>
    <xsl:otherwise>
      <fo:list-item id="{$id}" xsl:use-attribute-sets="list.item.spacing">
        <xsl:copy-of select="$item.contents"/>
      </fo:list-item>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ====================================================== -->
<!-- Sidebars, blockquotes, and admonitions -->
<!-- Sidebar properties -->

<xsl:attribute-set name="sidebar.properties"
     use-attribute-sets="formal.object.properties">
  <xsl:attribute name="border-style">solid</xsl:attribute>
  <xsl:attribute name="border-width">1pt</xsl:attribute>
  <xsl:attribute name="border-color">black</xsl:attribute>
  <xsl:attribute name="background-color">#FFFFFF</xsl:attribute>
  <xsl:attribute name="padding-left">12pt</xsl:attribute>
  <xsl:attribute name="padding-right">12pt</xsl:attribute>
  <xsl:attribute name="padding-top">5pt</xsl:attribute>
  <xsl:attribute name="padding-bottom">5pt</xsl:attribute>
</xsl:attribute-set>

<!-- Blockquote properties -->

<xsl:attribute-set name="blockquote.properties">
  <xsl:attribute name="margin-left">0.25in</xsl:attribute>
  <xsl:attribute name="margin-right">0.25in</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.2em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="indented.blockquote.properties">
  <xsl:attribute name="margin-left">0.5in</xsl:attribute>
  <xsl:attribute name="margin-right">0.5in</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.2em</xsl:attribute>
</xsl:attribute-set>

<xsl:template match="blockquote">
  <xsl:variable name="indent">
    <xsl:choose>
       <xsl:when test="@role='indented'">
          <xsl:value-of select="'indented'" />
       </xsl:when>
       <xsl:when test="ancestor::procedure|ancestor::listitem">
          <xsl:value-of select="'indented'" />
       </xsl:when>
       <xsl:otherwise>
          <xsl:value-of select="'toplevel'" />
       </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:choose>
  <xsl:when test="$indent='indented'">
    <fo:block xsl:use-attribute-sets="indented.blockquote.properties">
      <xsl:call-template name="anchor"/>
      <fo:block>
        <xsl:if test="title">
          <fo:block xsl:use-attribute-sets="formal.title.properties">
            <xsl:apply-templates select="." mode="object.title.markup"/>
          </fo:block>
        </xsl:if>
        <xsl:apply-templates select="*[local-name(.) != 'title'
                                     and local-name(.) != 'attribution']"/>
      </fo:block>
    <xsl:if test="attribution">
      <fo:block text-align="right">
        <!-- mdash -->
        <xsl:text>&#x2014;</xsl:text>
        <xsl:apply-templates select="attribution"/>
      </fo:block>
    </xsl:if>
  </fo:block>
  </xsl:when>
  <xsl:otherwise>
    <fo:block xsl:use-attribute-sets="blockquote.properties">
      <xsl:call-template name="anchor"/>
      <fo:block>
        <xsl:if test="title">
          <fo:block xsl:use-attribute-sets="formal.title.properties">
            <xsl:apply-templates select="." mode="object.title.markup"/>
          </fo:block>
        </xsl:if>
        <xsl:apply-templates select="*[local-name(.) != 'title'
                                     and local-name(.) != 'attribution']"/>
      </fo:block>
    <xsl:if test="attribution">
      <fo:block text-align="right">
        <!-- mdash -->
        <xsl:text>&#x2014;</xsl:text>
        <xsl:apply-templates select="attribution"/>
      </fo:block>
    </xsl:if>
  </fo:block>
  </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- Admonition properties -->

<xsl:attribute-set name="admonition.title.properties"
     use-attribute-sets="normal.para.spacing">
  <xsl:attribute name="font-size">10pt</xsl:attribute>
  <xsl:attribute name="font-weight">bold</xsl:attribute>
  <xsl:attribute name="font-family">
    <xsl:value-of select="$title.font.family"></xsl:value-of>
  </xsl:attribute>
  <xsl:attribute name="hyphenate">false</xsl:attribute>
  <xsl:attribute name="keep-with-next">always</xsl:attribute>
  <xsl:attribute name="space-before.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.6em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.6em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="admonition.properties">
  <xsl:attribute name="space-before.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.6em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0.4em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.6em</xsl:attribute>
</xsl:attribute-set>

<!-- Set width for admonition graphic -->
<!-- Changes the value for provisional-distance-between-starts
     and provisional-label-separation -->

<xsl:template name="graphical.admonition">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>
  <xsl:variable name="graphic.width">
     <xsl:apply-templates select="." mode="admon.graphic.width"/>
  </xsl:variable>
  <fo:block id="{$id}">
    <fo:list-block provisional-distance-between-starts="{$graphic.width} + 0pt"
    		provisional-label-separation="0pt"
		xsl:use-attribute-sets="list.block.spacing">
      <fo:list-item>
          <fo:list-item-label end-indent="label-end()">
            <fo:block>
              <fo:external-graphic width="auto" height="auto"
	      		           content-width="{$graphic.width}" >
                <xsl:attribute name="src">
                  <xsl:call-template name="admon.graphic"/>
                </xsl:attribute>
              </fo:external-graphic>
            </fo:block>
          </fo:list-item-label>
          <fo:list-item-body start-indent="body-start()">
            <xsl:if test="$admon.textlabel != 0 or title">
              <fo:block xsl:use-attribute-sets="admonition.title.properties">
                <xsl:apply-templates select="." mode="object.title.markup"/>
              </fo:block>
            </xsl:if>
            <fo:block xsl:use-attribute-sets="admonition.properties">
              <xsl:apply-templates/>
            </fo:block>
          </fo:list-item-body>
      </fo:list-item>
    </fo:list-block>
  </fo:block>
</xsl:template>

<!-- Center figures and captions -->

<xsl:attribute-set name="figure.properties"
                   use-attribute-sets="formal.object.properties">
    <xsl:attribute name="text-align">center</xsl:attribute>
</xsl:attribute-set>

<!-- ====================================================== -->
<!-- Format e-mail addresses -->

<xsl:template match="email">
  <xsl:call-template name="inline.italicseq">
    <xsl:with-param name="content">
      <fo:inline keep-together.within-line="always" hyphenate="false">
        <xsl:apply-templates/>
      </fo:inline>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<!-- ====================================================== -->
<!--                 VERBATIM ENVIRONMENTS                  -->
<!-- ====================================================== -->

<!-- Added condition to check for value of 'role' attribute.
     Override shading behavior set by the shade.verbatim
     parameter if role=noshading. Also made literallayout use
     a more compact layout so that we get rid of the leading and
     trailing vertical space around the text. -->

<xsl:attribute-set name="compact.verbatim.properties">
  <xsl:attribute name="space-before.minimum">0em</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0.2em</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0em</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0em</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0.2em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="small.verbatim.properties"
  use-attribute-sets="verbatim.properties">
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 0.85"/>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="small.compact.verbatim.properties"
     use-attribute-sets="compact.verbatim.properties
                         small.verbatim.properties">
  <xsl:attribute name="text-align">start</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="monospace.small.verbatim.properties"
     use-attribute-sets="small.verbatim.properties
                         monospace.properties">
  <xsl:attribute name="text-align">start</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="monospace.small.compact.verbatim.properties"
     use-attribute-sets="compact.verbatim.properties
                         small.verbatim.properties monospace.properties">
  <xsl:attribute name="text-align">start</xsl:attribute>
</xsl:attribute-set>

<xsl:template match="literallayout">
  <xsl:param name="suppress-numbers" select="'0'"/>
  <xsl:variable name="content">
    <xsl:choose>
      <xsl:when test="$suppress-numbers = '0'
                      and @linenumbering = 'numbered'
                      and $use.extensions != '0'
                      and $linenumbering.extension != '0'">
        <xsl:call-template name="number.rtf.lines">
          <xsl:with-param name="rtf">
            <xsl:apply-templates/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="@class='monospaced'">
      <xsl:choose>
        <xsl:when test="$shade.verbatim != 0">
          <xsl:choose>
            <xsl:when test="@role='noshading'">
              <fo:block
                    wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    linefeed-treatment="preserve"
                    xsl:use-attribute-sets="monospace.small.compact.verbatim.properties">
                <xsl:copy-of select="$content"/>
              </fo:block>
            </xsl:when>
            <xsl:otherwise>
              <fo:block
                    wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    linefeed-treatment="preserve"
                    xsl:use-attribute-sets="monospace.small.compact.verbatim.properties shade.verbatim.style">
                <xsl:copy-of select="$content"/>
              </fo:block>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <fo:block
                    wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    linefeed-treatment="preserve"
                    xsl:use-attribute-sets="monospace.small.compact.verbatim.properties">
            <xsl:copy-of select="$content"/>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="$shade.verbatim != 0">
          <xsl:choose>
            <xsl:when test="@role='noshading'">
              <fo:block
                    wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    text-align='start'
                    linefeed-treatment="preserve"
                    xsl:use-attribute-sets="small.compact.verbatim.properties">
                <xsl:copy-of select="$content"/>
              </fo:block>
            </xsl:when>
            <xsl:otherwise>
              <fo:block
                    wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    text-align='start'
                    linefeed-treatment="preserve"
                    xsl:use-attribute-sets="small.compact.verbatim.properties shade.verbatim.style">
                <xsl:copy-of select="$content"/>
              </fo:block>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <fo:block 
                    wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    text-align='start'
                    linefeed-treatment="preserve"
                    xsl:use-attribute-sets="small.compact.verbatim.properties">
            <xsl:copy-of select="$content"/>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Changed template for programlisting to use
     monospace.small.verbatim.properties attribute set.
     It obeys the shade.verbatim setting.  The screen and synopsys 
     elements are redefined, below, to use the
     monospace.small.verbatim.properties attribute set and have no
     shading. The two blockquote attribute sets are defined above. -->

<!-- Getting pretty complicated; should look for  a way to simplify
     the conditional tests -->
<xsl:template match="programlisting">
  <xsl:param name="suppress-numbers" select="'0'"/>
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>
  <xsl:variable name="content">
    <xsl:choose>
      <xsl:when test="$suppress-numbers = '0'
                      and @linenumbering = 'numbered'
                      and $use.extensions != '0'
                      and $linenumbering.extension != '0'">
        <xsl:call-template name="number.rtf.lines">
          <xsl:with-param name="rtf">
            <xsl:apply-templates/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="$shade.verbatim != 0">
      <xsl:choose>
        <xsl:when test="ancestor::procedure|ancestor::listitem">
          <fo:block wrap-option='no-wrap'
                    white-space-collapse='false'
                    white-space-treatment='preserve'
                    linefeed-treatment='preserve'
                    xsl:use-attribute-sets="indented.blockquote.properties monospace.small.verbatim.properties shade.verbatim.style">
            <xsl:copy-of select="$content"/>
          </fo:block>
        </xsl:when>
        <xsl:otherwise>
          <fo:block wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    linefeed-treatment='preserve'
                    xsl:use-attribute-sets="blockquote.properties monospace.small.verbatim.properties shade.verbatim.style">
            <xsl:copy-of select="$content"/>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="ancestor::procedure|ancestor::listitem">
          <fo:block wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    linefeed-treatment="preserve"
                    xsl:use-attribute-sets="indented.blockquote.properties monospace.small.verbatim.properties">
            <xsl:copy-of select="$content"/>
          </fo:block>
        </xsl:when>
        <xsl:otherwise>
          <fo:block wrap-option='no-wrap'
                    white-space-collapse='false'
		    white-space-treatment='preserve'
                    linefeed-treatment="preserve"
                    xsl:use-attribute-sets="blockquote.properties monospace.small.verbatim.properties">
            <xsl:copy-of select="$content"/>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="synopsis|screen">
  <xsl:param name="suppress-numbers" select="'0'"/>
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>
  <xsl:variable name="content">
    <xsl:choose>
      <xsl:when test="$suppress-numbers = '0'
                      and @linenumbering = 'numbered'
                      and $use.extensions != '0'
                      and $linenumbering.extension != '0'">
        <xsl:call-template name="number.rtf.lines">
          <xsl:with-param name="rtf">
            <xsl:apply-templates/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="ancestor::procedure|ancestor::listitem">
      <fo:block wrap-option='no-wrap'
                white-space-collapse='false'
                white-space-treatment='preserve'
                linefeed-treatment='preserve'
                xsl:use-attribute-sets="indented.blockquote.properties monospace.small.verbatim.properties">
        <xsl:copy-of select="$content"/>
      </fo:block>
    </xsl:when>
    <xsl:otherwise>
      <fo:block wrap-option='no-wrap'
                white-space-collapse='false'
                white-space-treatment='preserve'
                linefeed-treatment='preserve'
                xsl:use-attribute-sets="blockquote.properties monospace.small.verbatim.properties">
        <xsl:copy-of select="$content"/>
      </fo:block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- ====================================================== -->
<!--                  FORMALPARA TITLES                     -->
<!-- ====================================================== -->

<!-- Default is to have a period at the end of the title.  Omit
     the period if the paragraph title element has a role attribute
     value of "header". -->

<!-- removed 'keep-with-next.within-line="always"' from fo:inline -->


<xsl:template match="formalpara/title">
  <xsl:variable name="titleStr">
      <xsl:apply-templates/>
  </xsl:variable>
  <xsl:variable name="lastChar">
    <xsl:if test="$titleStr != ''">
      <xsl:value-of select="substring($titleStr,string-length($titleStr),1)"/>
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="titleRole">
    <xsl:apply-templates select="@role"/>
  </xsl:variable>
  <fo:inline font-weight="bold"
             padding-end="1em">
    <xsl:copy-of select="$titleStr"/>
    <xsl:if test="$titleRole != 'header' and $lastChar != ''
                  and not(contains($runinhead.title.end.punct, $lastChar))">
      <xsl:value-of select="$runinhead.default.title.end.punct"/>
    </xsl:if>
    <xsl:text>&#160;</xsl:text>
  </fo:inline>
</xsl:template>

<!-- ====================================================== -->
<!--                  TABLE FORMATTING                      -->
<!-- ====================================================== -->

<!-- Don't let rows break across pages                      -->
<!-- Copied from mailing list message from Bob Stayton      -->

<xsl:param name="keep.row.together">1</xsl:param>

<xsl:template match="row">
  <xsl:param name="spans"/>
  <fo:table-row>
   <xsl:if test="$keep.row.together != '0'">
    <xsl:attribute name="keep-together">always</xsl:attribute>
  </xsl:if>
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates select="(entry|entrytbl)[1]">
      <xsl:with-param name="spans" select="$spans"/>
    </xsl:apply-templates>
  </fo:table-row>
  <xsl:if test="following-sibling::row">
    <xsl:variable name="nextspans">
      <xsl:apply-templates select="(entry|entrytbl)[1]" mode="span">
        <xsl:with-param name="spans" select="$spans"/>
      </xsl:apply-templates>
    </xsl:variable>
    <xsl:apply-templates select="following-sibling::row[1]">
      <xsl:with-param name="spans" select="$nextspans"/>
    </xsl:apply-templates>
  </xsl:if>
</xsl:template>

<!-- Enable shaded background for informaltables (used in plain text
     data format appendix -->

<xsl:attribute-set name="informaltable.properties"
   use-attribute-sets="informal.object.properties">
   <xsl:attribute name="background-color">
    <xsl:choose>
      <xsl:when test="@tabstyle='shaded'">#E0E0E0</xsl:when>
      <xsl:otherwise>#FFFFFF</xsl:otherwise>
    </xsl:choose>
  </xsl:attribute>
</xsl:attribute-set>

<!-- create a new attribute set for more compact tables -->
<xsl:attribute-set name="table.compact.cell.padding">
  <xsl:attribute name="padding-left">2pt</xsl:attribute>
  <xsl:attribute name="padding-right">2pt</xsl:attribute>
  <xsl:attribute name="padding-top">.5pt</xsl:attribute>
  <xsl:attribute name="padding-bottom">.5pt</xsl:attribute>
</xsl:attribute-set>

<!--
<xsl:template name="table.layout">
  <xsl:param name="table.content" select="NOTANODE"/>
  <xsl:choose>
    <xsl:when test="@cellpassing='0'">
      <xsl:copy-of select="$table.content"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$table.content"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
-->

<!-- here's the l-o-n-g template for <entry> in a table row.
     Blanks lines and commented-out sections have been deleted to save
     a bit of space and make it easier to find the end of the template.
     The modification uses the compact padding attr set above when the
     <table> or <informaltable> has role="compact" -->

<xsl:template match="entry|entrytbl" name="entry">
  <xsl:param name="col" select="1"/>
  <xsl:param name="spans"/>
  <!-- KDL: Added 'spacing' variable to record parent table's 'cellpadding'
       attribute value
  -->
  <xsl:variable name="spacing" select="ancestor::informaltable/attribute::cellpadding"/>
  <xsl:variable name="row" select="parent::row"/>
  <xsl:variable name="group" select="$row/parent::*[1]"/>
  <xsl:variable name="frame" select="ancestor::tgroup/parent::*/@frame"/>
  <xsl:variable name="empty.cell" select="count(node()) = 0"/>
  <xsl:variable name="named.colnum">
    <xsl:call-template name="entry.colnum"/>
  </xsl:variable>
  <xsl:variable name="entry.colnum">
    <xsl:choose>
      <xsl:when test="$named.colnum &gt; 0">
        <xsl:value-of select="$named.colnum"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$col"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="entry.colspan">
    <xsl:choose>
      <xsl:when test="@spanname or @namest">
        <xsl:call-template name="calculate.colspan"/>
      </xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="following.spans">
    <xsl:call-template name="calculate.following.spans">
      <xsl:with-param name="colspan" select="$entry.colspan"/>
      <xsl:with-param name="spans" select="$spans"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="rowsep">
    <xsl:choose>
      <!-- If this is the last row, rowsep never applies. -->
      <xsl:when test="not(ancestor-or-self::row[1]/following-sibling::row
                          or ancestor-or-self::thead/following-sibling::tbody
                          or ancestor-or-self::tbody/preceding-sibling::tfoot)">
        <xsl:value-of select="0"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="inherited.table.attribute">
          <xsl:with-param name="entry" select="."/>
          <xsl:with-param name="colnum" select="$entry.colnum"/>
          <xsl:with-param name="attribute" select="'rowsep'"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="colsep">
    <xsl:choose>
      <!-- If this is the last column, colsep never applies. -->
      <xsl:when test="$following.spans = ''">0</xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="inherited.table.attribute">
          <xsl:with-param name="entry" select="."/>
          <xsl:with-param name="colnum" select="$entry.colnum"/>
          <xsl:with-param name="attribute" select="'colsep'"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="valign">
    <xsl:call-template name="inherited.table.attribute">
      <xsl:with-param name="entry" select="."/>
      <xsl:with-param name="colnum" select="$entry.colnum"/>
      <xsl:with-param name="attribute" select="'valign'"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="align">
    <xsl:call-template name="inherited.table.attribute">
      <xsl:with-param name="entry" select="."/>
      <xsl:with-param name="colnum" select="$entry.colnum"/>
      <xsl:with-param name="attribute" select="'align'"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="char">
    <xsl:call-template name="inherited.table.attribute">
      <xsl:with-param name="entry" select="."/>
      <xsl:with-param name="colnum" select="$entry.colnum"/>
      <xsl:with-param name="attribute" select="'char'"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="charoff">
    <xsl:call-template name="inherited.table.attribute">
      <xsl:with-param name="entry" select="."/>
      <xsl:with-param name="colnum" select="$entry.colnum"/>
      <xsl:with-param name="attribute" select="'charoff'"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="$spans != '' and not(starts-with($spans,'0:'))">
      <xsl:call-template name="entry">
        <xsl:with-param name="col" select="$col+1"/>
        <xsl:with-param name="spans" select="substring-after($spans,':')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$entry.colnum &gt; $col">
      <xsl:call-template name="empty.table.cell">
        <xsl:with-param name="colnum" select="$col"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
        <xsl:with-param name="col" select="$col+1"/>
        <xsl:with-param name="spans" select="substring-after($spans,':')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="cell.content">
        <fo:block>
          <xsl:call-template name="table.cell.block.properties"/>
          <!-- are we missing any indexterms? -->
          <xsl:if test="not(preceding-sibling::entry)
                        and not(parent::row/preceding-sibling::row)">
            <!-- this is the first entry of the first row -->
            <xsl:if test="ancestor::thead or
                          (ancestor::tbody
                           and not(ancestor::tbody/preceding-sibling::thead
                                   or ancestor::tbody/preceding-sibling::tbody))">
              <!-- of the thead or the first tbody -->
              <xsl:apply-templates select="ancestor::tgroup/preceding-sibling::indexterm"/>
            </xsl:if>
          </xsl:if>
          <xsl:choose>
            <xsl:when test="$empty.cell">
              <xsl:text>&#160;</xsl:text>
            </xsl:when>
            <xsl:when test="self::entrytbl">
              <xsl:variable name="prop-columns"
                            select=".//colspec[contains(@colwidth, '*')]"/>
              <fo:table xsl:use-attribute-sets="table.table.properties">
                <xsl:if test="count($prop-columns) != 0">
                  <xsl:attribute name="table-layout">fixed</xsl:attribute>
                </xsl:if>
                <xsl:call-template name="tgroup"/>
              </fo:table>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates/>
            </xsl:otherwise>
          </xsl:choose>
        </fo:block>
      </xsl:variable>
      <xsl:variable name="cell-orientation">
        <xsl:call-template name="dbfo-attribute">
          <xsl:with-param name="pis"
                          select="ancestor-or-self::entry/processing-instruction('dbfo')"/>
          <xsl:with-param name="attribute" select="'orientation'"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="row-orientation">
        <xsl:call-template name="dbfo-attribute">
          <xsl:with-param name="pis"
                          select="ancestor-or-self::row/processing-instruction('dbfo')"/>
          <xsl:with-param name="attribute" select="'orientation'"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="cell-width">
        <xsl:call-template name="dbfo-attribute">
          <xsl:with-param name="pis"
                          select="ancestor-or-self::entry/processing-instruction('dbfo')"/>
          <xsl:with-param name="attribute" select="'rotated-width'"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="row-width">
        <xsl:call-template name="dbfo-attribute">
          <xsl:with-param name="pis"
                          select="ancestor-or-self::row/processing-instruction('dbfo')"/>
          <xsl:with-param name="attribute" select="'rotated-width'"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="orientation">
        <xsl:choose>
          <xsl:when test="$cell-orientation != ''">
            <xsl:value-of select="$cell-orientation"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$row-orientation"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="rotated-width">
        <xsl:choose>
          <xsl:when test="$cell-width != ''">
            <xsl:value-of select="$cell-width"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$row-width"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="bgcolor">
        <xsl:call-template name="dbfo-attribute">
          <xsl:with-param name="pis"
                          select="ancestor-or-self::entry/processing-instruction('dbfo')"/>
          <xsl:with-param name="attribute" select="'bgcolor'"/>
        </xsl:call-template>
      </xsl:variable>
      <!-- KDL: Created a conditional here based on the value of the
           parent table's 'cellpadding' attribute.  If it's 0, we use
           the table.compact.cell.padding attribute set, otherwise we
           use the standard table.cell.padding attribute set. 
           Only the attribute set specified in the <fo:table> element
           changes in these versions. -->
      <xsl:choose>
        <xsl:when test="$spacing='0'">
         <fo:table-cell xsl:use-attribute-sets="table.compact.cell.padding">
           <xsl:call-template name="table.cell.properties">
             <xsl:with-param name="bgcolor.pi" select="$bgcolor"/>
             <xsl:with-param name="rowsep.inherit" select="$rowsep"/>
             <xsl:with-param name="colsep.inherit" select="$colsep"/>
             <xsl:with-param name="col" select="$col"/>
             <xsl:with-param name="valign.inherit" select="$valign"/>
             <xsl:with-param name="align.inherit" select="$align"/>
             <xsl:with-param name="char.inherit" select="$char"/>
           </xsl:call-template>
           <xsl:call-template name="anchor"/>
           <xsl:if test="@morerows">
             <xsl:attribute name="number-rows-spanned">
               <xsl:value-of select="@morerows+1"/>
             </xsl:attribute>
           </xsl:if>
           <xsl:if test="$entry.colspan &gt; 1">
             <xsl:attribute name="number-columns-spanned">
               <xsl:value-of select="$entry.colspan"/>
             </xsl:attribute>
           </xsl:if>
           <xsl:choose>
             <xsl:when test="$fop.extensions = 0 and $passivetex.extensions = 0
                             and $orientation != ''">
               <fo:block-container reference-orientation="{$orientation}">
                 <xsl:if test="$rotated-width != ''">
                   <xsl:attribute name="width">
                     <xsl:value-of select="$rotated-width"/>
                   </xsl:attribute>
                 </xsl:if>
                 <xsl:copy-of select="$cell.content"/>
               </fo:block-container>
             </xsl:when>
             <xsl:otherwise>
               <xsl:copy-of select="$cell.content"/>
             </xsl:otherwise>
           </xsl:choose>
         </fo:table-cell>
        </xsl:when>
        <xsl:otherwise>
         <fo:table-cell xsl:use-attribute-sets="table.cell.padding">
           <xsl:call-template name="table.cell.properties">
             <xsl:with-param name="bgcolor.pi" select="$bgcolor"/>
             <xsl:with-param name="rowsep.inherit" select="$rowsep"/>
             <xsl:with-param name="colsep.inherit" select="$colsep"/>
             <xsl:with-param name="col" select="$col"/>
             <xsl:with-param name="valign.inherit" select="$valign"/>
             <xsl:with-param name="align.inherit" select="$align"/>
             <xsl:with-param name="char.inherit" select="$char"/>
           </xsl:call-template>
           <xsl:call-template name="anchor"/>
           <xsl:if test="@morerows">
             <xsl:attribute name="number-rows-spanned">
               <xsl:value-of select="@morerows+1"/>
             </xsl:attribute>
           </xsl:if>
           <xsl:if test="$entry.colspan &gt; 1">
             <xsl:attribute name="number-columns-spanned">
               <xsl:value-of select="$entry.colspan"/>
             </xsl:attribute>
           </xsl:if>
           <xsl:choose>
             <xsl:when test="$fop.extensions = 0 and $passivetex.extensions = 0
                             and $orientation != ''">
               <fo:block-container reference-orientation="{$orientation}">
                 <xsl:if test="$rotated-width != ''">
                   <xsl:attribute name="width">
                     <xsl:value-of select="$rotated-width"/>
                   </xsl:attribute>
                 </xsl:if>
                 <xsl:copy-of select="$cell.content"/>
               </fo:block-container>
             </xsl:when>
             <xsl:otherwise>
               <xsl:copy-of select="$cell.content"/>
             </xsl:otherwise>
           </xsl:choose>
         </fo:table-cell>
        </xsl:otherwise>
      </xsl:choose>
      <!-- KDL: End edit -->
      <xsl:choose>
        <xsl:when test="following-sibling::entry|following-sibling::entrytbl">
          <xsl:apply-templates select="(following-sibling::entry
                                       |following-sibling::entrytbl)[1]">
            <xsl:with-param name="col" select="$col+$entry.colspan"/>
            <xsl:with-param name="spans" select="$following.spans"/>
          </xsl:apply-templates>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="finaltd">
            <xsl:with-param name="spans" select="$following.spans"/>
            <xsl:with-param name="col" select="$col+$entry.colspan"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- ====================================================== -->
<!--                 GLOSSARY FORMATTING                    -->
<!-- ====================================================== -->

<!-- Change inline formatting to use regular font instead of italics;
     use italics only for <firstterm> instances.
-->

<xsl:template match="glossterm" name="glossterm">
  <xsl:param name="firstterm" select="0"/>
  <xsl:choose>
    <xsl:when test="($firstterm.only.link = 0 or $firstterm = 1) and @linkend">
      <fo:basic-link internal-destination="{@linkend}"
                     xsl:use-attribute-sets="xref.properties">
        <xsl:choose>
          <xsl:when test="($firstterm = 1)">
            <xsl:call-template name="inline.italicseq"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="inline.charseq"/>
          </xsl:otherwise>
        </xsl:choose>
      </fo:basic-link>
    </xsl:when>
    <xsl:when test="not(@linkend)
                    and ($firstterm.only.link = 0 or $firstterm = 1)
                    and ($glossterm.auto.link != 0)
                    and $glossary.collection != ''">
      <xsl:variable name="term">
        <xsl:choose>
          <xsl:when test="@baseform"><xsl:value-of select="@baseform"/></xsl:when>
          <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="cterm"
           select="(document($glossary.collection,.)//glossentry[glossterm=$term])[1]"/>

      <xsl:choose>
        <xsl:when test="not($cterm)">
          <xsl:message>
            <xsl:text>There's no entry for </xsl:text>
            <xsl:value-of select="$term"/>
            <xsl:text> in </xsl:text>
            <xsl:value-of select="$glossary.collection"/>
          </xsl:message>
          <xsl:call-template name="inline.charseq"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="id">
            <xsl:choose>
              <xsl:when test="$cterm/@id">
                <xsl:value-of select="$cterm/@id"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="generate-id($cterm)"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <fo:basic-link internal-destination="{$id}"
                         xsl:use-attribute-sets="xref.properties">
            <xsl:choose>
              <xsl:when test="($firstterm = 1)">
                <xsl:call-template name="inline.italicseq"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="inline.charseq"/>
              </xsl:otherwise>
            </xsl:choose>
          </fo:basic-link>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="not(@linkend)
                    and ($firstterm.only.link = 0 or $firstterm = 1)
                    and $glossterm.auto.link != 0">
      <xsl:variable name="term">
        <xsl:choose>
          <xsl:when test="@baseform">
            <xsl:value-of select="@baseform"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="targets"
                    select="//glossentry[glossterm=$term or glossterm/@baseform=$term]"/>

      <xsl:variable name="target" select="$targets[1]"/>
      <xsl:choose>
        <xsl:when test="count($targets)=0">
          <xsl:message>
            <xsl:text>Error: no glossentry for glossterm: </xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>.</xsl:text>
          </xsl:message>
          <xsl:call-template name="inline.charseq"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="termid">
            <xsl:call-template name="object.id">
              <xsl:with-param name="object" select="$target"/>
            </xsl:call-template>
          </xsl:variable>
          <fo:basic-link internal-destination="{$termid}"
                         xsl:use-attribute-sets="xref.properties">
            <xsl:choose>
              <xsl:when test="($firstterm = 1)">
                <xsl:call-template name="inline.italicseq"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="inline.charseq"/>
              </xsl:otherwise>
            </xsl:choose>
          </fo:basic-link>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="inline.charseq"/>
    </xsl:otherwise>
  </xsl:choose>
  <!-- prints glossary terms in the body of the document -->
  <xsl:if test="$prox.review != 0">
    <fo:block font-size="8pt">
      <fo:inline color="green" font-style="italic">
        <xsl:choose>
          <xsl:when test="@baseform">
            <xsl:value-of select="@baseform"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </fo:inline>
    </fo:block>
  </xsl:if>
</xsl:template>

<!-- Make terms bold in the glossary -->

<xsl:template match="glossentry/glossterm" mode="glossary.as.blocks">
  <xsl:variable name="content">
    <xsl:apply-templates/>
  </xsl:variable>
  <xsl:call-template name="inline.boldseq">
    <xsl:with-param name="content" select="$content"/>
  </xsl:call-template>
  <xsl:if test="following-sibling::glossterm">, </xsl:if>
</xsl:template>

<!-- Header content -->
<!-- We add the date to the running header when in draft mode -->

<xsl:template name="header.content">
  <xsl:param name="pageclass" select="''"/>
  <xsl:param name="sequence" select="''"/>
  <xsl:param name="position" select="''"/>
  <xsl:param name="gentext-key" select="''"/>
  <!-- Specify no indent (otherwise outdented) -->
  <fo:block start-indent="0.0in">
    <!-- sequence can be odd, even, first, blank -->
    <!-- position can be left, center, right -->
    <xsl:choose>
      <xsl:when test="$sequence = 'blank'">
        <!-- nothing -->
      </xsl:when>
      <xsl:when test="$position='left'">
        <!-- Same for odd, even, empty, and blank sequences -->
        <xsl:call-template name="draft.text"/>
      </xsl:when>
      <xsl:when test="($sequence='odd' or $sequence='even') and $position='center'">
        <xsl:if test="$pageclass != 'titlepage'">
          <xsl:choose>
            <xsl:when test="ancestor::book and ($double.sided != 0)">
              <fo:retrieve-marker retrieve-class-name="section.head.marker"
                                  retrieve-position="first-including-carryover"
                                  retrieve-boundary="page-sequence"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="." mode="titleabbrev.markup"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
      </xsl:when>
      <xsl:when test="$position='center'">
        <!-- nothing for empty and blank sequences -->
      </xsl:when>
      <xsl:when test="$position='right'">
        <!-- Same for odd, even, empty, and blank sequences -->
        <!-- Print date if in draft mode -->
        <xsl:if test="$draft.mode = 'yes'">
          <xsl:call-template name="datetime.format">
             <xsl:with-param name="date" select="date:date-time()"/>  
             <xsl:with-param name="format" select="'m/d/Y'"/>  
          </xsl:call-template>
      </xsl:if>
      </xsl:when>
      <xsl:when test="$sequence = 'first'">
        <!-- nothing for first pages -->
      </xsl:when>
      <xsl:when test="$sequence = 'blank'">
        <!-- nothing for blank pages -->
      </xsl:when>
    </xsl:choose>
  </fo:block>
</xsl:template>

<!-- ====================================================== -->
<!--                   INLINE FORMATTING                    -->
<!-- ====================================================== -->

<!-- Define a new sans-serif attribute set -->

<xsl:attribute-set name="sans-serif.properties">
  <xsl:attribute name="font-family">
    <xsl:value-of select="$sans.font.family"/>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="sans-serif.inline.properties">
  <xsl:attribute name="font-family">
    <xsl:value-of select="$sans.font.family"/>
  </xsl:attribute>
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 0.9"></xsl:value-of>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="sans-serif.inline.tiny.properties">
  <xsl:attribute name="font-family">
    <xsl:value-of select="$sans.font.family"/>
  </xsl:attribute>
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 0.7"></xsl:value-of>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:template name="inline.sansseq">
  <xsl:param name="content">
    <xsl:apply-templates/>
  </xsl:param>
  <fo:inline xsl:use-attribute-sets="sans-serif.inline.properties">
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

<xsl:template name="inline.sansseq.tiny">
  <xsl:param name="content">
    <xsl:apply-templates/>
  </xsl:param>
  <fo:inline xsl:use-attribute-sets="sans-serif.inline.tiny.properties">
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

<!-- Define a new comment (red, italics) template -->

<!-- Used when setting role="edcomment" in <phrase> elements.
     The edited template for <phrase> is in prox-common.xsl -->

<xsl:template name="inline.edcomment">
  <xsl:param name="content">
    <xsl:apply-templates/>
  </xsl:param>
  <fo:inline font-style="italic" color="red">
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

<!-- More formatting attribute sets -->

<xsl:attribute-set name="monospace.inline.properties">
  <xsl:attribute name="font-family">
    <xsl:value-of select="$monospace.font.family"/>
  </xsl:attribute>
  <xsl:attribute name="font-size">
    <xsl:value-of select="$body.font.master * 0.9"></xsl:value-of>
    <xsl:text>pt</xsl:text>
  </xsl:attribute>
</xsl:attribute-set>

<!-- changes the default size of inline monospaced text to be slightly
     smaller -->

<xsl:template name="inline.monoseq">
  <xsl:param name="content">
    <xsl:apply-templates/>
  </xsl:param>
  <fo:inline xsl:use-attribute-sets="monospace.inline.properties">
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<!-- so we also have a version that doesn't reset the default size
     that we can use in heaadings -->

<xsl:template name="inline.monoseq.notsmaller">
  <xsl:param name="content">
    <xsl:apply-templates/>
  </xsl:param>
  <fo:inline xsl:use-attribute-sets="monospace.properties">
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<!-- and we do the same for italic versions of these properties -->

<xsl:template name="inline.italicmonoseq">
  <xsl:param name="content">
    <xsl:apply-templates/>
  </xsl:param>
  <fo:inline font-style="italic" xsl:use-attribute-sets="monospace.inline.properties">
    <xsl:if test="@id">
      <xsl:attribute name="id">
        <xsl:value-of select="@id"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<!-- here's the italic version that doesn't reset the default size
     that we can use in heaadings -->

<xsl:template name="inline.italicmonoseq.notsmaller">
  <xsl:param name="content">
    <xsl:apply-templates/>
  </xsl:param>
  <fo:inline font-style="italic" xsl:use-attribute-sets="monospace.properties">
    <xsl:if test="@id">
      <xsl:attribute name="id">
        <xsl:value-of select="@id"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<!-- this is kind of kludgy, but it seems to work for now.  We use a
     "heading" role to indicate when inline markup is used in a
     heading, so the template uses the correct font size.  To do so,
     we have to add a "role" attribute to each of the corresponding
     templates.  Would be better to have a more general solution, but
     that's proved elusive for now. -->

<xsl:template match="code">
  <xsl:choose>
    <xsl:when test="@role='heading'">
      <xsl:call-template name="inline.monoseq.notsmaller"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="inline.monoseq"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="varname">
  <xsl:choose>
    <xsl:when test="@role='heading'">
      <xsl:call-template name="inline.italicmonoseq.notsmaller"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="inline.italicmonoseq"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- handle, e.g., 
       <symbol role="ZapfDingbats">&entity;</symbol>
     in text 
-->

<xsl:template match="symbol[@role = 'ZapfDingbats']">
  <fo:inline font-family="ZapfDingbats">
    <xsl:apply-templates/>
  </fo:inline>
</xsl:template>

<xsl:template match="procedure/step|substeps/step">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>
  <fo:list-item xsl:use-attribute-sets="list.item.spacing">
    <fo:list-item-label end-indent="label-end()">
      <fo:block id="{$id}">
        <!-- dwc: fix for one step procedures. Use a bullet if there's no step 2 -->
        <xsl:choose>
          <xsl:when test="count(../step) = 1">
            <!-- originally was: <xsl:text>&#x2022;</xsl:text> -->
            <xsl:element name="fo:inline">
              <xsl:attribute name="font-family">
                <xsl:text>ZapfDingbats</xsl:text>
              </xsl:attribute>
              <xsl:text>&#x27A4;</xsl:text>
            </xsl:element>
            <!-- end alteration -->
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="." mode="number">
              <xsl:with-param name="recursive" select="0"/>
            </xsl:apply-templates>.
          </xsl:otherwise>
        </xsl:choose>
      </fo:block>
    </fo:list-item-label>
    <fo:list-item-body start-indent="body-start()">
      <fo:block>
        <xsl:apply-templates/>
      </fo:block>
    </fo:list-item-body>
  </fo:list-item>
</xsl:template>

<!-- ====================================================== -->
<!--         CLASSNAME and METHODNAME path handling         -->
<!-- ====================================================== -->

<!-- We use the "role" attribute to generate different versions of the
     Proximity documentation based on the intended distribution
     location.  We currently create documentation that gets included
     in the release tarfile ('distrib'), placed on the external,
     public KDL web pages ('web'), and placed on an internal kdl
     server to provide updates between releases ('kdl').  Each of
     these requires  a different path to the Javadoc files.

     For now, the three options produce identical results.  This is
     because I've been unable to find a way to link from a PDF file to
     an HTML file using relative links. (Some quick web searches seem
     to confirm this limitation, but not definitively.)  The options
     thus serve as placeholders for future modification that might let
     us use realtive links in this manner.
-->

<xsl:template match="classname">
  <!-- for some reason, I need to have 2 tests.  First to see if
       there's a value for the role attr, then to determine output
       based on that value -->
  <xsl:choose>
    <xsl:when test="@role">
      <xsl:choose>
        <xsl:when test="$prox.destination='distrib'">
          <fo:basic-link external-destination="http://kdl.cs.umass.edu/proximity/documentation/{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </fo:basic-link>
        </xsl:when>
        <xsl:when test="$prox.destination='web'">
          <fo:basic-link external-destination="http://kdl.cs.umass.edu/proximity/documentation/{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </fo:basic-link>
        </xsl:when>
        <xsl:when test="$prox.destination='kdl'">
          <fo:basic-link external-destination="http://kdl.cs.umass.edu/proximity/documentation/{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </fo:basic-link>
        </xsl:when>
        <xsl:otherwise>
          <!--
          <xsl:message>In classname: other</xsl:message>
          -->
          <xsl:call-template name="inline.monoseq"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="inline.monoseq"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="methodname">
  <!-- only create a link if there's a path (in the role attr) -->
  <xsl:choose>
    <xsl:when test="@role">
      <!--
      <xsl:message>methodname: <xsl:value-of select="."/></xsl:message>
      -->
      <xsl:choose>
        <xsl:when test="$prox.destination='distrib'">
          <fo:basic-link external-destination="http://kdl.cs.umass.edu/proximity/documentation/{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </fo:basic-link>
        </xsl:when>
        <xsl:when test="$prox.destination='web'">
          <fo:basic-link external-destination="http://kdl.cs.umass.edu/proximity/documentation/{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </fo:basic-link>
        </xsl:when>
        <xsl:when test="$prox.destination='kdl'">
          <fo:basic-link external-destination="http://kdl.cs.umass.edu/proximity/documentation/{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </fo:basic-link>
        </xsl:when>
        <xsl:otherwise>
            <xsl:call-template name="inline.monoseq"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <!--
      <xsl:message>non-linked method: <xsl:value-of select="."/></xsl:message>
      -->
      <xsl:call-template name="inline.monoseq"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ====================================================== -->
<!--                    TOOLS FOR EDITING                   -->
<!-- ====================================================== -->

<!-- These templates are used when the parameter prox.review is set
     to 1 (any non-zero value, actually).  The genproxdoc.sh script
     uses the "-r" switch to set this parameter. -->

<!-- Display glossary terms in green -->
<!-- See end of glossterm template, above -->

<!-- Display index term in small, red font -->

<xsl:template match="indexterm" name="indexterm">
  <!-- Temporal workaround for bug in AXF -->
  <xsl:variable name="wrapper.name">
    <xsl:choose>
      <xsl:when test="$axf.extensions != 0">fo:block</xsl:when>
      <xsl:otherwise>fo:wrapper</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:element name="{$wrapper.name}">
    <xsl:attribute name="id">
      <xsl:call-template name="object.id"/>
    </xsl:attribute>
    <xsl:choose>
      <xsl:when test="$xep.extensions != 0">
        <xsl:attribute name="rx:key">
          <xsl:value-of select="&primary;"/>
          <xsl:if test="@significance='preferred'"><xsl:value-of select="$significant.flag"/></xsl:if>
          <xsl:if test="secondary">
            <xsl:text>, </xsl:text>
            <xsl:value-of select="&secondary;"/>
          </xsl:if>
          <xsl:if test="tertiary">
            <xsl:text>, </xsl:text>
            <xsl:value-of select="&tertiary;"/>
          </xsl:if>
        </xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:comment>
          <xsl:call-template name="comment-escape-string">
            <xsl:with-param name="string">
              <xsl:value-of select="primary"/>
              <xsl:if test="secondary">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="secondary"/>
              </xsl:if>
              <xsl:if test="tertiary">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="tertiary"/>
              </xsl:if>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:comment>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:element>
  <!-- prints index terms in the body of the document -->
  <xsl:if test="$prox.review != 0">
    <fo:block font-size="8pt">
      <fo:inline color="red">
          <xsl:value-of select="primary"/>
          <xsl:if test="secondary">
          , <xsl:value-of select="secondary"/>
          </xsl:if>
      </fo:inline>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="imagedata">
  <xsl:variable name="vendor" select="system-property('xsl:vendor')"/>
  <xsl:variable name="filename">
    <xsl:call-template name="mediaobject.filename">
      <xsl:with-param name="object" select=".."/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="@format='linespecific'">
      <xsl:choose>
        <xsl:when test="$use.extensions != '0'
                        and $textinsert.extension != '0'">
          <xsl:choose>
            <xsl:when test="contains($vendor, 'SAXON')">
              <stext:insertfile href="{$filename}" encoding="{$textdata.default.encoding}"/>
            </xsl:when>
            <xsl:when test="contains($vendor, 'Apache Software Foundation')">
              <xtext:insertfile href="{$filename}"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:message terminate="yes">
                <xsl:text>Don't know how to insert files with </xsl:text>
                <xsl:value-of select="$vendor"/>
              </xsl:message>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <a xlink:type="simple" xlink:show="embed" xlink:actuate="onLoad"
             href="{$filename}"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="process.image"/>
    </xsl:otherwise>
  </xsl:choose>
  <!-- prints image file names underneath the image -->
  <xsl:if test="$prox.review != 0">
    <fo:block text-align="center" font-size="8pt">
      <fo:inline color="red" font-style="italic">
          <xsl:value-of select="$filename"/>
      </fo:inline>
    </fo:block>
  </xsl:if>
</xsl:template>

<!-- uncomment and use "-imagelist" switch and PDF generation to
     generate list of image files used in this document; list is
     written to STDOUT -->

<!--
<xsl:template match="/">
  <xsl:if test="$prox.imagelist != 0">
    <xsl:for-each select="//imagedata">
      <xsl:sort select="@fileref" />
      <xsl:message>
        <xsl:value-of select="@fileref"/>
      </xsl:message>
    </xsl:for-each>
  </xsl:if>
</xsl:template>
-->


<!-- ************************************************************
     TEST - TEST - TEST - TEST - TEST - TEST - TEST - TEST - TEST
     ************************************************************ 
-->

<!-- A revised version of the process.image template designed to
     correctly handle valign attribute for images.  This was designed
     for FOP ).93, however.  FOP 0.20.5 does not support this
     attribute, so this template is held here for future reference
     once/if we change to FOP 0.93. (Does not seem to adversely affect
     processing under FOP 0.20.5.) -->

<xsl:template name="process.image">
    <xsl:variable name="scalefit">
      <xsl:choose>
    <xsl:when test="$ignore.image.scaling != 0">0</xsl:when>
    <xsl:when test="@contentwidth">0</xsl:when>
    <xsl:when test="@contentdepth and
            @contentdepth != '100%'">0</xsl:when>
    <xsl:when test="@scale">0</xsl:when>
    <xsl:when test="@scalefit"><xsl:value-of select="@scalefit"/></xsl:when>
    <xsl:when test="@width or @depth">1</xsl:when>
    <xsl:otherwise>0</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="scale">
      <xsl:choose>
    <xsl:when test="$ignore.image.scaling != 0">0</xsl:when>
    <xsl:when test="@contentwidth or @contentdepth">1.0</xsl:when>
    <xsl:when test="@scale">
      <xsl:value-of select="@scale div 100.0"/>
    </xsl:when>
    <xsl:otherwise>1.0</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="filename">
      <xsl:choose>
    <xsl:when test="local-name(.) = 'graphic'
            or local-name(.) = 'inlinegraphic'">
      <!-- handle legacy graphic and inlinegraphic by new template -->
      <xsl:call-template name="mediaobject.filename">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <!-- imagedata, videodata, audiodata -->
      <xsl:call-template name="mediaobject.filename">
        <xsl:with-param name="object" select=".."/>
      </xsl:call-template>
    </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="content-type">
      <xsl:if test="@format">
    <xsl:call-template name="graphic.format.content-type">
      <xsl:with-param name="format" select="@format"/>
    </xsl:call-template>
      </xsl:if>
    </xsl:variable>
    <xsl:variable name="bgcolor">
      <xsl:call-template name="dbfo-attribute">
    <xsl:with-param name="pis"
            select="../processing-instruction('dbfo')"/>
    <xsl:with-param name="attribute" select="'background-color'"/>
      </xsl:call-template>
    </xsl:variable>
    <fo:external-graphic>
      <xsl:attribute name="src">
    <xsl:call-template name="fo-external-image">
      <xsl:with-param name="filename">
        <xsl:if test="$img.src.path != '' and
              not(starts-with($filename, '/')) and
              not(contains($filename, '://'))">
          <xsl:value-of select="$img.src.path"/>
        </xsl:if>
        <xsl:value-of select="$filename"/>
      </xsl:with-param>
    </xsl:call-template>
      </xsl:attribute>
      <xsl:attribute name="width">
    <xsl:choose>
      <xsl:when test="$ignore.image.scaling != 0">auto</xsl:when>
      <xsl:when test="contains(@width,'%')">
        <xsl:value-of select="@width"/>
      </xsl:when>
      <xsl:when test="@width and not(@width = '')">
        <xsl:call-template name="length-spec">
          <xsl:with-param name="length" select="@width"/>
          <xsl:with-param name="default.units" select="'px'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="not(@depth) and $default.image.width != ''">
        <xsl:call-template name="length-spec">
          <xsl:with-param name="length" select="$default.image.width"/>
          <xsl:with-param name="default.units" select="'px'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>auto</xsl:otherwise>
    </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="height">
    <xsl:choose>
      <xsl:when test="$ignore.image.scaling != 0">auto</xsl:when>
      <xsl:when test="contains(@depth,'%')">
        <xsl:value-of select="@depth"/>
      </xsl:when>
      <xsl:when test="@depth">
        <xsl:call-template name="length-spec">
          <xsl:with-param name="length" select="@depth"/>
          <xsl:with-param name="default.units" select="'px'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>auto</xsl:otherwise>
    </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="content-width">
    <xsl:choose>
      <xsl:when test="$ignore.image.scaling != 0">auto</xsl:when>
      <xsl:when test="contains(@contentwidth,'%')">
        <xsl:value-of select="@contentwidth"/>
      </xsl:when>
      <xsl:when test="@contentwidth">
        <xsl:call-template name="length-spec">
          <xsl:with-param name="length" select="@contentwidth"/>
          <xsl:with-param name="default.units" select="'px'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="number($scale) != 1.0">
        <xsl:value-of select="$scale * 100"/>
        <xsl:text>%</xsl:text>
      </xsl:when>
      <xsl:when test="$scalefit = 1">scale-to-fit</xsl:when>
      <xsl:otherwise>auto</xsl:otherwise>
    </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="content-height">
    <xsl:choose>
      <xsl:when test="$ignore.image.scaling != 0">auto</xsl:when>
      <xsl:when test="contains(@contentdepth,'%')">
        <xsl:value-of select="@contentdepth"/>
      </xsl:when>
      <xsl:when test="@contentdepth">
        <xsl:call-template name="length-spec">
          <xsl:with-param name="length" select="@contentdepth"/>
          <xsl:with-param name="default.units" select="'px'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="number($scale) != 1.0">
        <xsl:value-of select="$scale * 100"/>
        <xsl:text>%</xsl:text>
      </xsl:when>
      <xsl:when test="$scalefit = 1">scale-to-fit</xsl:when>
      <xsl:otherwise>auto</xsl:otherwise>
    </xsl:choose>
      </xsl:attribute>
      <xsl:if test="$content-type != ''">
    <xsl:attribute name="content-type">
      <xsl:value-of select="concat('content-type:',$content-type)"/>
    </xsl:attribute>
      </xsl:if>
      <xsl:if test="$bgcolor != ''">
    <xsl:attribute name="background-color">
      <xsl:value-of select="$bgcolor"/>
    </xsl:attribute>
      </xsl:if>
      <xsl:if test="@align">
    <xsl:attribute name="text-align">
      <xsl:value-of select="@align"/>
    </xsl:attribute>
      </xsl:if>
      <xsl:if test="@valign">
    <xsl:attribute name="vertical-align">
      <xsl:choose>
        <xsl:when test="@valign = 'top'">super</xsl:when>
        <xsl:when test="@valign = 'middle'">middle</xsl:when>
        <xsl:when test="@valign = 'bottom'">sub</xsl:when>
        <xsl:otherwise>auto</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
      </xsl:if>
    </fo:external-graphic>
</xsl:template>

<!-- End stylesheet -->

</xsl:stylesheet>
