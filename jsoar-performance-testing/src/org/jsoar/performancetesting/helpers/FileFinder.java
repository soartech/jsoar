package org.jsoar.performancetesting.helpers;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Taken from: http://wilddiary.com/list-files-matching-a-naming-pattern-java/
 * No copyright was listed so I took it and modified it. If there is a copyright
 * concern, well I can make a new version.
 */

public class FileFinder extends SimpleFileVisitor<Path>
{
    private final PathMatcher matcher;

    private List<Path> matchedPaths = new ArrayList<Path>();

    public FileFinder(String pattern)
    {
        matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    }

    // Compares the glob pattern against
    // the file or directory name.
    public void match(Path file)
    {
        Path name = file.getFileName();

        if (name != null && matcher.matches(name))
        {
            matchedPaths.add(name);
        }
    }

    // Invoke the pattern matching
    // method on each file.
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
    {
        match(file);
        return CONTINUE;
    }

    // Invoke the pattern matching
    // method on each directory.
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
    {
        match(dir);
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
    {
        return CONTINUE;
    }

    public int getTotalMatches()
    {
        return matchedPaths.size();
    }

    public Collection<Path> getMatchedPaths()
    {
        return matchedPaths;
    }
}
