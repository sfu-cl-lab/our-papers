<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exsl="http://exslt.org/common"
                version='1.0'>

<!-- Part of the open-source Proximity system (see LICENSE for
     copyright and license information).
-->

<!-- Style sheet for HTML version of Proximity Tutorial     -->
<!-- ====================================================== -->

<!-- ====================================================== -->
<!-- Load other style sheets -->

<!-- Set this path to point to your docbook-xsl style sheets -->
<xsl:import href="/usr/local/sgml/docbook-xsl-1.71.1/html/chunk.xsl"/>

<!-- Load common style sheet definitions -->
<xsl:include href="prox-common.xsl"/>

<!-- ====================================================== -->
<!-- DocBook XSL Parameters -->

<!-- HTML table generation tends to work better with
     tablecolumns.extensions turned on -->
<xsl:param name="tablecolumns.extension" select="'1'"></xsl:param>
<xsl:param name="chunker.output.indent" select="'yes'"/>
<xsl:param name="chunk.section.depth" select="2"/>
<xsl:param name="chunk.first.sections" select="0"></xsl:param>
<!-- now passed in from genproxdoc.sh script
  <xsl:param name="base.dir" select="'HTML/'"/>
-->
<xsl:param name="toc.max.depth">2</xsl:param>
<xsl:param name="preferred.mediaobject.role" select="'html'"></xsl:param>
<xsl:param name="html.stylesheet" select="'prox.css'"></xsl:param>

<!-- ====================================================== -->
<!-- Proximity parameters -->

<!-- Default value is 'distrib'; can be changed by genproxdoc.sh
     script -->
<xsl:param name="prox.destination" select="distrib"></xsl:param>

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

<xsl:param name="proxhtml.l10n.xml" select="document('')"/> 
<l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0"> 
  <l:l10n language="en"> 
     <l:context name="xref">
     <!-- These defns are used for crossrefs to non-numbered elements -->
     </l:context>
     <l:context name="xref-number">
     <!-- These defns are used when xref.with.number.and.title = 0 -->
     </l:context>
     <l:context name="xref-number-and-title">
     <!-- These defns are used when xref.with.number.and.title = 1 -->
     </l:context>
  </l:l10n>
</l:i18n>

<xsl:param name="local.l10n.merged">
  <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
    <l:l10n language="en">
      <xsl:copy-of
           select="$proxhtml.l10n.xml//l:i18n/l:l10n[@language='en']/*"/>
      <xsl:copy-of
           select="$proxcommon.l10n.xml//l:i18n/l:l10n[@language='en']/*"/>
    </l:l10n>
  </l:i18n>
</xsl:param>

<xsl:param name="local.l10n.xml"
           select="exsl:node-set($local.l10n.merged)"/>


<!-- Specify front matter to include -->

<xsl:param name="generate.toc">
article   nop
book      toc,title,procedure
chapter   toc,title,procedure
</xsl:param>

<!-- ========================================================= -->
<!-- Page breaks are no-ops for HTML  -->

<xsl:template match="processing-instruction('pagebreak')">
</xsl:template>

<!-- But line breaks should be obeyed -->

<xsl:template match="processing-instruction('linebreak')">
  <BR/>
</xsl:template>

<!-- ====================================================== -->
<!-- Add copyright statement to generated HTML files -->

<xsl:template name="system.head.content">
  <xsl:param name="node" select="."/>
      <xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
      <xsl:text> Part of the open-source Proximity system (see LICENSE for
           copyright and license information). </xsl:text>
     <xsl:text disable-output-escaping="yes">--&gt;</xsl:text>
</xsl:template>

<!-- Format e-mail addresses -->

<xsl:template match="email">
  <xsl:call-template name="inline.italicseq">
    <xsl:with-param name="content">
      <a>
       <xsl:attribute name="href">mailto:<xsl:value-of select="."/></xsl:attribute>
       <xsl:apply-templates/>
      </a>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<!-- Sidebars -->
<!-- Format with pale blue background -->

