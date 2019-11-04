/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.controllers;

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

  private static final long GIGABYTE = 1 << 30;
  private static final long MAX_FILE_BYTES = 10 * GIGABYTE;

  private MultipartFileValidator() {}

  public static MultipartFile validate(final MultipartFile file) {
    validateSize(file);
    validateContentType(file);
    validateFilename(file);
    validateInputStream(file);
    return file;
  }

  private static void validateInputStream(final MultipartFile file) {
    try {
      file.getInputStream();
    } catch (IOException e) {
      throw new ValidationException("Unable to read file", e);
    }
  }

  private static void validateContentType(final MultipartFile file) {
    final String mediaType = file.getContentType();
    if (isBlank(mediaType)) {
      throw new ValidationException("Media type is missing");
    }
  }

  private static void validateFilename(final MultipartFile file) {
    final String filename = file.getOriginalFilename();
    if (isBlank(filename)) {
      throw new ValidationException("Media type is missing");
    }
  }

  private static void validateSize(final MultipartFile file) {
    final Long fileSize = file.getSize();
    if (fileSize > MAX_FILE_BYTES) {
      throw new ValidationException(
          String.format(
              "File size is %d bytes. File size cannot be greater than %d bytes",
              fileSize, MAX_FILE_BYTES));
    }
  }
}
