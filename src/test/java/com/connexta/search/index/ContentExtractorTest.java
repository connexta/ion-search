/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.connexta.search.index.exceptions.ContentException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentExtractorTest {

  private final Tika mockTika = mock(Tika.class);
  private final InputStream mockInputStream = mock(InputStream.class);

  @AfterEach
  void afterEach() {
    reset(mockTika, mockInputStream);
  }

  @Test
  @DisplayName("Test Construction")
  void testConstruction() throws Exception {
    new ContentExtractorImpl(mockTika, 1);
    verify(mockTika).setMaxStringLength(1);
  }

  @Test
  @DisplayName("Test Extraction")
  void testExtraction() throws ContentException, IOException, TikaException {
    final String text = "contents";
    final int maxLength = text.length();
    doReturn(text).when(mockTika).parseToString(mockInputStream);
    ContentExtractor contentExtractor = new ContentExtractorImpl(mockTika, maxLength);
    assertThat(contentExtractor.extractText(mockInputStream), is(text));
  }

  @Test
  @DisplayName("Test Null Input")
  void testNullInput() {
    assertThrows(
        ContentException.class, () -> new ContentExtractorImpl(mockTika, 1).extractText(null));
  }

  @Test
  @DisplayName("Test Parsing Exception")
  void testParsingException() throws IOException, TikaException {
    final IOException ioException = new IOException();
    doThrow(ioException).when(mockTika).parseToString(mockInputStream);
    final Throwable throwable =
        assertThrows(
            ContentException.class,
            () -> new ContentExtractorImpl(mockTika, 1).extractText(mockInputStream));
    assertThat(throwable.getCause(), is(ioException));
  }
}