<xsl:attribute-set name="shade.sidebar.style">
  <xsl:attribute name="border">0</xsl:attribute>
  <xsl:attribute name="bgcolor">#CCCCFF</xsl:attribute>
</xsl:attribute-set>

<xsl:template match="sidebar">
  <div class="{name(.)}">
    <xsl:call-template name="anchor"/>
    <xsl:variable name="content">
      <xsl:apply-templates/>
    </xsl:variable>
      <table xsl:use-attribute-sets="shade.sidebar.style">
        <tr>
          <td>
            <xsl:copy-of select="$content"/>
          </td>
        </tr>
      </table>
  </div>
</xsl:template>

<!-- in progress

<xsl:template match="important[role]">
  <xsl:choose>
    <xsl:when test="@role='modified'">
      <div class="{name(.)}"> 
         <xsl:apply-templates/>
      </div>
    </xsl:when>
  </xsl:choose>
</xsl:template>

-->

<!-- Formal paragraph title format -->
<!-- Default is to have a period at the end of the title.  Omit
     the period if the paragraph title element has a role attribute
     value of "header". -->

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
  <b>
    <xsl:copy-of select="$titleStr"/>
      <xsl:if test="$titleRole != 'header' and $lastChar != ''
                    and not(contains($runinhead.title.end.punct, $lastChar))">
        <xsl:value-of select="$runinhead.default.title.end.punct"/>
      </xsl:if>
    <xsl:text>&#160;</xsl:text>
  </b>
</xsl:template>

<!-- =================== GLOSSARY FORMATTING =================== -->

<!-- Only make <firstterm> elements in italics; regular <glossterm>
     elements use the regular font. -->
<!-- Note: This works, but generates spurious warnings stating that
     there are no automatic glossaries.  (The error messages appear if
     we simply copy the existing template definition without
     modification so it doesn't seem to be the modifications that
     produce the errors.  We comment out the sections that produce the
     error messages (and add -w0 to call to Saxon to suppress
     recoverable error messages). -->

<xsl:template match="glossterm" name="glossterm">
  <xsl:param name="firstterm" select="0"/>
  <!-- To avoid extra <a name=""> anchor from inline.italicseq -->
  <xsl:variable name="content">
    <xsl:apply-templates/>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="($firstterm.only.link = 0 or $firstterm = 1) and @linkend">
      <xsl:variable name="targets" select="key('id',@linkend)"/>
      <xsl:variable name="target" select="$targets[1]"/>
      <xsl:call-template name="check.id.unique">
        <xsl:with-param name="linkend" select="@linkend"/>
      </xsl:call-template>
      <a>
        <xsl:if test="@id">
          <xsl:attribute name="name">
            <xsl:value-of select="@id"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="href">
          <xsl:call-template name="href.target">
            <xsl:with-param name="object" select="$target"/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:choose>
          <xsl:when test="($firstterm = 1)">
            <xsl:call-template name="inline.italicseq">
              <xsl:with-param name="content" select="$content"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="inline.charseq">
              <xsl:with-param name="content" select="$content"/>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </a>
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
      <!-- HACK HACK HACK! But it works... -->
      <!-- You'd need to do more work if you wanted to chunk on glossdiv, though -->
      <xsl:variable name="glossary"
      select="//glossary[@role='auto']"/>
<!-- KDL: comment out spurious error messages
      <xsl:if test="count($glossary) != 1">
        <xsl:message>
          <xsl:text>Warning: glossary.collection specified, but there are </xsl:text>
          <xsl:value-of select="count($glossary)"/>
          <xsl:text> automatic glossaries</xsl:text>
        </xsl:message>
      </xsl:if>
-->
      <xsl:variable name="glosschunk">
        <xsl:call-template name="href.target">
          <xsl:with-param name="object" select="$glossary"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="chunkbase">
        <xsl:choose>
          <xsl:when test="contains($glosschunk, '#')">
            <xsl:value-of select="substring-before($glosschunk, '#')"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$glosschunk"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="not($cterm)">
<!-- KDL: comment out spurious error messages
          <xsl:message>
            <xsl:text>There's no entry for </xsl:text>
            <xsl:value-of select="$term"/>
            <xsl:text> in </xsl:text>
            <xsl:value-of select="$glossary.collection"/>
          </xsl:message>
-->
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
          <a href="{$chunkbase}#{$id}">
            <xsl:choose>
              <xsl:when test="($firstterm = 1)">
                <xsl:call-template name="inline.italicseq">
                  <xsl:with-param name="content" select="$content"/>
                </xsl:call-template>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="inline.charseq">
                  <xsl:with-param name="content" select="$content"/>
                </xsl:call-template>
              </xsl:otherwise>
            </xsl:choose>
          </a>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="not(@linkend)
                    and ($firstterm.only.link = 0 or $firstterm = 1)
                    and $glossterm.auto.link != 0">
      <xsl:variable name="term">
        <xsl:choose>
          <xsl:when test="@baseform">
            <xsl:value-of select="normalize-space(@baseform)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="normalize-space(.)"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="targets"
                    select="//glossentry[normalize-space(glossterm)=$term
                              or normalize-space(glossterm/@baseform)=$term]"/>
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
          <a>
            <xsl:if test="@id">
              <xsl:attribute name="name">
                <xsl:value-of select="@id"/>
              </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="href">
              <xsl:call-template name="href.target">
                <xsl:with-param name="object" select="$target"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:choose>
              <xsl:when test="($firstterm = 1)">
                <xsl:call-template name="inline.italicseq">
                  <xsl:with-param name="content" select="$content"/>
                </xsl:call-template>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="inline.charseq">
                  <xsl:with-param name="content" select="$content"/>
                </xsl:call-template>
              </xsl:otherwise>
            </xsl:choose>
          </a>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="inline.charseq"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Terms in the glossary are formatted in boldface -->

