package org.sqldroid;

import java.io.PrintStream;
import java.io.PrintWriter;

import android.database.SQLException;



public class SQLDroidSQLException extends java.sql.SQLException {
  private static final long serialVersionUID = -7299376329007161001L;

  /** The exception that this exception was created for. */
  SQLException sqlException;
  
  /** Create a hard java.sql.SQLException from the RuntimeException android.database.SQLException. */ 
  public SQLDroidSQLException (SQLException sqlException) {
    this.sqlException = sqlException;
  }

  /**
   * @param o
   * @return
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    return sqlException.equals(o);
  }

  /**
   * @return
   * @see Throwable#fillInStackTrace()
   */
  public Throwable fillInStackTrace() {
    return sqlException.fillInStackTrace();
  }

  /**
   * @return
   * @see Throwable#getCause()
   */
  public Throwable getCause() {
    return sqlException.getCause();
  }

  /**
   * @return
   * @see Throwable#getLocalizedMessage()
   */
  public String getLocalizedMessage() {
    return sqlException.getLocalizedMessage();
  }

  /**
   * @return
   * @see Throwable#getMessage()
   */
  public String getMessage() {
    return sqlException.getMessage();
  }

  /**
   * @return
   * @see Throwable#getStackTrace()
   */
  public StackTraceElement[] getStackTrace() {
    return sqlException.getStackTrace();
  }

  /**
   * 
   * @see Throwable#printStackTrace()
   */
  public void printStackTrace() {
    sqlException.printStackTrace();
  }

  /**
   * @param err
   * @see Throwable#printStackTrace(PrintStream)
   */
  public void printStackTrace(PrintStream err) {
    sqlException.printStackTrace(err);
  }

  /**
   * @param err
   * @see Throwable#printStackTrace(PrintWriter)
   */
  public void printStackTrace(PrintWriter err) {
    sqlException.printStackTrace(err);
  }

  /**
   * @return
   * @see Throwable#toString()
   */
  public String toString() {
    return sqlException.toString();
  }
  
}
