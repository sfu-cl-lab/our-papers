Part of the open-source Proximity system (see LICENSE for copyright
and license information).

How to generate the Proximity tutorial from the XML source files.

=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
Organization of this file:
  Software     - the software needed to create and generate the
                 Proximity documentation products
  Environment  - the environment variables required to generate 
                 Proximity documentation from the DocBook source files
  Style sheets - local style sheets that set processing parameters and
                 modify the standard style sheets
  HTML         - how to generate the HTML documentation files
  PDF          - how to generate the PDF documentation file
  Source       - notes on the source DocBook files  
=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

-----------------------------------------------------------------------
                          ****  Software  ****
-----------------------------------------------------------------------
Required software:

  DocBook-xsl
     Description: XSL stylesheets for transforming DocBook SGML/XML
         document to JavaHelp and other formats
     Source: docbook.sourceforge.net
     Version: 1.65.1
  Saxon  
     Description: XSLT processor
     Source: saxon.sourceforge.net
     Version 6.5.3
     Notes:
  Xerces2
     Description: XML parser.
     Source: xml.apache.org/xerces2-j
     Version: 2.6.2
  FOP
     Description: Formatting objects processor
     Source: xml.apache.org/fop
     Version: 0.20.5
  Jimi   
     Description: Image processing library (Java)
     Source: java.sun.com/products/jimi/
     Version: 1.0
  Catalog Entity Resolver
     Description: Maps publc and system identifiers to local files
     Source: http://xml.apache.org/dist/commons/
     Version: 1.1

Other useful software:
  DocBook XML
     Description: DTD and support files
     Source: www.oasis-open.org/docbook/xml/
     Version: 4.3
  psgml
     Description: Major mode for emacs editing of SGML and XML files
     Source: 
     Version: 1.2.5

-----------------------------------------------------------------------
                         ****  Environment  ****
-----------------------------------------------------------------------
Define the following environment variables:
  SGML_CATALOG_FILES = <location of catalog file>            [Required]
  PROX_HOME = <Proximity installation directory>             [Optional]
  DOCBOOK_XSL_HOME = <location of DocBook XSL style sheets>  [Optional]

Add to CLASSPATH:
     Catalog resolver:
                   <path-to-resolver>/resolver-1.0.jar
                   directory containing CatalogManager.properties file
     Saxon:        <path-to-saxon>/saxon.jar
     Saxon extensions for DocBook:
                   $DOCBOOK_XSL_HOME/extensions/saxon651.jar
     Xerces2:      <path-to-xerces2>/xercesImpl.jar
                   <path-to-xerces2>/xml-apis.jar
                   <path-to-xerces2>/xercesSamples.jar
     FOP:          <path-to-fop>/build/fop.jar
                   <path-to-fop>/lib/batik.jar
                   <path-to-fop>/lib/xalan-2.4.1.jar
                   <path-to-fop>/lib/xercesImpl-2.2.1.jar
                   <path-to-fop>/lib/JimiProClassses.jar
                   <path-to-fop>/lib/avalon-framework-cvs-20020806.jar

-----------------------------------------------------------------------
                        ****  STYLE SHEETS  ****
-----------------------------------------------------------------------

prox-common.xsl defines style sheet paramters and functons usesd by
     both HTML and FO generation.

prox-html.xsl imports the $DOCBOOK_XSL_HOME/html/chunk.xsl style sheet
     and sets additional parameters for HTML processing.

prox-fo.xsl imports the $DOCBOOK_XSL_HOME/fo/docbook.xsl style sheet
     and sets additional parameters for FO processing.

-----------------------------------------------------------------------
                           ****  HTML  ****
-----------------------------------------------------------------------

To generate HTML using Saxon:
  cd $PROX_HOME/src/xml/doc/user/tutorial
  java -Djavax.xml.parsers.DocumentBuilderFactory=\
           org.apache.xerces.jaxp.DocumentBuilderFactoryImpl \
       -Djavax.xml.parsers.SAXParserFactory=\
           org.apache.xerces.jaxp.SAXParserFactoryImpl \
       -Djavax.xml.transform.TransformerFactory=\
           com.icl.saxon.TransformerFactoryImpl \
       com.icl.saxon.StyleSheet \
       -x org.apache.xml.resolver.tools.ResolvingXMLReader \
       -y org.apache.xml.resolver.tools.ResolvingXMLReader \
       -r org.apache.xml.resolver.tools.CatalogResolver \
       -u -w0 \
       Tutorial.xml \ 
       $PROX_HOME/src/xml/doc/prox-html.xsl

-----------------------------------------------------------------------
                            ****  PDF  ****
-----------------------------------------------------------------------

Step 1:
-------

To generate the .fo file using the Xerces parser with Saxon:
  cd $PROX_HOME/src/xml/doc/user/tutorial
  java -Djavax.xml.parsers.DocumentBuilderFactory=\
           org.apache.xerces.jaxp.DocumentBuilderFactoryImpl \
       -Djavax.xml.parsers.SAXParserFactory=\
           org.apache.xerces.jaxp.SAXParserFactoryImpl \
       -Djavax.xml.transform.TransformerFactory=\
           com.icl.saxon.TransformerFactoryImpl \
       com.icl.saxon.StyleSheet \
       -x org.apache.xml.resolver.tools.ResolvingXMLReader \
       -y org.apache.xml.resolver.tools.ResolvingXMLReader \
       -r org.apache.xml.resolver.tools.CatalogResolver \
       -u -w0 \
       -o Tutorial.fo \
       Tutorial.xml  \
       $PROX_HOME/src/xml/doc/prox-fo.xsl

Step 2:
-------
     
To generate PDF from the .fo file:
  cd $PROX_HOME/src/xml/doc/user/tutorial
  java -Xmx256M org.apache.fop.apps.Fop  \
       -fo Tutorial.fo  \
       -pdf Tutorial.pdf

Note the increase in the JVM memory to 256MB (specified with the
"-Xmx256M" option). This is (so far) not needed to generate the
Tutorial but is required for the QGraph Guide.

To turn on debugging in FOP, use the -d option.

-----------------------------------------------------------------------
                           ****  Source  ****
-----------------------------------------------------------------------

The Tutorial is broken up into separate documents for each chapter,
with a root document that pulls them all together.

   Tutorial.xml                       Root document
   TutorialEntities.ent               Entity definitions
   TutorialInfo.xml                   Front matter
   Tutorial_Introduction.xml          Chapter 1
   Tutorial_ProxOverview.xml          Chapter 2
   Tutorial_Importing.xml             Chapter 3
   Tutorial_Browsing.xml              Chapter 4
   Tutorial_Queries.xml               Chapter 5
   Tutorial_Scripts.xml               Chapter 6
   Tutorial_Models.xml                Chapter 7
   Tutorial_ApxQuickRef.xml           Appendix A
   Tutorial_ApxInstalling.xml         Appendix B
   Tutorial_ApxXMLImport.xml          Appendix C
   Tutorial_ApxQueryXMLFormat.xml     Appendix D

Graphics:

   Source files for the graphics are in tutorial/images.
   They are copied to the HTML directoy for distribution.

Bibliography:

   Bibliography entries are stored in a master bibliography file,
   $PROX_HOME/src/xml/doc/ProxmintyBibliograph.xml.  The Proxmity
   style sheets (prox-html.xsl, prox-javahelp.xsl, and prox-fo.xsl)
   are set up to use this master bibliograph when generating the
   tutorial.