<xsl:template match="glossentry">
  <xsl:choose>
    <xsl:when test="$glossentry.show.acronym = 'primary'">
      <dt><b>
        <xsl:call-template name="anchor">
          <xsl:with-param name="conditional">
            <xsl:choose>
              <xsl:when test="$glossterm.auto.link != 0">0</xsl:when>
              <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:call-template>
        <xsl:choose>
          <xsl:when test="acronym|abbrev">
            <xsl:apply-templates select="acronym|abbrev"/>
            <xsl:text> (</xsl:text>
            <xsl:apply-templates select="glossterm"/>
            <xsl:text>)</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="glossterm"/>
          </xsl:otherwise>
        </xsl:choose>
      </b></dt>
    </xsl:when>
    <xsl:when test="$glossentry.show.acronym = 'yes'">
      <dt><b>
        <xsl:call-template name="anchor">
          <xsl:with-param name="conditional">
            <xsl:choose>
              <xsl:when test="$glossterm.auto.link != 0">0</xsl:when>
              <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:call-template>
        <xsl:apply-templates select="glossterm"/>
        <xsl:if test="acronym|abbrev">
          <xsl:text> (</xsl:text>
          <xsl:apply-templates select="acronym|abbrev"/>
          <xsl:text>)</xsl:text>
        </xsl:if>
      </b></dt>
    </xsl:when>
    <xsl:otherwise>
      <dt><b>
        <xsl:call-template name="anchor">
          <xsl:with-param name="conditional">
            <xsl:choose>
              <xsl:when test="$glossterm.auto.link != 0">0</xsl:when>
              <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:call-template>
        <xsl:apply-templates select="glossterm"/>
      </b></dt>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates select="indexterm|revhistory|glosssee|glossdef"/>
</xsl:template>

<!-- ====================================================== -->
<!--                         CAPTIONS                       -->
<!-- ====================================================== -->

<!-- Add a choose statement to distinguish figure captions from other
     formal object headings -->

