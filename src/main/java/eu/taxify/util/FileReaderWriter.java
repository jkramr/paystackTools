package eu.taxify.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
public class FileReaderWriter {

  private SimpleFileReader getFileReader(String path, String fileName) {
    try {
      return new SimpleFileReader(new FileReader(
              new File(path)));
    } catch (Exception ignored) {
    }

    return readResource(fileName);
  }

  public void readFile(
          String path,
          String fileName,
          Consumer<String[]> action,
          boolean skipFirst
  ) {
    SimpleFileReader reader = getFileReader(path, fileName);

    readFile(reader, action, skipFirst);
  }

  private void readFile(
          SimpleFileReader reader,
          Consumer<String[]> action,
          boolean skipFirst
  ) {
    String file = reader.readFile();

    Arrays.stream(file.split("\n"))
          .skip(skipFirst ? 1 : 0)
          .forEach(line -> action.accept(parseCsv(line)));
  }

  private String[] parseCsv(String line) {
    List<String> matches = new ArrayList<>();

    parse(line, matches, 0);

    return matches.toArray(new String[matches.size()]);
  }

  private void parse(String line, List<String> matches, int i) {
    if (line.length() < 2 || i >= line.length() - 1) {
      return;
    }

    int    start = i;
    String word;
    char   endMatcher;
    int    hasQuote = 0;

    if (line.charAt(i) == '\"') {
      endMatcher = '\"';
      hasQuote = 1;
      i++;
    } else {
      endMatcher = ',';
    }

    while (i < line.length() - 1 && line.charAt(i) != endMatcher) {
      i++;
    }

    word = line.substring(start + hasQuote, i);

    matches.add(word);

    parse(line, matches, i + 1 + hasQuote);
  }

  private SimpleFileReader readResource(String fileName) {
    return new SimpleFileReader(
            new InputStreamReader(this.getClass()
                                      .getClassLoader()
                                      .getResourceAsStream(fileName)));
  }

  static class SimpleFileWriter {

    void writeFile(String path) {
      File file = new File(path);
    }
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
