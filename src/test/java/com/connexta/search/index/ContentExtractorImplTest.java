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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.connexta.search.index.exceptions.ContentException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentExtractorImplTest {

  @Mock private Tika mockTika;
  @Mock private InputStream mockInputStream;

  private ContentExtractor contentExtractor;

  @BeforeEach
  void beforeEach() {
    contentExtractor = new ContentExtractorImpl(mockTika);
  }

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(mockTika, mockInputStream);
  }

  @Test
  @DisplayName("Test null Tika")
  void testNullTika() {
    assertThrows(NullPointerException.class, () -> new ContentExtractorImpl(null));
  }

  @Test
  @DisplayName("Test Null Input")
  void testNullInput() {
    assertThrows(NullPointerException.class, () -> contentExtractor.extractText(null));
  }

  @Test
  @DisplayName("Test Parsing Exception")
  void testParsingException() throws IOException, TikaException {
    final IOException ioException = new IOException();
    doThrow(ioException).when(mockTika).parseToString(mockInputStream);
    final Throwable throwable =
        assertThrows(ContentException.class, () -> contentExtractor.extractText(mockInputStream));
    assertThat(throwable.getCause(), is(ioException));
  }
}
