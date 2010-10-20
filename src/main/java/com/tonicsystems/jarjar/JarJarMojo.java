/**
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tonicsystems.jarjar;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils;

import com.tonicsystems.jarjar.util.StandaloneJarProcessor;

/**
 * TODO
 * 
 * @goal jarjar
 * @phase package
 * @requiresDependencyResolution test
 */
public class JarJarMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * TODO
     * 
     * @parameter
     * @required
     */
    private List<PatternElement> rules;

    /**
     * TODO
     * 
     * @parameter
     */
    private File outputFile;

    /**
     * TODO
     * 
     * @parameter
     */
    private List<String> includes;

    /**
     * TODO
     * 
     * @parameter
     */
    private List<String> excludes;

    /**
     * TODO
     * 
     * @parameter
     */
    private boolean skipManifest;

    /**
     * @component
     */
    private ArchiverManager archiverManager;

    @SuppressWarnings( "unchecked" )
    public void execute()
        throws MojoExecutionException
    {
        final MainProcessor processor = new MainProcessor( rules, getLog().isDebugEnabled(), skipManifest );

        final File file = project.getArtifact().getFile();
        final File orig = new File( file.getParentFile(), "original-" + file.getName() );
        final File uber = new File( file.getParentFile(), "uber-" + file.getName() );

        final AndArtifactFilter filter = new AndArtifactFilter();
        if ( null != includes )
        {
            filter.add( new IncludesArtifactFilter( includes ) );
        }
        if ( null != excludes )
        {
            filter.add( new ExcludesArtifactFilter( excludes ) );
        }

        try
        {
            final Archiver archiver = archiverManager.getArchiver( "jar" );

            archiver.setDestFile( uber );
            archiver.setIncludeEmptyDirs( false );
            archiver.addArchivedFileSet( file );

            for ( final Artifact a : (Set<Artifact>) project.getArtifacts() )
            {
                if ( filter.include( a ) )
                {
                    try
                    {
                        archiver.addArchivedFileSet( a.getFile() );
                    }
                    catch ( final Throwable e )
                    {
                        getLog().info( "Ignoring: " + a );
                        getLog().debug( e );
                    }
                }
            }

            archiver.createArchive();

            if ( null == outputFile )
            {
                FileUtils.rename( file, orig );
                outputFile = file;
            }

            getLog().info( "JarJar'ing to: " + outputFile );
            StandaloneJarProcessor.run( uber, outputFile, processor );
            processor.strip( outputFile );

            uber.delete();
        }
        catch ( final Throwable e )
        {
            throw new MojoExecutionException( e.toString() );
        }
    }
}
