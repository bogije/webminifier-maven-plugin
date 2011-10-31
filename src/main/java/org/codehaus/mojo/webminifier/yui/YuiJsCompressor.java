package org.codehaus.mojo.webminifier.yui;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.webminifier.AbstractCompressor;
import org.codehaus.mojo.webminifier.rhino.RhinoExceptionReporter;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * Compress YUI things. TODO: Requires a test case.
 */
public class YuiJsCompressor
    extends AbstractCompressor
{
    private int yuiLinebreak;

    private boolean yuiMunge;

    private boolean yuiPreserveSemi;

    private boolean yuiDisableOptimizations;

    /**
     * Constructor
     * 
     * @param source stream to read.
     * @param target stream to writer.
     * @param encoding encoding to use.
     * @param logger where to log errors to.
     */
    public YuiJsCompressor( InputStream source, OutputStream target, String encoding, Log logger )
    {
        super( source, target, encoding, logger );
    }

    @Override
    public void compress()
        throws IOException
    {
        InputStreamReader resourceReader = new InputStreamReader( source, encoding );
        try
        {
            OutputStreamWriter resourceWriter = new OutputStreamWriter( target, encoding );
            try
            {
                // Setup JavaScriptCompressor and compress JS
                exceptionState = new RhinoExceptionReporter( logger );
                JavaScriptCompressor compressor =
                    new JavaScriptCompressor( resourceReader, (RhinoExceptionReporter) exceptionState );
                compressor.compress( resourceWriter, yuiLinebreak, yuiMunge, false, yuiPreserveSemi,
                                     yuiDisableOptimizations );
            }
            finally
            {
                resourceWriter.close();
            }
        }
        finally
        {
            resourceReader.close();
        }
    }

    /**
     * Option builder.
     * 
     * @param yuiLinebreakParam option.
     * @param yuiMungeParam option.
     * @param yuiPreserveSemiParam option.
     * @param yuiDisableOptimizationsParam option.
     */
    public void setOptions( int yuiLinebreakParam, boolean yuiMungeParam, boolean yuiPreserveSemiParam,
                            boolean yuiDisableOptimizationsParam )
    {
        this.yuiLinebreak = yuiLinebreakParam;
        this.yuiMunge = yuiMungeParam;
        this.yuiPreserveSemi = yuiPreserveSemiParam;
        this.yuiDisableOptimizations = yuiDisableOptimizationsParam;
    }

}
