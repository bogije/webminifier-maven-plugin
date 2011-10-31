package org.codehaus.mojo.webminifier.closure;

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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;

import com.google.javascript.jscomp.CompilationLevel;

/**
 * @author Christopher Hunt
 */
public class ClosureJsCompressorTest
{
    private ClosureJsCompressor compressor;

    private InputStream source;

    private OutputStream target;

    Log logger;

    /**
     * @throws UnsupportedEncodingException if something goes wrong.
     */
    @Before
    public void setUp()
        throws UnsupportedEncodingException
    {
        source = new ByteArrayInputStream( "this.a = 1;".getBytes( "UTF-8" ) );
        target = new ByteArrayOutputStream();
        String encoding = "UTF-8";
        logger = mock( Log.class );
        compressor = new ClosureJsCompressor( source, target, encoding, logger );
    }

    /**
     * Test a regular execution.
     * 
     * @throws IOException if something goes wrong.
     */
    @Test
    public void testCompress()
        throws IOException
    {
        CompilationLevel compilationLevelParam = CompilationLevel.SIMPLE_OPTIMIZATIONS;
        boolean acceptConstKeywordParam = false;
        compressor.setOptions( compilationLevelParam, acceptConstKeywordParam );

        compressor.compress();

        verify( logger, times( 0 ) ).error( (CharSequence) any() );
        verify( logger, times( 0 ) ).warn( (CharSequence) any() );
        assertEquals( "this.a=1;", target.toString() );
    }

    /**
     * Test a warning execution.
     * 
     * @throws IOException if something goes wrong.
     */
    @Test
    public void testCompressWithWarnings()
        throws IOException
    {
        CompilationLevel compilationLevelParam = CompilationLevel.ADVANCED_OPTIMIZATIONS;
        boolean acceptConstKeywordParam = false;
        compressor.setOptions( compilationLevelParam, acceptConstKeywordParam );

        compressor.compress();

        verify( logger, times( 0 ) ).error( (CharSequence) any() );
        verify( logger, times( 1 ) )//
        .warn( "JSC_USED_GLOBAL_THIS. dangerous use of the global this object at input line 1 : 0" );
        assertEquals( "this.a=1;", target.toString() );
    }
}
