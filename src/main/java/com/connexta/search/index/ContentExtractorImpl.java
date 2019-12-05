/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.apache.commons.lang3.Validate.notNull;

import com.connexta.search.index.exceptions.ContentException;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class ContentExtractorImpl implements ContentExtractor {

  private final Tika tika;

  /** @throws NullPointerException if {@code tika} is {@code null} */
  public ContentExtractorImpl(@NonNull Tika tika) {
    this.tika = tika;
  }

  /** @throws NullPointerException if {@code inputStream} is {@code null} */
  @Override
  public String extractText(@NonNull InputStream inputStream) throws ContentException {
    /* TODO: This will load the entire contents into memory. It will become a problem eventually. The easiest solution is to create a file and stream the contents to it. */
    try {
      return tika.parseToString(notNull(inputStream));
    } catch (IOException | TikaException e) {
      throw new ContentException("Could not extract text", e);
    }
  }
}