<xsl:template name="formal.object.heading">
  <xsl:param name="object" select="."/>
  <xsl:choose>
    <xsl:when test="local-name(.) = 'figure'">
      <p class="title" align="center">
        <b>
          <xsl:apply-templates select="$object" mode="object.title.markup">
            <xsl:with-param name="allow-anchors" select="1"/>
          </xsl:apply-templates>
        </b>
      </p>
    </xsl:when>
    <xsl:otherwise>
      <p class="title">
        <b>
          <xsl:apply-templates select="$object" mode="object.title.markup">
            <xsl:with-param name="allow-anchors" select="1"/>
          </xsl:apply-templates>
        </b>
      </p>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ====================================================== -->
<!--                      PROCEDURES                        -->
<!-- ====================================================== -->

<!-- 
Start of modification for one-step procedure to use a different bullet
symbol. Best guess for proceeding is to use a "style" attribute on the
<ul> tag and CSS to change the bullet to the desired arrow.  For
future development (currently commented out).

Note that some internal comments have been altered so as to avoid
parser problems. These will need to be fixed before using this code
section.

<xsl:template match="procedure">
  <xsl:variable name="param.placement"
                select="substring-after(normalize-space($formal.title.placement),
                                        concat(local-name(.), ' '))"/>
  <xsl:variable name="placement">
    <xsl:choose>
      <xsl:when test="contains($param.placement, ' ')">
        <xsl:value-of select="substring-before($param.placement, ' ')"/>
      </xsl:when>
      <xsl:when test="$param.placement = ''">before</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$param.placement"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <!- - Preserve order of PIs and comments - ->
  <xsl:variable name="preamble"
        select="*[not(self::step
                  or self::title
                  or self::titleabbrev)]
                |comment()[not(preceding-sibling::step)]
                |processing-instruction()[not(preceding-sibling::step)]"/>
  <div class="{name(.)}">
    <xsl:call-template name="anchor">
      <xsl:with-param name="conditional">
        <xsl:choose>
	  <xsl:when test="title">0</xsl:when>
	  <xsl:otherwise>1</xsl:otherwise>
	</xsl:choose>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:if test="title and $placement = 'before'">
      <xsl:call-template name="formal.object.heading"/>
    </xsl:if>
    <xsl:apply-templates select="$preamble"/>
    <xsl:choose>
      <xsl:when test="count(step) = 1">
        <ul>
          <xsl:apply-templates mode="onestep"
            select="step
                    |comment()[preceding-sibling::step]
                    |processing-instruction()[preceding-sibling::step]"/>
        </ul>
      </xsl:when>
      <xsl:otherwise>
        <ol>
          <xsl:attribute name="type">
            <xsl:value-of select="substring($procedure.step.numeration.formats,1,1)"/>
          </xsl:attribute>
          <xsl:apply-templates 
            select="step
                    |comment()[preceding-sibling::step]
                    |processing-instruction()[preceding-sibling::step]"/>
        </ol>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="title and $placement != 'before'">
      <xsl:call-template name="formal.object.heading"/>
    </xsl:if>
  </div>
</xsl:template>

<xsl:template match="step" mode="onestep">
  <img src="images/onestep.png"/>
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
</xsl:template>

End commented out section
-->

<!-- ====================================================== -->
<!--                   INLINE FORMATTING                    -->
<!-- ====================================================== -->

<xsl:template name="inline.sansseq">
  <xsl:param name="content">
    <xsl:call-template name="anchor"/>
    <xsl:call-template name="simple.xlink">
      <xsl:with-param name="content">
        <xsl:apply-templates/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:param>
  <span class="{local-name(.)}" style="font-family: sans-serif">
    <xsl:if test="@dir">
      <xsl:attribute name="dir">
        <xsl:value-of select="@dir"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </span>
</xsl:template>

<!-- The inline.sansseq.tiny template is the same as the regular
     inline.sansseq template, but we need to have a separate one
     because the <token> template calls this if we use 
     <token role="attr"> in a footnote (for formatting in PDF).
-->

<xsl:template name="inline.sansseq.tiny">
  <xsl:param name="content">
    <xsl:call-template name="anchor"/>
    <xsl:call-template name="simple.xlink">
      <xsl:with-param name="content">
        <xsl:apply-templates/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:param>
  <span class="{local-name(.)}" style="font-family: sans-serif">
    <xsl:if test="@dir">
      <xsl:attribute name="dir">
        <xsl:value-of select="@dir"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </span>
