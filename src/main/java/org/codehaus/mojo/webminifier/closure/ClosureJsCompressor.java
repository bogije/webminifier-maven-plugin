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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.webminifier.AbstractCompressor;
import org.codehaus.mojo.webminifier.ExceptionState;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;

/**
 * Provide a compressor around Google's Closure Compiler. TODO: Write unit tests.
 */
public class ClosureJsCompressor
    extends AbstractCompressor
{

    /**
     * Compilation level.
     */
    private CompilationLevel compilationLevel;

    /**
     * Signal whether or not the const keyword is permitted.
     */
    private boolean acceptConstKeyword;

    /**
     * Construct the compressor.
     * 
     * @param source js to read.
     * @param target js to write.
     * @param encoding js file encoding to read/write.
     * @param logger where to log problems.
     */
    public ClosureJsCompressor( InputStream source, OutputStream target, String encoding, Log logger )
    {
        super( source, target, encoding, logger );
    }

    @Override
    public void compress()
        throws IOException
    {
        // Setup the compiler
        com.google.javascript.jscomp.Compiler.setLoggingLevel( Level.OFF );
        com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();

        JSSourceFile sourceFile = JSSourceFile.fromInputStream( "input", source );

        CompilerOptions options = new CompilerOptions();
        compilationLevel.setOptionsForCompilationLevel( options );
        options.setAcceptConstKeyword( acceptConstKeyword );
        options.setOutputCharset( encoding );

        // We're never concerned with non standard JSDOC - it is hardly a standard...
        options.setWarningLevel( DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF );

        // Compile
        Result result = compiler.compile( new JSSourceFile[0], new JSSourceFile[] { sourceFile }, options );

        // Report the outcomes.
        exceptionState = new ExceptionState();

        JSError[] errors = result.errors;
        for ( JSError error : errors )
        {
            logger.error( error.toString() );
        }
        if ( errors.length > 0 )
        {
            exceptionState.signalErrors();
        }

        JSError[] warnings = result.warnings;
        for ( JSError warning : warnings )
        {
            logger.warn( warning.toString() );
        }
        if ( warnings.length > 0 )
        {
            exceptionState.signalWarnings();
        }

        // Write the compiled source.

        OutputStreamWriter resourceWriter = new OutputStreamWriter( target, encoding );
        try
        {
            resourceWriter.append( compiler.toSource() );
        }
        finally
        {
            resourceWriter.close();
        }
    }

    /**
     * Build options.
     * 
     * @param compilationLevelParam how aggresive the compression should be as a result of compilation.
     * @param acceptConstKeywordParam true if the const keyword is acceptable.
     */
    public void setOptions( CompilationLevel compilationLevelParam, boolean acceptConstKeywordParam )
    {
        this.compilationLevel = compilationLevelParam;
        this.acceptConstKeyword = acceptConstKeywordParam;
    }

}
