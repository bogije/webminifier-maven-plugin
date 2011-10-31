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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.webminifier.WebMinifierMojo.JsCompressorType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the JSMinifierMojo class methods.
 * 
 * @author huntc
 */
public class WebMinifierMojoTest
{

    /**
     * The mojo to test.
     */
    private WebMinifierMojo mojo;

    /**
     * Set up a our mojo for testing.
     * 
     * @throws URISyntaxException if something went wrong.
     */
    @Before
    public void setUpMojo()
        throws URISyntaxException
    {
        File sourceFolder = ( new File( WebMinifierMojoTest.class.getResource( "a.html" ).toURI() ) ).getParentFile();
        File destinationFolder = new File( System.getProperty( "java.io.tmpdir" ), "WebMinifierMojoTest" );
        destinationFolder.mkdirs();

        mojo = new WebMinifierMojo();
        mojo.setDestinationFolder( destinationFolder );
        mojo.setSourceFolder( sourceFolder );
        mojo.setEncoding( "UTF-8" );
        mojo.setJsCompressorType( JsCompressorType.YUI );
        mojo.setYuiDisableOptimizations( false );
        mojo.setYuiLinebreak( -1 );
        mojo.setYuiMunge( true );
        mojo.setYuiPreserveSemi( false );
    }

    /**
     * Tidy up.
     */
    @After
    public void tearDownMojo()
    {
        FileUtils.deleteQuietly( mojo.getDestinationFolder() );
    }

    /**
     * Take the MOJO for a normal run.
     * 
     * @throws MojoFailureException if something goes wrong.
     * @throws MojoExecutionException if something goes wrong.
     */
    @Test
    public void testNormalRun()
        throws MojoExecutionException, MojoFailureException
    {
        mojo.execute();

        @SuppressWarnings( "unchecked" )
        Collection<File> files = FileUtils.listFiles( mojo.getDestinationFolder(), //
                                                      new String[] { "html", "js" }, true );

        assertEquals( 3, files.size() );
        Iterator<File> iter = files.iterator();
        assertEquals( "1.min.js", iter.next().getName() );
        assertEquals( "a.html", iter.next().getName() );
        assertEquals( "d.js", iter.next().getName() );
    }
}
