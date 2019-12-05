/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.connexta.search.index.exceptions.ContentException;
import java.io.ByteArrayInputStream;
import org.apache.tika.Tika;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ContentExtractorImplITest {

  @Test
  @DisplayName("Test Extraction")
  void testExtraction() throws ContentException {
    final Tika tika = new Tika();
    tika.setMaxStringLength(10485760);

    final String text = "contents";
    // TODO Not sure why \n is needed
    assertThat(
        new ContentExtractorImpl(tika).extractText(new ByteArrayInputStream(text.getBytes())),
        is(text + "\n"));
  }
}