</xsl:template>

<!-- Used when setting role="edcomment" in <phrase> elements.
     The edited template for <phrase> is in prox-common.xsl -->

<xsl:template name="inline.edcomment">
  <xsl:param name="content">
    <xsl:call-template name="anchor"/>
    <xsl:call-template name="simple.xlink">
      <xsl:with-param name="content">
        <xsl:apply-templates/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:param>
  <span class="{local-name(.)}" style="font-style: italic; color: red">
    <xsl:if test="@dir">
      <xsl:attribute name="dir">
        <xsl:value-of select="@dir"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </span>
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
     these requires a different path to the Javadoc files.
-->

<xsl:template match="classname">
  <!-- for some reason, I need to have 2 tests.  First to see if
       there's a value for the role attr, then to determine output
       based on that value -->
  <xsl:choose>
    <xsl:when test="@role">
      <!--
      <xsl:message>In classname:</xsl:message>
      <xsl:message>  docdestination: <xsl:value-of select="$prox.destination"/></xsl:message>
      -->
      <xsl:choose>
        <xsl:when test="$prox.destination='distrib'">
          <a href="../../../../{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </a>
        </xsl:when>
        <xsl:when test="$prox.destination='web'">
          <a href="../{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </a>
        </xsl:when>
        <xsl:when test="$prox.destination='kdl'">
          <a href="../../{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </a>
        </xsl:when>
        <xsl:when test="$prox.destination='blog'">
          <a href="http://kdl.cs.umass.edu/proximity/documentation/{@role}">
          <xsl:call-template name="inline.monoseq"/>
          </a>
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
      <xsl:message>docdestination: <xsl:value-of select="$prox.destination"/></xsl:message>
      -->
      <xsl:choose>
        <xsl:when test="$prox.destination='distrib'">
            <a href="../../../../{@role}">
            <xsl:call-template name="inline.monoseq"/>
            </a>
        </xsl:when>
        <xsl:when test="$prox.destination='web'">
            <a href="../{@role}">
            <xsl:call-template name="inline.monoseq"/>
            </a>
        </xsl:when>
        <xsl:when test="$prox.destination='kdl'">
            <a href="../../{@role}">
            <xsl:call-template name="inline.monoseq"/>
            </a>
        </xsl:when>
        <xsl:when test="$prox.destination='blog'">
            <a href="http://kdl.cs.umass.edu/proximity/documentation/{@role}">
            <xsl:call-template name="inline.monoseq"/>
            </a>
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
<!--                       PAGE LAYOUT                      -->
<!-- ====================================================== -->

<!-- Remove top line from generated header.  This simply repeats the 
     root heading for the page and looks redundant.
-->

<xsl:template name="header.navigation">
  <xsl:param name="prev" select="/foo"/>
  <xsl:param name="next" select="/foo"/>
  <xsl:param name="nav.context"/>

  <xsl:variable name="home" select="/*[1]"/>
  <xsl:variable name="up" select="parent::*"/>

<!-- orig row 1
  <xsl:variable name="row1" select="$navig.showtitles != 0"/>
-->
<!-- new row 1 -->
  <xsl:variable name="row1" select="($prev and $navig.showtitles != 0)
                                    or (generate-id($home) != generate-id(.)
                                        or $nav.context = 'toc')
                                    or ($chunk.tocs.and.lots != 0
                                        and $nav.context != 'toc')
                                    or ($next and $navig.showtitles != 0)"/>
<!-- new row 1 ends here -->

  <xsl:variable name="row2" select="count($prev) &gt; 0
                                    or (count($up) &gt; 0 
                                        and generate-id($up) != generate-id($home)
                                        and $navig.showtitles != 0)
                                    or count($next) &gt; 0"/>

  <xsl:if test="$suppress.navigation = '0' and $suppress.header.navigation = '0'">
    <div class="navheader">
      <xsl:if test="$row1 or $row2">
        <table width="100%" summary="Navigation header">

