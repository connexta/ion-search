/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.content;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BodyContentExtractorTest {

  private static final String SAMPLE_PDF = "sample.pdf";

  private static final String SAMPLE_DOC = "file-sample_100kB.doc";

  private static final String SAMPLE_DOCX = "file-sample_100kB.docx";

  private static final String SAMPLE_PPT = "file_example_PPT_250kB.ppt";

  private static final String SAMPLE_PPTX = "sample-pptx.pptx";

  private static final String SAMPLE_XLS = "file_example_XLS_10.xls";

  private static final String SAMPLE_XLSX = "file_example_XLSX_10.xlsx";

  private static final String SAMPLE_JPG = "file_example_JPG_100kB.jpg";

  private static final String SAMPLE_TXT = "sample1.txt";

  @Test
  @DisplayName("Text Extraction from PDF")
  public void testExtractTextPdf() throws Exception {
    final String EXPECTED_WORD = "demonstration";
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    try (InputStream inputStream = getFileInputStream(SAMPLE_PDF)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, containsString(EXPECTED_WORD));
    }
  }

  @Test
  @DisplayName("Test Text Extraction from DOC")
  public void testExtractTextDoc() throws Exception {
    final String EXPECTED_WORD = "sollicitudin";
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    try (InputStream inputStream = getFileInputStream(SAMPLE_DOC)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, containsString(EXPECTED_WORD));
    }
  }

  @Test
  @DisplayName("Test Text Extraction from DOCX")
  public void testExtractTextDocx() throws Exception {
    final String EXPECTED_WORD = "Pellentesque";
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    try (InputStream inputStream = getFileInputStream(SAMPLE_DOCX)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, containsString(EXPECTED_WORD));
    }
  }

  @Test
  @DisplayName("Test Text Extraction from PPT")
  public void testExtractTextPpt() throws Exception {
    final String EXPECTED_WORD = "Maecenas";
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    try (InputStream inputStream = getFileInputStream(SAMPLE_PPT)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, containsString(EXPECTED_WORD));
    }
  }

  @Test
  @DisplayName("Test Text Extraction from PPTX")
  public void testExtractTextPptx() throws Exception {
    final String EXPECTED_WORD = "outline";
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    try (InputStream inputStream = getFileInputStream(SAMPLE_PPTX)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, containsString(EXPECTED_WORD));
    }
  }

  @Test
  @DisplayName("Test Text Extraction from XLS")
  public void testExtractTextXls() throws Exception {
    final String EXPECTED_WORD = "Hashimoto";
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    try (InputStream inputStream = getFileInputStream(SAMPLE_XLS)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, containsString(EXPECTED_WORD));
    }
  }

  @Test
  @DisplayName("Test Text Extraction from XLSX")
  public void testExtractTextXlsx() throws Exception {
    final String EXPECTED_WORD = "Melgar";
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    try (InputStream inputStream = getFileInputStream(SAMPLE_XLSX)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, containsString(EXPECTED_WORD));
    }
  }

  @Test
  @DisplayName("Test Text Extraction from JPG")
  public void testExtractTextJpg() throws Exception {
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    try (InputStream inputStream = getFileInputStream(SAMPLE_JPG)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, emptyString());
    }
  }

  @Test
  @DisplayName("Test Text Extraction from null Input Stream")
  public void testExtractTextNullInputStreamThrowsException() {
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor();
    assertThrows(
        IOException.class,
        () -> {
          bodyContentExtractor.extractText(null);
        });
  }

  @Test
  @DisplayName("Test Max Content Length")
  public void testMaxContentLength() throws Exception {
    final String EXPECTED_WORD = "This";
    final int MAX_CONTENT_LENGTH = 4;
    final BodyContentExtractor bodyContentExtractor = new BodyContentExtractor(MAX_CONTENT_LENGTH);
    try (InputStream inputStream = getFileInputStream(SAMPLE_TXT)) {
      final String content = bodyContentExtractor.extractText(inputStream);
      assertThat(content, is(EXPECTED_WORD));
    }
  }

  private static InputStream getFileInputStream(String fileName) {
    final InputStream inputStream =
        BodyContentExtractorTest.class.getClassLoader().getResourceAsStream(fileName);
    return inputStream;
  }
}
