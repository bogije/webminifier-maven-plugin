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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.plugin.logging.Log;

/**
 * An abstraction of a compressor.
 */
public abstract class AbstractCompressor
{
    /**
     * Source to read.
     */
    protected final InputStream source;

    /**
     * Target to write.
     */
    protected final OutputStream target;

    /**
     * The encoding to use for reading and writing the streams.
     */
    protected final String encoding;

    /**
     * Where to log problems to.
     */
    protected Log logger;

    /**
     * The error reporter to use.
     */
    protected ExceptionState exceptionState;

    /**
     * Construct a compressor.
     * 
     * @param source Stream to read.
     * @param target Stream to write.
     * @param encoding The encoding to read/write the streams.
     * @param logger where to log problems to.
     */
    public AbstractCompressor( InputStream source, OutputStream target, String encoding, Log logger )
    {
        this.source = source;
        this.target = target;
        this.encoding = encoding;
        this.logger = logger;
    }

    /**
     * Perform the compression.
     * 
     * @throws IOException if there is a problem reading/writing the resource.
     */
    public abstract void compress()
        throws IOException;

    /**
     * @return the error reporter used.
     */
    public ExceptionState getExceptionState()
    {
        return exceptionState;
    }
}
