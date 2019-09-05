/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import com.connexta.search.index.exceptions.IndexException;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public interface IndexManager {

  void index(@NotBlank String productId, @NotBlank String mediaType, @NotNull MultipartFile file)
      throws UnsupportedOperationException, IndexException;
}
