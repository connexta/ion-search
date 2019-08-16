/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import com.connexta.search.query.exceptions.QueryException;
import java.net.URI;
import java.util.List;

public interface QueryManager {

  List<URI> find(String keyword) throws QueryException;
}
