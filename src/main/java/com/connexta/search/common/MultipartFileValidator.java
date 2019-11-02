/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import javax.validation.ValidationException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Encapsulate common validations of an incoming MultipartFile. Throw a ValidationException if there
 * is a violation.
 */

// TODO Use a proper assertion library like Truth or Hamcrest instead of if statements
public class MultipartFileValidator {
  private final MultipartFile file;
  private static final long GIGABYTE = 1 << 30;
  private static final long MAX_FILE_BYTES = 10 * GIGABYTE;

  public MultipartFileValidator(MultipartFile file) {
    this.file = file;
  }

  public void validate() {
    validateSize();
    validateContentType();
    validateFilename();
    validateInputStream();
  }

  private void validateInputStream() {
    try {
      file.getInputStream();
    } catch (IOException e) {
      throw new ValidationException("Unable to read file", e);
    }
  }

  private void validateContentType() {
    final String mediaType = file.getContentType();
    if (isBlank(mediaType)) {
      throw new ValidationException("Media type is missing");
    }
  }

  private void validateFilename() {
    final String filename = file.getOriginalFilename();
    if (isBlank(filename)) {
      throw new ValidationException("Media type is missing");
    }
  }

  private void validateSize() {
    final Long fileSize = file.getSize();
    if (fileSize > MAX_FILE_BYTES) {
      throw new ValidationException(
          String.format(
              "File size is %d bytes. File size cannot be greater than %d bytes",
              fileSize, MAX_FILE_BYTES));
    }
  }
}
