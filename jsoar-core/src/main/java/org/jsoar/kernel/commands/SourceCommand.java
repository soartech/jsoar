/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.ProductionAddedEvent;
import org.jsoar.kernel.events.ProductionExcisedEvent;
import org.jsoar.util.FileTools;
import org.jsoar.util.UrlTools;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;

/**
 * Implementation of the "source" command.
 *
 * <p>Manages the following:
 *
 * <ul>
 *   <li>The current working directory (pwd)
 *   <li>The directory stack (pushd and popd)
 *   <li>Stats about current top-level source command (last command, productions added, etc)
 * </ul>
 *
 * @author ray
 */
public class SourceCommand {
  private final SourceCommandAdapter interp;
  private DirStackEntry workingDirectory =
      new DirStackEntry(new File(System.getProperty("user.dir")));
  private Stack<DirStackEntry> directoryStack = new Stack<DirStackEntry>();
  private Stack<String> fileStack = new Stack<String>();

  /**
   * Save the path to each sourced file in this list.
   *
   * <p>The Soar IDE uses this list to notify the user of un-sourced files.
   *
   * <p>QUESTION: Should this also have urls added to it in evalUrlAndPop()?
   */
  private List<String> sourcedFiles = new ArrayList<String>();

  /* package */ TopLevelState topLevelState;
  /* package */ final SoarEventManager events;
  /* package */ final SoarEventListener eventListener =
      new SoarEventListener() {
        @Override
        public void onEvent(SoarEvent event) {
          if (event instanceof ProductionAddedEvent) {
            topLevelState.productionAdded(((ProductionAddedEvent) event).getProduction());
          } else if (event instanceof ProductionExcisedEvent) {
            topLevelState.productionExcised(((ProductionExcisedEvent) event).getProduction());
          }
        }
      };
  /* package */ String[] lastTopLevelCommand = null;

  public SourceCommand(SourceCommandAdapter interp, SoarEventManager events) {
    this.interp = interp;
    this.events = events;
    fileStack.push("");
  }

  public String getWorkingDirectory() {
    return workingDirectory.url != null
        ? workingDirectory.url.toExternalForm()
        : workingDirectory.file.getAbsolutePath();
  }

  /* package */ DirStackEntry getWorkingDirectoryRaw() {
    return workingDirectory;
  }

  public String getCurrentFile() {
    return fileStack.peek();
  }

  public List<String> getSourcedFiles() {
    return sourcedFiles;
  }

  public void pushd(String dirString) throws SoarException {
    File newDir = new File(dirString);
    URL url = FileTools.asUrl(dirString);
    if (url != null || UrlTools.isClassPath(dirString)) {
      if (UrlTools.isClassPath(dirString)) {
        try {
          url = UrlTools.lookupClassPathURL(dirString);
        } catch (IOException e) {
          throw new SoarException(e);
        }
      }
      // A new URL. Just set that to be the working directory
      directoryStack.push(workingDirectory);
      workingDirectory = new DirStackEntry(url);
    } else if (workingDirectory.url != null && !newDir.isAbsolute()) {
      // Relative path where current directory is a URL.
      directoryStack.push(workingDirectory);
      workingDirectory = new DirStackEntry(joinUrl(workingDirectory.url, dirString));
    } else {
      if (!newDir.isAbsolute()) {
        assert workingDirectory.url == null;
        newDir = new File(workingDirectory.file, dirString);
      }

      if (!newDir.exists()) {
        throw new SoarException("Directory '" + newDir + "' does not exist");
      }
      if (!newDir.isDirectory()) {
        throw new SoarException("'" + newDir + "' is not a directory");
      }
      directoryStack.push(workingDirectory);
      workingDirectory = new DirStackEntry(newDir);
    }
  }

  public void popd() throws SoarException {
    if (directoryStack.isEmpty()) {
      throw new SoarException("Directory stack is empty");
    }
    workingDirectory = directoryStack.pop();
  }

