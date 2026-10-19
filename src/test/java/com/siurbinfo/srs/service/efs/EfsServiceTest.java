package com.siurbinfo.srs.service.efs;

import com.siurbinfo.srs.exception.EmailNotFoundedException;
import com.siurbinfo.srs.exception.FileNotExistInEfsException;
import com.siurbinfo.srs.exception.InvalidFileException;
import com.siurbinfo.srs.exception.PathNotExistException;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EfsServiceTest {

    private static final String TOKEN = "access-token";
    private static final String EMAIL = "fulano@empresa.com";
    private static final String IMG_DIR = "b1f8c2e4-0000-0000-0000-000000000000";

    @TempDir
    Path basePath;

    private UserRepository repository;
    private CognitoUserServiceImpl userService;
    private EfsService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        userService = mock(CognitoUserServiceImpl.class);
        service = new EfsService(basePath.toString(), repository, userService);
    }

    private void createStoredImage(String dir) throws IOException {
        Path folder = basePath.resolve("img").resolve(dir);
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("photo.jpeg"), "fake-image-bytes");
    }

    @Test
    void getImg_returnsReadableResource_whenFileExists() throws IOException {
        createStoredImage(IMG_DIR);
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);

        Resource resource = service.getImg(TOKEN);

        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void getImg_noRecordInDb_throwsPathNotExist() {
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn(null);

        assertThrows(PathNotExistException.class, () -> service.getImg(TOKEN));
    }

    @Test
    void getImg_blankRecordInDb_throwsPathNotExist() {
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn("   ");

        assertThrows(PathNotExistException.class, () -> service.getImg(TOKEN));
    }

    @Test
    void getImg_fileMissingOnDisk_throwsFileNotExist() {
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);

        assertThrows(FileNotExistInEfsException.class, () -> service.getImg(TOKEN));
    }

    @Test
    void getImg_pathTraversal_throwsSecurity() {
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn("../../evil");

        assertThrows(SecurityException.class, () -> service.getImg(TOKEN));
    }

    @Test
    void getImgByParam_returnsReadableResource_whenFileExists() throws IOException {
        createStoredImage(IMG_DIR);
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);

        Resource resource = service.getImgByParam(EMAIL);

        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
        verifyNoInteractions(userService);
    }

    @Test
    void getImgByParam_noRecordInDb_throwsPathNotExist() {
        when(repository.getImgInUser(EMAIL)).thenReturn(null);

        assertThrows(PathNotExistException.class, () -> service.getImgByParam(EMAIL));
    }

    @Test
    void getImgByParam_blankRecordInDb_throwsPathNotExist() {
        when(repository.getImgInUser(EMAIL)).thenReturn("   ");

        assertThrows(PathNotExistException.class, () -> service.getImgByParam(EMAIL));
    }

    @Test
    void getImgByParam_fileMissingOnDisk_throwsFileNotExist() {
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);

        assertThrows(FileNotExistInEfsException.class, () -> service.getImgByParam(EMAIL));
    }

    @Test
    void getImgByParam_pathTraversal_throwsSecurity() {
        when(repository.getImgInUser(EMAIL)).thenReturn("../../evil");

        assertThrows(SecurityException.class, () -> service.getImgByParam(EMAIL));
    }

    @Test
    void uploud_emptyFile_throwsInvalidFile() {
        MultipartFile empty = new MockMultipartFile("file", "photo.jpeg", "image/jpeg", new byte[0]);

        assertThrows(InvalidFileException.class,
                () -> service.uploud(empty, TOKEN));
    }

    @Test
    void uploud_unsupportedContentType_throwsInvalidFile() {
        MultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(InvalidFileException.class,
                () -> service.uploud(pdf, TOKEN));
    }

    @Test
    void uploudThenGetImg_returnsSameContent() throws IOException {
        byte[] uploaded = "conteudo-da-imagem".getBytes(StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile("file", "foto.jpeg", "image/jpeg", uploaded);

        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);

        service.uploud(file, TOKEN);

        Path stored = basePath.resolve("img").resolve(IMG_DIR).resolve("photo.jpeg");
        assertTrue(Files.exists(stored));
        verify(repository).setImgInUser(IMG_DIR, EMAIL);

        Resource resource = service.getImg(TOKEN);

        assertTrue(resource.exists());
        byte[] readBack;
        try (InputStream in = resource.getInputStream()) {
            readBack = in.readAllBytes();
        }
        assertArrayEquals(uploaded, readBack);
    }

    @Test
    void uploud_pngIsConvertedToJpeg() throws IOException {
        BufferedImage png = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = png.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 10, 10);
        g.dispose();
        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(png, "png", pngBytes);

        MultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", pngBytes.toByteArray());
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);

        service.uploud(file, TOKEN);

        Path stored = basePath.resolve("img").resolve(IMG_DIR).resolve("photo.jpeg");
        assertTrue(Files.exists(stored));

        byte[] storedBytes = Files.readAllBytes(stored);
        assertEquals((byte) 0xFF, storedBytes[0]);
        assertEquals((byte) 0xD8, storedBytes[1]);
        assertEquals((byte) 0xFF, storedBytes[2]);

        BufferedImage reread = ImageIO.read(stored.toFile());
        assertNotNull(reread);
        assertEquals(10, reread.getWidth());
        assertEquals(10, reread.getHeight());
    }

    @Test
    void uploud_webp_isRejected() {
        MultipartFile webp = new MockMultipartFile("file", "foto.webp", "image/webp", new byte[]{1, 2, 3});

        assertThrows(InvalidFileException.class,
                () -> service.uploud(webp, TOKEN));
    }

    @Test
    void deleteImg_removesFileAndKeepsDbRecord_whenExists() throws IOException {
        createStoredImage(IMG_DIR);
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);

        service.deleteImg(TOKEN);

        Path dir = basePath.resolve("img").resolve(IMG_DIR);
        assertFalse(Files.exists(dir.resolve("photo.jpeg")));
        assertTrue(Files.exists(dir));
        verify(repository, never()).deleteImgByEmail(anyString());
    }

    @Test
    void deleteImg_nullEmail_throwsEmailNotFounded() {
        when(userService.getEmail(TOKEN)).thenReturn(null);

        assertThrows(EmailNotFoundedException.class, () -> service.deleteImg(TOKEN));
        verify(repository, never()).deleteImgByEmail(anyString());
    }

    @Test
    void deleteImg_blankEmail_throwsEmailNotFounded() {
        when(userService.getEmail(TOKEN)).thenReturn("   ");

        assertThrows(EmailNotFoundedException.class, () -> service.deleteImg(TOKEN));
        verify(repository, never()).deleteImgByEmail(anyString());
    }

    @Test
    void deleteImg_noRecordInDb_throwsPathNotExist() {
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn(null);

        assertThrows(PathNotExistException.class, () -> service.deleteImg(TOKEN));
        verify(repository, never()).deleteImgByEmail(anyString());
    }

    @Test
    void deleteImg_blankRecordInDb_throwsPathNotExist() {
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn("   ");

        assertThrows(PathNotExistException.class, () -> service.deleteImg(TOKEN));
        verify(repository, never()).deleteImgByEmail(anyString());
    }

    @Test
    void deleteImg_folderMissingOnDisk_throwsFileNotExist() {
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);

        assertThrows(FileNotExistInEfsException.class, () -> service.deleteImg(TOKEN));
        verify(repository, never()).deleteImgByEmail(anyString());
    }

    @Test
    void deleteImg_pathTraversal_throwsSecurity() {
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn("../../evil");

        assertThrows(SecurityException.class, () -> service.deleteImg(TOKEN));
        verify(repository, never()).deleteImgByEmail(anyString());
    }
}
