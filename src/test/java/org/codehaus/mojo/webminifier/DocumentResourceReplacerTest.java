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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test the document replacer.
 * 
 * @author huntc
 */
public class DocumentResourceReplacerTest
{

    private DocumentResourceReplacer replacer;

    private File html;

    /**
     * Setup.
     * 
     * @throws IOException if something goes wrong.
     * @throws SAXException if something goes wrong.
     * @throws URISyntaxException if something goes wrong.
     */
    @Before
    public void setUp()
        throws SAXException, IOException, URISyntaxException
    {
        URL url = DocumentResourceReplacer.class.getResource( "a.html" );
        html = new File( url.toURI() );
        replacer = new DocumentResourceReplacer( html );
    }

    /**
     * Test that JS files can be extracted from the parsed document.
     */
    @Test
    public void testFindJSResources()
    {
        List<File> jsFiles = replacer.findJSResources();
        assertEquals( 3, jsFiles.size() );
        assertEquals( "a.js", jsFiles.get( 0 ).getName() );
        assertEquals( "b.js", jsFiles.get( 1 ).getName() );
        assertEquals( "c.js", jsFiles.get( 2 ).getName() );
    }

    /**
     * Test that we can successfully replace what we have as script elements with a new one.
     * 
     * @throws URISyntaxException if something goes wrong.
     */
    @Test
    public void testReplaceJSResources()
        throws URISyntaxException
    {
        Set<File> jsResources = new HashSet<File>( 1 );
        URL url = DocumentResourceReplacer.class.getResource( "d.js" );
        File js = new File( url.toURI() );
        jsResources.add( js );
        replacer.replaceJSResources( html.getParentFile(), html, jsResources );

        List<File> jsFiles = replacer.findJSResources();
        assertEquals( 1, jsFiles.size() );
        assertEquals( "d.js", jsFiles.get( 0 ).getName() );
    }

    /**
     * Test that we can successfully write out the html document.
     * 
     * @throws IOException if something goes wrong.
     * @throws TransformerException if something goes wrong.
     */
    @Test
    public void testWriteHTML()
        throws IOException, TransformerException
    {
        File htmlFile = File.createTempFile( "tempHtml", ".html" );
        replacer.writeHTML( htmlFile, "UTF-8" );
        final long expectedLength = 535L;
        assertEquals( expectedLength, htmlFile.length() );
        htmlFile.delete();
    }
}