  public void source(String fileString) throws SoarException {
    URL url = FileTools.asUrl(fileString);
    File file = new File(fileString);
    if (url != null) {
      pushd(getParentUrl(url).toExternalForm());
      evalUrlAndPop(url);
    } else if (UrlTools.isClassPath(fileString)) {
      try {
        // jtcl has built-in support for "resource:", which looks to be the same as "classpath:"
        // in tcl we replace the native source command with this one, so this "patch" provides
        // compatibility
        // compatibility is needed because jtcl internally uses source with "resource:", e.g., as
        // part of the "package require" command
        if (fileString.startsWith("resource:")) {
          fileString = fileString.replaceFirst("resource:", "classpath:");
        }

        url = UrlTools.lookupClassPathURL(fileString);
      } catch (IOException e) {
        throw new SoarException(e);
      }
      pushd(getParentUrl(url).toExternalForm());
      evalUrlAndPop(url);
    } else if (file.isAbsolute()) {
      pushd(file.getParent());
      evalFileAndPop(file);
    } else if (workingDirectory.url != null) {
      final URL childUrl = joinUrl(workingDirectory.url, fileString);
      pushd(getParentUrl(childUrl).toExternalForm());
      evalUrlAndPop(childUrl);
    } else {
      file = new File(workingDirectory.file, file.getPath());
      pushd(file.getParent());
      evalFileAndPop(file);
    }
  }

  private URL getParentUrl(URL url) throws SoarException {
    final String s = url.toExternalForm();
    final int i = s.lastIndexOf('/');
    if (i == -1) {
      throw new SoarException("Cannot determine parent of URL: " + url);
    }
    URL parent = FileTools.asUrl(s.substring(0, i));
    if (parent != null) {
      return parent;
    }
    return FileTools.asUrl(s.substring(0, i) + "/");
  }

  /* package */ URL joinUrl(URL parent, String child) {
    final String s = parent.toExternalForm();
    return FileTools.asUrl(s.endsWith("/") ? s + child : s + "/" + child);
  }

  private void evalFileAndPop(File file) throws SoarException {
    try {
      // replace the system file separator to be a standard forward slash
      sourcedFiles.add(file.getAbsolutePath().replace(File.separator, "/"));

      fileStack.push(file.getAbsolutePath());
      if (topLevelState != null) {
        topLevelState.files.add(new FileInfo(file.getName()));
      }
      interp.eval(file);
    } finally {
      fileStack.pop();
      popd();
    }
  }

  private void evalUrlAndPop(URL urlIn) throws SoarException {
    URL url = normalizeUrl(urlIn);

    try {
      fileStack.push(url.toExternalForm());
      if (topLevelState != null) {
        topLevelState.files.add(new FileInfo(url.toExternalForm()));
      }
      interp.eval(url);
    } finally {
      fileStack.pop();
      popd();
    }
  }

  /**
   * Make sure an URL is normalized, i.e. does not contain any .. or . path components.
   *
   * @param url the url to normalize
   * @return normalized URL
   * @throws SoarException if there are any problems with the URL
   */
  private URL normalizeUrl(URL url) throws SoarException {
    try {
      return url.toURI().normalize().toURL();
    } catch (MalformedURLException e) {
      throw new SoarException(e.getMessage(), e);
    } catch (URISyntaxException e) {
      throw new SoarException(e.getMessage(), e);
    }
  }

  // This ain't pretty, but it's private and it works
  /* package */ static class DirStackEntry {
    File file;
    URL url;

    public DirStackEntry(File file) {
      this.file = file;
    }

    public DirStackEntry(URL url) {
      this.url = url;
    }
  }

  /* package */ static class FileInfo {
    final String name;
    final List<String> productionsAdded = new ArrayList<String>();
    final List<String> productionsExcised = new ArrayList<String>();

    public FileInfo(String name) {
      this.name = name;
    }
  }

  /* package */ static class TopLevelState {
    final List<FileInfo> files = new ArrayList<FileInfo>();
    int totalProductionsAdded = 0;
    int totalProductionsExcised = 0;
    // int totalProductionsIgnored = 0; // TODO implement totalProductionsIgnored

    void productionAdded(Production p) {
      current().productionsAdded.add(p.getName());
      totalProductionsAdded++;
    }

    void productionExcised(Production p) {
      current().productionsExcised.add(p.getName());
      totalProductionsExcised++;
    }

    private FileInfo current() {
      return files.get(files.size() - 1);
    }
  }
}
