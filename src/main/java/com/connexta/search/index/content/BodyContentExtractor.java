/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.content;

import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class BodyContentExtractor {

  private static final int NO_LIMIT_MAX_CONTENT_LENGTH = -1;

  private final Tika tika;

  public BodyContentExtractor() {
    tika = new Tika();
    tika.setMaxStringLength(NO_LIMIT_MAX_CONTENT_LENGTH);
  }

  public BodyContentExtractor(int maxContentLength) {
    this.tika = new Tika();
    tika.setMaxStringLength(maxContentLength);
  }

  public String extractText(InputStream inputStream) throws IOException, TikaException {
    if (inputStream == null) {
      throw new IOException("Cannot extract text. Null input stream.");
    }
    return tika.parseToString(inputStream);
  }
}