<!-- comment out printing row1
          <xsl:if test="$row1">
            <tr>
              <th colspan="3" align="center">
                <xsl:apply-templates select="." mode="object.title.markup"/>
              </th>
            </tr>
          </xsl:if>
-->
<!-- and try printing the new row 1 -->
<!-- changed width of each section of header table -->
<!--
          <xsl:if test="$row1">
            <tr>
              <td width="33%" align="left" valign="top">
                <xsl:if test="$navig.showtitles != 0">
                  <xsl:apply-templates select="$prev" mode="object.title.markup"/>
                </xsl:if>
                <xsl:text>&#160;</xsl:text>
              </td>
              <td width="34%" align="center">
                <xsl:choose>
                  <xsl:when test="$home != . or $nav.context = 'toc'">
                    <a accesskey="h">
                      <xsl:attribute name="href">
                        <xsl:call-template name="href.target">
                          <xsl:with-param name="object" select="$home"/>
                        </xsl:call-template>
                      </xsl:attribute>
                      <xsl:call-template name="navig.content">
                        <xsl:with-param name="direction" select="'home'"/>
                      </xsl:call-template>
                    </a>
                    <xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
                      <xsl:text>&#160;|&#160;</xsl:text>
                    </xsl:if>
                  </xsl:when>
                  <xsl:otherwise>&#160;</xsl:otherwise>
                </xsl:choose>

                <xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
                  <a accesskey="t">
                    <xsl:attribute name="href">
                      <xsl:apply-templates select="/*[1]"
                                           mode="recursive-chunk-filename">
                        <xsl:with-param name="recursive" select="true()"/>
                      </xsl:apply-templates>
                      <xsl:text>-toc</xsl:text>
                      <xsl:value-of select="$html.ext"/>
                    </xsl:attribute>
                    <xsl:call-template name="gentext">
                      <xsl:with-param name="key" select="'nav-toc'"/>
                    </xsl:call-template>
                  </a>
                </xsl:if>
              </td>
              <td width="33%" align="right" valign="top">
                <xsl:text>&#160;</xsl:text>
                <xsl:if test="$navig.showtitles != 0">
                  <xsl:apply-templates select="$next" mode="object.title.markup"/>
                </xsl:if>
              </td>
            </tr>
          </xsl:if>
-->
<!-- end new row1 -->

          <xsl:if test="$row2">
            <tr>
              <td width="20%" align="left">
                <xsl:if test="count($prev)>0">
                  <a accesskey="p">
                    <xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object" select="$prev"/>
                      </xsl:call-template>
                    </xsl:attribute>
                    <xsl:call-template name="navig.content">
                      <xsl:with-param name="direction" select="'prev'"/>
                    </xsl:call-template>
                  </a>
                </xsl:if>
                <xsl:text>&#160;</xsl:text>
              </td>
              <th width="60%" align="center">
                <xsl:choose>
                  <xsl:when test="count($up) > 0
                                  and generate-id($up) != generate-id($home)
                                  and $navig.showtitles != 0">
                    <xsl:apply-templates select="$up" mode="object.title.markup"/>
                  </xsl:when>
                  <xsl:otherwise>&#160;</xsl:otherwise>
                </xsl:choose>
              </th>
              <td width="20%" align="right">
                <xsl:text>&#160;</xsl:text>
                <xsl:if test="count($next)>0">
                  <a accesskey="n">
                    <xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object" select="$next"/>
                      </xsl:call-template>
                    </xsl:attribute>
                    <xsl:call-template name="navig.content">
                      <xsl:with-param name="direction" select="'next'"/>
                    </xsl:call-template>
                  </a>
                </xsl:if>
              </td>
            </tr>
          </xsl:if>
        </table>
      </xsl:if>
      <xsl:if test="$header.rule != 0">
        <hr/>
      </xsl:if>
    </div>
  </xsl:if>
</xsl:template>

<!-- ====================================================== -->
<!--                       Test section                     -->
<!-- ====================================================== -->

<!-- End stylesheet -->

</xsl:stylesheet>