package eu.taxify.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Arrays;
import java.util.function.Consumer;

@Component
public class FileSourceReader {

  private SimpleFileReader getFileReader(String path, String fileName) {
    try {
      return new SimpleFileReader(new FileReader(
              new File(path)));
    } catch (FileNotFoundException ignored) {
    }

    return readResource(fileName);
  }

  public void readFile(
          String path,
          String fileName,
          Consumer<String> action
  ) {
    SimpleFileReader reader = getFileReader(path, fileName);

    readFile(reader, action);
  }

  private void readFile(
          SimpleFileReader reader,
          Consumer<String> action
  ) {
    Arrays.stream(reader.readFile().split("\n"))
          .forEach(action);
  }

  private SimpleFileReader readResource(String fileName) {
    return new SimpleFileReader(
            new InputStreamReader(this.getClass()
                                      .getClassLoader()
                                      .getResourceAsStream(fileName)));
  }

  static class SimpleFileReader {

    private Reader reader;

    SimpleFileReader(Reader reader) {
      this.reader = reader;
    }

    String readFile() {
      StringBuilder  builder = new StringBuilder();
      BufferedReader reader  = null;
      try {
        reader = new BufferedReader(this.reader);
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

    private void closeQuietly(Closeable c) {
      if (c != null) {
        try {
          c.close();
        } catch (IOException ignored) {
        }
      }
    }
  }
}
