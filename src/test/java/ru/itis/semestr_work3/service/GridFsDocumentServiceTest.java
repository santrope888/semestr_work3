package ru.itis.semestr_work3.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GridFsDocumentServiceTest {

    @Mock
    private GridFsTemplate gridFsTemplate;

    @InjectMocks
    private GridFsDocumentService gridFsDocumentService;

    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile(
                "file", "passport.jpg", "image/jpeg", "content".getBytes());
    }

    @Test
    void store_withValidFile_returnsHexId() {
        ObjectId objectId = new ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString()))
                .thenReturn(objectId);

        String result = gridFsDocumentService.store(validFile);

        assertThat(result).isEqualTo(objectId.toHexString());
    }

    @Test
    void store_whenFileNull_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> gridFsDocumentService.store(null));
        verify(gridFsTemplate, never()).store(any(), anyString(), anyString());
    }

    @Test
    void store_whenFileEmpty_throwsException() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> gridFsDocumentService.store(empty));
    }

    @Test
    void store_whenFileTooLarge_throwsException() {
        byte[] big = new byte[6 * 1024 * 1024];
        MockMultipartFile large = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", big);

        assertThrows(IllegalArgumentException.class,
                () -> gridFsDocumentService.store(large));
    }

    @Test
    void store_whenContentTypeNull_throwsException() {
        MockMultipartFile noType = new MockMultipartFile(
                "file", "x.jpg", null, "data".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> gridFsDocumentService.store(noType));
    }

    @Test
    void store_whenContentTypeNotAllowed_throwsException() {
        MockMultipartFile html = new MockMultipartFile(
                "file", "evil.html", "text/html", "<script>".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> gridFsDocumentService.store(html));
    }

    @Test
    void store_whenInputStreamFails_throwsIllegalState() throws IOException {
        MultipartFile failing = mock(MultipartFile.class);
        when(failing.isEmpty()).thenReturn(false);
        when(failing.getSize()).thenReturn(100L);
        when(failing.getContentType()).thenReturn("application/pdf");
        when(failing.getInputStream()).thenThrow(new IOException("boom"));

        assertThrows(IllegalStateException.class,
                () -> gridFsDocumentService.store(failing));
    }

    @Test
    void store_acceptsPdf() {
        ObjectId objectId = new ObjectId();
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdfdata".getBytes());
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString()))
                .thenReturn(objectId);

        String result = gridFsDocumentService.store(pdf);

        assertThat(result).isEqualTo(objectId.toHexString());
    }

    @Test
    void load_withValidId_returnsResource() {
        ObjectId objectId = new ObjectId();
        String hexId = objectId.toHexString();
        GridFSFile gridFSFile = mock(GridFSFile.class);
        GridFsResource resource = mock(GridFsResource.class);

        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(gridFSFile);
        when(gridFsTemplate.getResource(gridFSFile)).thenReturn(resource);

        GridFsResource result = gridFsDocumentService.load(hexId);

        assertThat(result).isSameAs(resource);
    }

    @Test
    void load_whenIdNull_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> gridFsDocumentService.load(null));
    }

    @Test
    void load_whenIdBlank_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> gridFsDocumentService.load("   "));
    }

    @Test
    void load_whenFileNotFound_throwsException() {
        ObjectId objectId = new ObjectId();
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> gridFsDocumentService.load(objectId.toHexString()));
    }

    @Test
    void delete_withValidId_callsTemplate() {
        ObjectId objectId = new ObjectId();

        gridFsDocumentService.delete(objectId.toHexString());

        verify(gridFsTemplate).delete(any(Query.class));
    }

    @Test
    void delete_whenIdNull_doesNothing() {
        gridFsDocumentService.delete(null);

        verify(gridFsTemplate, never()).delete(any(Query.class));
    }

    @Test
    void delete_whenIdBlank_doesNothing() {
        gridFsDocumentService.delete("  ");

        verify(gridFsTemplate, never()).delete(any(Query.class));
    }
}