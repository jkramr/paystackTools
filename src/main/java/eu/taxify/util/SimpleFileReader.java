package eu.taxify.util;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by jkramr on 2/28/17.
 */
@Getter
public
class SimpleFileReader {

  private Reader reader;

  public
  SimpleFileReader(Reader reader) {
    this.reader = reader;
  }

  public
  String readFile() {
    StringBuilder  builder = new StringBuilder();
    BufferedReader reader  = null;
    try {
      reader = new BufferedReader(getReader());
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line).append('\n');
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      closeQuietly(reader);
    }
    return builder.toString();
  }

  private
  void closeQuietly(Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException ignored) {
      }
    }
  }
}
