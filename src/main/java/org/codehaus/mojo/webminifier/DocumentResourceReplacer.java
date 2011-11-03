package org.codehaus.mojo.webminifier;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Responsible identifying resource statements in a document and providing the means to replace them.
 */
public class DocumentResourceReplacer
{

    private final Document document;

    private final File documentParentFile;

    private final DOMParser parser;

    /**
     * @param htmlFile the html document.
     * @throws IOException if something goes wrong.
     * @throws SAXException if something goes wrong.
     */
    public DocumentResourceReplacer( File htmlFile )
        throws SAXException, IOException
    {
        parser = new DOMParser();

        parser.parse( htmlFile.toString() );
        documentParentFile = htmlFile.getParentFile();
        document = parser.getDocument();
    }

    /**
     * @return a list of JS script declarations returned as files.
     */
    public List<File> findJSResources()
    {
        List<File> jsResources = new ArrayList<File>();
        // Get all <script> tags from the document
        NodeList scriptNodes = document.getElementsByTagName( "script" );
        for ( int i = 0; i < scriptNodes.getLength(); i++ )
        {
            Node scriptNode = scriptNodes.item( i );
            NamedNodeMap scriptAttrNodes = scriptNode.getAttributes();
            if ( scriptAttrNodes != null )
            {
                Attr srcAttrNode = (Attr) scriptAttrNodes.getNamedItem( "src" );
                if ( srcAttrNode != null )
                {
                    String jsSrc = srcAttrNode.getValue();
                    // If it has a SRC which can be resolved
                    File scriptFile = new File( documentParentFile, jsSrc );
                    if ( scriptFile.isFile() )
                    {
                        jsResources.add( scriptFile );
                    }
                }
            }
        }

        return jsResources;
    }

    /**
     * @return the html source as a string.
     * @throws TransformerException if something does wrong.
     */
    private String getHTMLSource()
        throws TransformerException
    {
        // Use a Transformer for output
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        
        DOMSource source = new DOMSource( document );
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult( writer );

        DocumentType doctype = document.getDoctype();
        if ( doctype != null )
        {
            boolean docTypeSet = false;
            if ( doctype.getPublicId() != null )
            {
                transformer.setOutputProperty( OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId() );
                docTypeSet = true;
            }
            if ( doctype.getSystemId() != null )
            {
                transformer.setOutputProperty( OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId() );
                docTypeSet = false;
            }
            // If we know we have a doctype, but there's no id then they'll be nothing written. In this case we write
            // out an empty doctype html in support of html that has not yet been ratified (which is as per the source
            // document).
            if ( !docTypeSet )
            {
                writer.write( "<!DOCTYPE html>" );
            }
        }

        transformer.transform( source, result );

        return writer.toString();
    }

    /**
     * Replace the script statements that exist in the document with the new set.
     * 
     * @param documentDir the folder that represents the root.
     * @param jsResources the new set.
     */
    public void replaceJSResources( File documentDir, Set<File> jsResources )
    {
        // Get and remove all SCRIPT elements
        NodeList scriptNodes = document.getElementsByTagName( "script" );
        while ( scriptNodes.getLength() > 0 )
        {
            // Remove existing script nodes
            Node scriptNode = scriptNodes.item( 0 );
            scriptNode.getParentNode().removeChild( scriptNode );
        }

        // Note the head node to add to.
        NodeList headElements = document.getElementsByTagName( "head" );
        if ( headElements.getLength() == 1 )
        {
            Node headElement = headElements.item( 0 );

            // Insert new SCRIPT elements for all replaced resources
            String documentUri = documentDir.getParentFile().toURI().toString();
            for ( File jsResource : jsResources )
            {
                String jsResourceRelUri = jsResource.toURI().toString();
                jsResourceRelUri = jsResourceRelUri.substring( documentUri.length() );

                Element jsElement = document.createElement( "script" );
                jsElement.setAttribute( "type", "text/javascript" );
                jsElement.setAttribute( "src", jsResourceRelUri );
                headElement.appendChild( jsElement );
            }
        }
    }

    /**
     * Write out the html source for the current document.
     * 
     * @param htmlFile the file to write.
     * @param encoding the encoding to use.
     * @throws TransformerException if something goes wrong.
     * @throws IOException there is a problem writing the file.
     */
    public void writeHTML( File htmlFile, String encoding )
        throws TransformerException, IOException
    {
        OutputStream fos = new FileOutputStream( htmlFile );
        try
        {
            OutputStreamWriter updatedHTMLWriter = new OutputStreamWriter( new BufferedOutputStream( fos ), encoding );
            try
            {
                updatedHTMLWriter.write( getHTMLSource() );
            }
            finally
            {
                updatedHTMLWriter.close();
            }
        }
        finally
        {
            fos.close();
        }
    }

}