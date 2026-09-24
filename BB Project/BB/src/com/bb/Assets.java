package com.bb;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds art without needing {@code lib} on the classpath.
 *
 * <p>Every image used to be loaded with {@code getClass().getResource(path)}, which only
 * works when {@code lib} has been added to the classpath by hand - {@code run.bat} does
 * this, but a plain "Run" from an IDE with no project configuration does not, which is why
 * the game used to start with no art unless someone first set that up manually.
 *
 * <p>{@link #getResource(String)} tries the classpath first (so nothing changes for anyone
 * who already has {@code lib} wired in - {@code run.bat}, the committed VS Code launch
 * config), and only falls back to searching the filesystem for a sibling {@code lib}
 * directory when that comes back empty. The search starts from the working directory and
 * from wherever the running code itself lives, and walks upward a few levels from each -
 * between them that covers "run from the project root" (every IDE's default working
 * directory) and "run from somewhere else entirely" (the code's own location always finds
 * its way back to the project). Once found, the directory is cached for the rest of the run.
 */
public final class Assets {

    /** How far to walk up from a starting point looking for a sibling {@code lib}. */
    private static final int MAX_ANCESTORS = 8;

    private static volatile File libDir;
    private static volatile boolean searched;

    private Assets() {}

    /** Resolves a resource path such as {@code "/ships/battleCrusier.png"} to a URL, or null. */
    public static URL getResource(String path) {
        if (path == null || path.isEmpty()) return null;
        String normalized = path.startsWith("/") ? path : "/" + path;

        URL onClasspath = Assets.class.getResource(normalized);
        if (onClasspath != null) return onClasspath;

        File dir = libDirectory();
        if (dir == null) return null;

        // lib/ is the classpath root the path was written against, so strip the leading
        // slash and resolve the rest under it directly.
        File file = new File(dir, normalized.substring(1).replace('/', File.separatorChar));
        return file.isFile() ? toURL(file) : null;
    }

    /** The {@code lib} directory this run found, or null if none was found. */
    private static File libDirectory() {
        if (searched) return libDir;
        synchronized (Assets.class) {
            if (searched) return libDir;
            libDir = findLibDirectory();
            searched = true;
            if (libDir == null) {
                System.err.println("Assets: could not find a 'lib' directory near the working "
                        + "directory (" + new File("").getAbsolutePath() + ") or the running "
                        + "code. Run the game from the project root, or via run.bat, so art "
                        + "can be found.");
            }
            return libDir;
        }
    }

    private static File findLibDirectory() {
        for (File root : searchRoots()) {
            File candidate = new File(root, "lib");
            if (candidate.isDirectory()) return candidate;
        }
        return null;
    }

    /**
     * Starting points to search upward from, in order: the working directory (where every
     * IDE's default run configuration and {@code run.bat} both leave it - the project root),
     * then wherever the running code itself was loaded from (compiled classes or a jar),
     * which stays correct even if something unusual set a different working directory.
     */
    private static List<File> searchRoots() {
        List<File> starts = new ArrayList<>();
        starts.add(new File("").getAbsoluteFile());

        try {
            File codeLocation = new File(
                    Assets.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            starts.add(codeLocation.isFile() ? codeLocation.getParentFile() : codeLocation);
        } catch (Exception | Error ignored) {
            // Sandboxed environments (no CodeSource) or a malformed URI just mean this
            // starting point is skipped - the working directory search still runs.
        }

        Set<File> roots = new LinkedHashSet<>();
        for (File start : starts) {
            File dir = start;
            for (int depth = 0; dir != null && depth < MAX_ANCESTORS; depth++) {
                roots.add(dir);
                dir = dir.getParentFile();
            }
        }
        return new ArrayList<>(roots);
    }

    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
