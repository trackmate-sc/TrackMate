package fiji.plugin.trackmate.io.geff;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for GEFF tests. Sets up the native library path for blosc
 * before any test code runs, covering all platforms and environments
 * (Eclipse, Maven, CI) without requiring manual run configuration.
 */
public abstract class GeffTestBase
{
    static
    {
        setupJnaLibraryPath();
    }

    /**
     * Prepends common blosc installation directories to {@code jna.library.path}
     * so JNA can find {@code libblosc} regardless of the execution environment.
     * <p>
     * Priority order matters: more specific paths (Apple Silicon Homebrew) come
     * before more generic ones so the correct architecture is always preferred.
     */
    private static void setupJnaLibraryPath()
    {
        final String[] candidates = {
            "/opt/homebrew/lib",           // macOS Apple Silicon (Homebrew)
            "/usr/local/lib",              // macOS Intel (Homebrew) or manual install
            "/usr/lib",                    // Linux generic
            "/usr/lib/x86_64-linux-gnu",   // Ubuntu / Debian x86_64
            "/usr/lib/aarch64-linux-gnu",  // Ubuntu / Debian ARM64
        };

        // Start from whatever is already set (e.g. by Maven Surefire -D flags)
        final String existing = System.getProperty( "jna.library.path", "" );
        final List< String > paths = new ArrayList<>(
                Arrays.asList( existing.split( File.pathSeparator ) ) );
        paths.removeIf( String::isEmpty );

        for ( final String candidate : candidates )
        {
            final File dir = new File( candidate );
            if ( dir.isDirectory() && !paths.contains( candidate ) )
                paths.add( candidate );
        }

        final String joined = String.join( File.pathSeparator, paths );
        System.setProperty( "jna.library.path", joined );
        System.out.println( "[GeffTestBase] jna.library.path = " + joined );
    }
}
