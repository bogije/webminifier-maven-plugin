package org.codehaus.mojo.webminifier.rhino;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;

/**
 * Test error reporting.
 * 
 * @author huntc
 */
public class JavaScriptErrorReporterTest
{

    private RhinoExceptionReporter reporter;

    private Log logger;

    private static final int TEST_LINENO = 100;

    private static final int TEST_COLNO = 30;

    /**
     * Setup the tests.
     */
    @Before
    public void setUp()
    {
        logger = mock( Log.class );
        reporter = new RhinoExceptionReporter( logger );
    }

    /**
     * Error format.
     */
    @Test
    public void testError()
    {
        reporter.error( "Whoops", "badsource.js", TEST_LINENO, "var a", TEST_COLNO );
        verify( logger ).error( "Error: Whoops - badsource.js:100:30:var a" );
    }

    /**
     * Runtime format.
     */
    @Test
    public void testRuntimeError()
    {
        EvaluatorException e = reporter.runtimeError( "Whoops", "badsource.js", TEST_LINENO, "var a", TEST_COLNO );
        verify( logger ).error( "Runtime error: Whoops - badsource.js:100:30:var a" );
        assertEquals( "Whoops (badsource.js#100)", e.getMessage() );
    }

    /**
     * Warning.
     */
    @Test
    public void testWarning()
    {
        reporter.warning( null, null, TEST_LINENO, null, TEST_COLNO );
        reporter.warning( null, "badsource.js", TEST_LINENO, null, TEST_COLNO );

        verify( logger, times( 2 ) ).warn( "Warning" );
    }

}
