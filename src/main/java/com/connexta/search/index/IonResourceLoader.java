/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import java.io.IOException;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ResourceLoader;

@AllArgsConstructor
public class IonResourceLoader {

  private final ResourceLoader resourceLoader;

  public InputStream get(String resourceUrl) throws IOException {
    return resourceLoader.getResource(resourceUrl).getInputStream();
  }
}
