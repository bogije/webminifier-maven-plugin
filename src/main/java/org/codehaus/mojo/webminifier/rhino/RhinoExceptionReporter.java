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

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.webminifier.ExceptionState;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * A Rhino compatible error reporter.
 */
public class RhinoExceptionReporter
    extends ExceptionState
    implements ErrorReporter
{
    private final Log logger;

    /**
     * @param logger the logger to use for reporting.
     */
    public RhinoExceptionReporter( Log logger )
    {
        super();
        this.logger = logger;
    }

    private String constructMessage( String type, String message, String sourceName, int line, String lineSource,
                                     int lineOffset )
    {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append( type );

        if ( message != null )
        {
            stringBuilder.append( ": " + message );
        }

        if ( sourceName != null && lineSource != null )
        {
            stringBuilder.append( " - " + sourceName );
            stringBuilder.append( ":" + Integer.valueOf( line ) );
            stringBuilder.append( ":" + Integer.valueOf( lineOffset ) );
            stringBuilder.append( ":" + lineSource );
        }

        return stringBuilder.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void error( String message, String sourceName, int line, String lineSource, int lineOffset )
    {
        signalErrors();
        logger.error( constructMessage( "Error", message, sourceName, line, lineSource, lineOffset ) );
    }

    /**
     * {@inheritDoc}
     */
    public EvaluatorException runtimeError( String message, String sourceName, int line, String lineSource,
                                            int lineOffset )
    {
        signalErrors();
        logger.error( constructMessage( "Runtime error", message, sourceName, line, lineSource, lineOffset ) );
        return new EvaluatorException( message, sourceName, line, lineSource, lineOffset );
    }

    /**
     * {@inheritDoc}
     */
    public void warning( String message, String sourceName, int line, String lineSource, int lineOffset )
    {
        signalWarnings();
        logger.warn( constructMessage( "Warning", message, sourceName, line, lineSource, lineOffset ) );
    }
}