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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.index.exceptions.ContentException;
import com.connexta.search.rest.models.IndexRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
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

  private static final String FILE_URL = "http://file";
  private static final String IRM_URL = "http://irm";
  private static final String METACARD_URL = "http://metacard";
  private static final IndexRequest INDEX_REQUEST =
      new IndexRequest().fileLocation(FILE_URL).irmLocation(IRM_URL).metacardLocation(METACARD_URL);

  @Mock private ContentExtractor mockContentExtractor;
  @Mock private IndexStorageAdaptor mockIndexStorageAdaptor;
  @Mock private IonResourceLoader mockIonResourceLoader;

  private IndexService indexService;

  @BeforeEach
  void beforeEach() {
    indexService =
        new IndexServiceImpl(mockIndexStorageAdaptor, mockIonResourceLoader, mockContentExtractor);
  }

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(mockContentExtractor, mockIndexStorageAdaptor, mockIonResourceLoader);
  }

  @Test
  void testIndex(@Mock final InputStream mockInputStream) throws Exception {
    final UUID datasetId = UUID.randomUUID();

    when(mockIndexStorageAdaptor.existsById(datasetId.toString())).thenReturn(false);
    doReturn(mockInputStream).when(mockIonResourceLoader).get(FILE_URL);
    doReturn("FileContents").when(mockContentExtractor).extractText(mockInputStream);
    indexService.index(datasetId, INDEX_REQUEST);
    ArgumentCaptor<Index> indexCaptor = ArgumentCaptor.forClass(Index.class);
    verify(mockIndexStorageAdaptor).save(indexCaptor.capture());
    Index index = indexCaptor.getValue();
    assertThat(index.getId(), is(datasetId.toString()));
    assertThat(index.getFileUrl(), is(FILE_URL));
    assertThat(index.getIrmUrl(), is(IRM_URL));
    assertThat(index.getContents(), is("FileContents"));
    assertThat(index.getCountryCode(), nullValue());
    assertThat(index.getCreated(), nullValue());
    assertThat(index.getMetacardUrl(), is(METACARD_URL));
    assertThat(index.getModified(), nullValue());

    verify(mockInputStream).close();
    verifyNoMoreInteractions(mockInputStream);
  }

  @Test
  void testDatasetAlreadyExists() {
    // given
    final UUID datasetId = UUID.randomUUID();
    // stub dataset already exists
    when(mockIndexStorageAdaptor.existsById(datasetId.toString())).thenReturn(true);

    // expect
    final SearchException thrown =
        assertThrows(SearchException.class, () -> indexService.index(datasetId, INDEX_REQUEST));
    assertThat(thrown.getStatus(), is(BAD_REQUEST));
    verify(mockIndexStorageAdaptor, never()).save(any());
  }

  @Test
  void testExceptionWhenCheckingIfDatasetExists() {
    // stub dataset already exists
    final RuntimeException runtimeException = new RuntimeException();
    final UUID datasetId = UUID.randomUUID();
    doThrow(runtimeException).when(mockIndexStorageAdaptor).existsById(datasetId.toString());

    // expect
    expectException(runtimeException, HttpStatus.INTERNAL_SERVER_ERROR, datasetId);
    verify(mockIndexStorageAdaptor, never()).save(any());
  }

  @Test
  void testBadInputStreamWhenProcessingFile() throws Exception {
    when(mockIndexStorageAdaptor.existsById(anyString())).thenReturn(false);
    IOException ioException = new IOException();
    doThrow(ioException).when(mockIonResourceLoader).get(anyString());
    expectException(ioException, INTERNAL_SERVER_ERROR, UUID.randomUUID());
    verify(mockIndexStorageAdaptor, never()).save(any());
  }

  @Test
  void testExceptionWhileExtractingText(@Mock final InputStream mockInputStream) throws Exception {
    final UUID datasetId = UUID.randomUUID();
    when(mockIndexStorageAdaptor.existsById(datasetId.toString())).thenReturn(false);
    final ContentException exception = new ContentException("");
    doReturn(mockInputStream).when(mockIonResourceLoader).get(FILE_URL);
    doThrow(exception).when(mockContentExtractor).extractText(mockInputStream);
    expectException(exception, HttpStatus.INTERNAL_SERVER_ERROR, datasetId);
    verify(mockIndexStorageAdaptor, never()).save(any());
    verify(mockInputStream).close();
    verifyNoMoreInteractions(mockInputStream);
  }

  @Test
  void testExceptionsWhenClosingInputStream(@Mock final InputStream mockInputStream)
      throws Exception {
    final UUID datasetId = UUID.randomUUID();
    when(mockIndexStorageAdaptor.existsById(datasetId.toString())).thenReturn(false);
    final IOException ioException = new IOException("");
    doReturn(mockInputStream).when(mockIonResourceLoader).get(FILE_URL);
    doReturn("FileContents").when(mockContentExtractor).extractText(mockInputStream);
    doThrow(ioException).when(mockInputStream).close();
    expectException(ioException, HttpStatus.INTERNAL_SERVER_ERROR, datasetId);
    verify(mockIndexStorageAdaptor, never()).save(any());
    verify(mockInputStream).close();
    verifyNoMoreInteractions(mockInputStream);
  }

  @Test
  void testExceptionWhenSaving(@Mock final InputStream mockInputStream) throws Exception {
    // given
    final UUID datasetId = UUID.randomUUID();
    when(mockIndexStorageAdaptor.existsById(datasetId.toString())).thenReturn(false);
    doReturn(mockInputStream).when(mockIonResourceLoader).get(FILE_URL);
    doReturn("FileContents").when(mockContentExtractor).extractText(mockInputStream);

    // and stub CrudRepository#existsById
    when(mockIndexStorageAdaptor.existsById(datasetId.toString())).thenReturn(false);

    // and stub CrudRepository#save
    final RuntimeException runtimeException = new RuntimeException();
    doThrow(runtimeException).when(mockIndexStorageAdaptor).save(any());

    // expect
    expectException(runtimeException, HttpStatus.INTERNAL_SERVER_ERROR, datasetId);
    verify(mockInputStream).close();
    verifyNoMoreInteractions(mockInputStream);
  }

  void expectException(Throwable throwable, HttpStatus httpStatus, UUID datasetId) {
    SearchException thrown =
        assertThrows(SearchException.class, () -> indexService.index(datasetId, INDEX_REQUEST));
    assertThat(thrown.getStatus(), is(httpStatus));
    assertThat(thrown.getCause(), is(throwable));
  }
}
