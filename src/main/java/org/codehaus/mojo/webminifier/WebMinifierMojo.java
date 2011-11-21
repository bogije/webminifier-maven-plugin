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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.webminifier.closure.ClosureJsCompressor;
import org.codehaus.mojo.webminifier.yui.YuiJsCompressor;
import org.codehaus.plexus.util.DirectoryScanner;
import org.xml.sax.SAXException;

import com.google.javascript.jscomp.CompilationLevel;

/**
 * Mojo to invoke WebMinifier plugin to minify web files.
 * 
 * @author Ben Jones
 * @author Christopher Hunt
 * @goal minify-js
 * @phase prepare-package
 */
public class WebMinifierMojo
    extends AbstractMojo
{
    /**
     * The type of JS Compressor to use.
     */
    public enum JsCompressorType
    {
        /** Types */
        YUI, CLOSURE, NONE
    }

    /**
     * The source folder with un-minified files.
     * 
     * @parameter default-value="${project.build.directory}/classes"
     * @required
     */
    private File sourceFolder;

    /**
     * The output folder to write minified files to.
     * 
     * @parameter default-value="${project.build.directory}/min/classes"
     */
    private File destinationFolder;

    /**
     * Process HTML files which match these patterns.
     * 
     * @parameter
     */
    private List<String> htmlIncludes;

    /**
     * Do not process HTML files which match these patterns.
     * 
     * @parameter
     */
    private List<String> htmlExcludes;

    /**
     * If a JavaScript resource contains one of these target/classes/js relative file names is found while minifying it
     * will be the last script file appended to the current minified script file. A new minified script will be created
     * for the next file, if one exists. Each name in the property corresponds to the relative file path of a file
     * accessible from the destinationFolder e.g. js/a.js would match up with a file located at target/classes/js/a.js.
     * Each property value, if provided, corresponds to the name component of a file that will be generated and without
     * the file extension. If omitted then a numbering scheme will be employed to name the file at the split point.
     * 
     * @parameter
     */
    private Properties jsSplitPoints;

    /**
     * All HTML, JavaScript and CSS files are assumed to have this encoding. Ê
     * 
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    private String encoding;

    /**
     * The type of compressor to use for JS files.
     * 
     * @parameter default-value="CLOSURE"
     */
    private JsCompressorType jsCompressorType;

    /**
     * YUI option 'linebreak'; insert a linebreak after VALUE columnns.
     * 
     * @parameter default-value="-1"
     */
    private int yuiLinebreak;

    /**
     * YUI option 'disableOptimizations'; disable all micro-optimizations.
     * 
     * @parameter default-value="false"
     */
    private boolean yuiDisableOptimizations;

    /**
     * YUI option 'munge'; minify and obfuscate. If false, minify only.
     * 
     * @parameter default-value="true"
     */
    private boolean yuiMunge;

    /**
     * YUI option 'preserveSemi'; preserve semicolons before }.
     * 
     * @parameter default-value="false"
     */
    private boolean yuiPreserveSemi;

    /**
     * Closure compiler level option either:
     * <ol>
     * <li>WHITESPACE_ONLY</li>
     * <li>SIMPLE_OPTIMIZATIONS</li>
     * <li>ADVANCED_OPTIMIZATIONS</li>
     * </ol>
     * 
     * @parameter default-value="SIMPLE_OPTIMIZATIONS"
     */
    private CompilationLevel closureCompilationLevel;

    /**
     * Whether or not the const keyword is to be accepted.
     * 
     * @parameter default-value="false"
     */
    private boolean closureAcceptConstKeyword;

    /**
     * Concatenate two files.
     * 
     * @param inputFile the file to concatenated.
     * @param outputFile the file to be concatenated.
     * @throws IOException if there is a problem with the operation.
     */
    private void concatenateFile( File inputFile, File outputFile )
        throws IOException
    {
        InputStream is = new FileInputStream( inputFile );
        try
        {
            OutputStream os = new FileOutputStream( outputFile, true );
            try
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Concatenating file: " + inputFile );
                }
                IOUtils.copy( is, os );
                os.write( '\n' );
            }
            finally
            {
                os.close();
            }
        }
        finally
        {
            is.close();
        }

    }

    /**
     * Main entry point for the MOJO.
     * 
     * @throws MojoExecutionException if there's a problem in the normal course of execution.
     * @throws MojoFailureException if there's a problem with the MOJO itself.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // Start off by copying all files over. We'll ultimately remove the js files that we don't need from there, and
        // create new ones in there (same goes for css files and anything else we minify).

        FileUtils.deleteQuietly( destinationFolder );
        try
        {
            FileUtils.copyDirectory( sourceFolder, destinationFolder );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot copy file to target folder", e );
        }

        // Process each HTML source file and concatenate into unminified output scripts
        int minifiedCounter = 0;

        // If a split point already exists on disk then we've been through the minification process. As
        // minification can be expensive, we would like to avoid performing it multiple times. Thus storing
        // a set of what we've previously minified enables us.
        Set<File> existingConcatenatedJsResources = new HashSet<File>();

        for ( String targetHTMLFile : getArrayOfTargetHTMLFiles() )
        {
            File targetHTML = new File( destinationFolder, targetHTMLFile );

            // Parse HTML file and locate SCRIPT elements
            DocumentResourceReplacer replacer;
            try
            {
                replacer = new DocumentResourceReplacer( targetHTML );
            }
            catch ( SAXException e )
            {
                throw new MojoExecutionException( "Problem reading html document", e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Problem opening html document", e );
            }

            List<File> jsResources = replacer.findJSResources();

            if ( jsSplitPoints == null )
            {
                jsSplitPoints = new Properties();
            }

            File concatenatedJsResource = null;

            URI destinationFolderUri = destinationFolder.toURI();

            // Walk backwards through the script declarations and note what files will map to what split point.
            Map<File, File> jsResourceTargetFiles = new HashMap<File, File>( jsResources.size() );
            ListIterator<File> jsResourcesIter = jsResources.listIterator( jsResources.size() );

            while ( jsResourcesIter.hasPrevious() )
            {
                File jsResource = jsResourcesIter.previous();

                URI candidateSplitPointNameUri = destinationFolderUri.relativize( jsResource.toURI() );
                String splitPointName = (String) jsSplitPoints.get( candidateSplitPointNameUri.toString() );

                // If we have no name and we've not been in here before, then assign an initial name based on a number.
                if ( splitPointName == null && concatenatedJsResource == null )
                {
                    concatenatedJsResource = new File( destinationFolder, //
                                                       Integer.valueOf( ++minifiedCounter ) + ".js" );
                }
                else if ( splitPointName != null )
                {
                    // We have a new split name so use it for this file and upwards in the script statements until we
                    // either hit another split point or there are no more script statements.
                    concatenatedJsResource = new File( destinationFolder, splitPointName + ".js" );

                    // Note that we've previously created this.
                    if ( concatenatedJsResource.exists() )
                    {
                        existingConcatenatedJsResources.add( concatenatedJsResource );
                    }
                }

                jsResourceTargetFiles.put( jsResource, concatenatedJsResource );
            }

            for ( File jsResource : jsResources )
            {
                concatenatedJsResource = jsResourceTargetFiles.get( jsResource );
                if ( !existingConcatenatedJsResources.contains( concatenatedJsResource ) )
                {
                    // Concatenate input file onto output resource file
                    try
                    {
                        concatenateFile( jsResource, concatenatedJsResource );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoExecutionException( "Problem concatenating JS files", e );
                    }

                    // Finally, remove the JS resource from the target folder as it is no longer required (we've
                    // concatenated it).
                    jsResource.delete();
                }
            }

            // Minify the concatenated JS resource files

            Set<File> concatenatedJsResources = new HashSet<File>( jsResourceTargetFiles.values() );

            if ( jsCompressorType != JsCompressorType.NONE )
            {
                Set<File> minifiedJSResources = new LinkedHashSet<File>( concatenatedJsResources.size() );
                for ( File concatenatedJSResource : concatenatedJsResources )
                {
                    File minifiedJSResource;
                    try
                    {
                        minifiedJSResource = FileUtils.toFile( //
                        new URL( concatenatedJSResource.toURI().toString().replace( ".js", ".min.js" ) ) );
                    }
                    catch ( MalformedURLException e )
                    {
                        throw new MojoExecutionException( "Problem determining file URL", e );
                    }

                    minifiedJSResources.add( minifiedJSResource );

                    // If we've not actually performed the minification before... then do so. This is the expensive bit
                    // so we like to avoid it if we can.
                    if ( !existingConcatenatedJsResources.contains( concatenatedJsResource ) )
                    {
                        boolean warningsFound;
                        try
                        {
                            warningsFound = minifyJSFile( concatenatedJSResource, minifiedJSResource );
                        }
                        catch ( IOException e )
                        {
                            throw new MojoExecutionException( "Problem reading/writing JS", e );
                        }

                        logCompressionRatio( minifiedJSResource.getName(), concatenatedJSResource.length(),
                                             minifiedJSResource.length() );

                        // If there were warnings then the user may want to manually invoke the compressor for further
                        // investigation.
                        if ( warningsFound )
                        {
                            getLog().warn( "Warnings were found. " + concatenatedJSResource
                                               + " is available for your further investigations." );
                        }
                    }
                }

                // Update source references
                replacer.replaceJSResources( destinationFolder, targetHTML, minifiedJSResources );
            }
            else
            {
                replacer.replaceJSResources( destinationFolder, targetHTML, concatenatedJsResources );
                getLog().info( "Concatenated resources with no compression" );
            }

            // Write HTML file to output dir
            try
            {
                replacer.writeHTML( targetHTML, encoding );
            }
            catch ( TransformerException e )
            {
                throw new MojoExecutionException( "Problem transforming html", e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Problem writing html", e );
            }

        }

        // Clean up the destination folder recursively where directories have nothing left in them.
        removeEmptyFolders( destinationFolder );
    }

    /**
     * @return an array of html files to be processed.
     */
    private String[] getArrayOfTargetHTMLFiles()
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( destinationFolder );

        String[] includesArray = getPatternsOrDefault( htmlIncludes, getDefaultIncludes() );
        scanner.setIncludes( includesArray );

        String[] excludesArray = getPatternsOrDefault( htmlExcludes, getDefaultExcludes() );
        scanner.setExcludes( excludesArray );

        scanner.scan();
        String[] includedFiles = scanner.getIncludedFiles();

        return includedFiles;
    }

    /**
     * @return the list of excludes by default.
     */
    protected String[] getDefaultExcludes()
    {
        return new String[0];
    }

    /**
     * @return the list of includes by default.
     */
    protected String[] getDefaultIncludes()
    {
        return new String[] { "**/*.html", "**/*.htm" };
    }

    /**
     * @return property
     */
    public File getDestinationFolder()
    {
        return destinationFolder;
    }

    /**
     * @return property
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * @return property
     */
    public List<String> getHtmlExcludes()
    {
        return htmlExcludes;
    }

    /**
     * @return property
     */
    public List<String> getHtmlIncludes()
    {
        return htmlIncludes;
    }

    /**
     * @return property
     */
    public JsCompressorType getJsCompressorType()
    {
        return jsCompressorType;
    }

    /**
     * @return property
     */
    public Properties getJsSplitPoints()
    {
        return jsSplitPoints;
    }

    private String[] getPatternsOrDefault( List<String> patterns, String[] defaultPatterns )
    {
        if ( patterns == null || patterns.isEmpty() )
        {
            return defaultPatterns;
        }
        else
        {
            return patterns.toArray( new String[patterns.size()] );
        }
    }

    /**
     * @return property
     */
    public File getSourceFolder()
    {
        return sourceFolder;
    }

    /**
     * @return property
     */
    public int getYuiLinebreak()
    {
        return yuiLinebreak;
    }

    /**
     * @return property
     */
    public boolean isYuiDisableOptimizations()
    {
        return yuiDisableOptimizations;
    }

    /**
     * @return property
     */
    public boolean isYuiMunge()
    {
        return yuiMunge;
    }

    /**
     * @return property
     */
    public boolean isYuiPreserveSemi()
    {
        return yuiPreserveSemi;
    }

    private void logCompressionRatio( String filename, long original, long changed )
    {
        String percentageString;
        if ( original > 0 )
        {
            int sizePercentage = (int) ( ( Double.valueOf( changed ) / Double.valueOf( original ) ) * 100.0 );
            percentageString = sizePercentage + "%";
        }
        else
        {
            percentageString = "-";
        }

        getLog().info( filename + " minified from " + Long.valueOf( original ) + " to " + Long.valueOf( changed )
                           + " bytes (" + percentageString + " of original size)" );
    }

    /**
     * Perform the actual minification.
     * 
     * @throws IOException a problem reading/writing files.
     * @throws MojoExecutionException if there's a problem during compression.
     * @return true if minification succeeded with no warnings.
     */
    private boolean minifyJSFile( File source, File target )
        throws IOException, MojoExecutionException
    {
        boolean warningsFound = false;

        // Minify JS file and append to output JS file
        InputStream is = new BufferedInputStream( new FileInputStream( source ) );
        try
        {
            OutputStream os = new FileOutputStream( target );
            try
            {
                AbstractCompressor compressor;
                switch ( jsCompressorType )
                {
                    case YUI:
                        compressor = new YuiJsCompressor( is, os, encoding, getLog() );
                        ( (YuiJsCompressor) compressor ).setOptions( yuiLinebreak, yuiMunge, yuiPreserveSemi,
                                                                     yuiDisableOptimizations );
                        break;
                    case CLOSURE:
                        compressor = new ClosureJsCompressor( is, os, encoding, getLog() );
                        ( (ClosureJsCompressor) compressor ).setOptions( closureCompilationLevel,
                                                                         closureAcceptConstKeyword );
                        break;
                    default:
                        assert false;
                        compressor = null;
                }

                if ( compressor != null )
                {
                    compressor.compress();
                    ExceptionState exceptionState = compressor.getExceptionState();
                    if ( exceptionState.hasErrors() )
                    {
                        throw new MojoExecutionException( "Problem(s) prevented compression from completing." );
                    }
                    else
                    {
                        warningsFound = exceptionState.hasWarnings();
                    }
                }
            }
            finally
            {
                os.close();
            }
        }
        finally
        {
            is.close();
        }

        return warningsFound;
    }

    private void removeEmptyFolders( File folder )
    {
        File[] files = folder.listFiles();
        boolean folderHasFile = false;
        for ( File file : files )
        {
            if ( file.isDirectory() )
            {
                removeEmptyFolders( file );
            }
            else
            {
                folderHasFile = true;
            }
        }
        if ( !folderHasFile )
        {
            folder.delete();
        }
    }

    /**
     * @param destinationFolder to set.
     */
    public void setDestinationFolder( File destinationFolder )
    {
        this.destinationFolder = destinationFolder;
    }

    /**
     * @param encoding to set.
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    /**
     * @param htmlExcludes to set.
     */
    public void setHtmlExcludes( List<String> htmlExcludes )
    {
        this.htmlExcludes = htmlExcludes;
    }

    /**
     * @param htmlIncludes to set.
     */
    public void setHtmlIncludes( List<String> htmlIncludes )
    {
        this.htmlIncludes = htmlIncludes;
    }

    /**
     * @param jsCompressorType to set.
     */
    public void setJsCompressorType( JsCompressorType jsCompressorType )
    {
        this.jsCompressorType = jsCompressorType;
    }

    /**
     * @param jsSplitPoints to set.
     */
    public void setJsSplitPoints( Properties jsSplitPoints )
    {
        this.jsSplitPoints = jsSplitPoints;
    }

    /**
     * @param sourceFolder to set.
     */
    public void setSourceFolder( File sourceFolder )
    {
        this.sourceFolder = sourceFolder;
    }

    /**
     * @param yuiDisableOptimizations to set.
     */
    public void setYuiDisableOptimizations( boolean yuiDisableOptimizations )
    {
        this.yuiDisableOptimizations = yuiDisableOptimizations;
    }

    /**
     * @param yuiLinebreak to set.
     */
    public void setYuiLinebreak( int yuiLinebreak )
    {
        this.yuiLinebreak = yuiLinebreak;
    }

    /**
     * @param yuiMunge to set.
     */
    public void setYuiMunge( boolean yuiMunge )
    {
        this.yuiMunge = yuiMunge;
    }

    /**
     * @param yuiPreserveSemi to set.
     */
    public void setYuiPreserveSemi( boolean yuiPreserveSemi )
    {
        this.yuiPreserveSemi = yuiPreserveSemi;
    }
}
