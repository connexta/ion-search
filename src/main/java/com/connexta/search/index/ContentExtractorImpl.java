/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import com.connexta.search.index.exceptions.ContentException;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.Min;
import lombok.NonNull;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

public class ContentExtractorImpl implements ContentExtractor {

  private final Tika tika;

  public ContentExtractorImpl(@NonNull Tika tika, @Min(-1) int maxLength) {
    this.tika = tika;
    tika.setMaxStringLength(maxLength);
  }

  @Override
  public String extractText(@NonNull InputStream inputStream) throws ContentException {
    if (inputStream == null) {
      throw new ContentException("Cannot extract text. Null input stream.");
    }
    /* TODO: This will load the entire contents into memory. It will become a problem eventually. The easiest solution is to create a file and stream the contents to it. */
    try {
      return tika.parseToString(inputStream);
    } catch (IOException | TikaException e) {
      throw new ContentException("Could not extract text", e);
    }
  }
}
