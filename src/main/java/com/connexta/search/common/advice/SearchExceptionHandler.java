/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common.advice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.connexta.search.common.exceptions.DetailedErrorAttributes;
import com.connexta.search.index.exceptions.IndexException;
import com.connexta.search.query.exceptions.QueryException;
import java.util.Map;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class SearchExceptionHandler extends ResponseEntityExceptionHandler {

  @Inject private DetailedErrorAttributes detailedErrorAttributes;

  @ExceptionHandler(ConstraintViolationException.class)
  protected ResponseEntity<Object> handleConstraintViolation(
      @NotNull final ConstraintViolationException e, @NotNull final WebRequest request) {
    log.warn("Request is invalid: {}. Returning {}.", e.getMessage(), BAD_REQUEST, e);
    Map<String, Object> serializableError =
        detailedErrorAttributes.getErrorAttributes(request, false);
    return new ResponseEntity<>(serializableError, BAD_REQUEST);
  }

  @ExceptionHandler(QueryException.class)
  protected ResponseEntity<Object> handleQueryException(
      @NotNull final QueryException e, final WebRequest request) {
    log.warn("Query exception", e);
    return new ResponseEntity<>(e, e.getStatus());
  }

  @ExceptionHandler(IndexException.class)
  protected ResponseEntity<Object> handleIndexException(
      @NotNull final IndexException e, final WebRequest request) {
    log.warn("Index exception", e);
    return new ResponseEntity<>(e, e.getStatus());
  }
}
