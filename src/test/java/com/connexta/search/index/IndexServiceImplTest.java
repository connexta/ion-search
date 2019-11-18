/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.connexta.search.common.Index;
import com.connexta.search.common.IndexRepository;
import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.index.exceptions.ContentException;
import com.connexta.search.rest.models.IndexRequest;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class IndexServiceImplTest {

  static final String DATASET_ID = "00067360b70e4acfab561fe593ad3f7a";
  static final String FILE_URL = "http://file";
  static final String IRM_URL = "http://irm";

  final ContentExtractor mockContentExtractor = mock(ContentExtractor.class);
  final IndexRepository mockIndexRepository = mock(IndexRepository.class);
  final IonResourceLoader mockIonResourceLoader = mock(IonResourceLoader.class);

  final IndexService indexService =
      new IndexServiceImpl(mockIndexRepository, mockIonResourceLoader, mockContentExtractor);
  Index testIndex;
  IndexRequest indexRequest;

  @BeforeEach
  void beforeEach() {
    indexRequest = new IndexRequest();
    indexRequest.setFileLocation(FILE_URL);
    indexRequest.setIrmLocation(IRM_URL);
  }

  @AfterEach
  void afterEach() {
    reset(mockContentExtractor, mockIndexRepository, mockIonResourceLoader);
  }

  @Test
  void testIndex(@Mock final InputStream mockInputStream) throws Exception {
    doReturn("irm").when(mockIonResourceLoader).getAsString(IRM_URL);
    doReturn(mockInputStream).when(mockIonResourceLoader).get(FILE_URL);
    doReturn("FileContents").when(mockContentExtractor).extractText(mockInputStream);
    indexService.index(DATASET_ID, indexRequest);
    ArgumentCaptor<Index> indexCaptor = ArgumentCaptor.forClass(Index.class);
    verify(mockIndexRepository).save(indexCaptor.capture());
    Index index = indexCaptor.getValue();
    assertThat(index.getId(), is(DATASET_ID));
    assertThat(index.getFileUrl(), is(FILE_URL));
    assertThat(index.getIrmUrl(), is(IRM_URL));
    assertThat(index.getContents(), is("FileContents"));
    assertThat(index.getTitle(), is("irm"));
    assertThat(index.getCountryCode(), nullValue());
    assertThat(index.getCreated(), nullValue());
    assertThat(index.getMetacardUrl(), nullValue());
    assertThat(index.getModified(), nullValue());
  }

  @Test
  void testBadInputStreamWhenProcessingIrm() throws Exception {
    IOException ioException = new IOException();
    doThrow(ioException).when(mockIonResourceLoader).getAsString(anyString());
    expectException(ioException, BAD_REQUEST);
    verify(mockIndexRepository, never()).save(any());
  }

  @Test
  void testDatasetAlreadyExists() {
    // given
    // stub dataset already exists
    when(mockIndexRepository.existsById(DATASET_ID)).thenReturn(true);

    // expect
    final SearchException thrown =
        assertThrows(SearchException.class, () -> indexService.index(DATASET_ID, indexRequest));
    assertThat(thrown.getStatus(), is(BAD_REQUEST));
    verify(mockIndexRepository, never()).save(any());
  }

  @Test
  void testExceptionWhenCheckingIfDatasetExists() {
    // stub dataset already exists
    final RuntimeException runtimeException = new RuntimeException();
    doThrow(runtimeException).when(mockIndexRepository).existsById(DATASET_ID);

    // expect
    expectException(runtimeException, HttpStatus.INTERNAL_SERVER_ERROR);
    verify(mockIndexRepository, never()).save(any());
  }

  @Test
  void testExceptionWhenSaving() throws Exception {
    // given
    doReturn("fileContents").when(mockContentExtractor).extractText(any(InputStream.class));

    // and stub CrudRepository#existsById
    when(mockIndexRepository.existsById(DATASET_ID)).thenReturn(false);

    // and stub CrudRepository#save
    final RuntimeException runtimeException = new RuntimeException();
    doThrow(runtimeException).when(mockIndexRepository).save(any());

    // expect
    expectException(runtimeException, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void testUnableToReadIrmBody() throws Exception {
    final IOException ioException = new IOException();
    doThrow(ioException).when(mockIonResourceLoader).getAsString(IRM_URL);
    expectException(ioException, BAD_REQUEST);
    verify(mockIndexRepository, never()).save(any());
  }

  @Test
  void testExceptionsWhileExtractingText(@Mock final InputStream inputStream) throws Exception {
    final ContentException exception = new ContentException("");
    doReturn(inputStream).when(mockIonResourceLoader).get(FILE_URL);
    doThrow(exception).when(mockContentExtractor).extractText(inputStream);
    expectException(exception, HttpStatus.INTERNAL_SERVER_ERROR);
    verify(mockIndexRepository, never()).save(any());
  }

  void expectException(Throwable throwable, HttpStatus httpStatus) {
    SearchException thrown =
        assertThrows(SearchException.class, () -> indexService.index(DATASET_ID, indexRequest));
    assertThat(thrown.getStatus(), is(httpStatus));
    assertThat(thrown.getCause(), is(throwable));
  }
}
