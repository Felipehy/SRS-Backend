package com.siurbinfo.srs.service.efs;

import com.siurbinfo.srs.exception.FileNotExistInEfsException;
import com.siurbinfo.srs.exception.InvalidFileException;
import com.siurbinfo.srs.exception.PathNotExistException;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.CognitoUserService;
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

/**
 * Testes unitarios do EfsService.
 *
 * UserRepository e CognitoUserService sao mockados; o diretorio base do EFS e
 * um @TempDir, entao cada teste manipula arquivos reais isolados, sem tocar em
 * AWS nem no filesystem do projeto.
 */
class EfsServiceTest {

    private static final String TOKEN = "access-token";
    private static final String EMAIL = "fulano@empresa.com";
    private static final String IMG_DIR = "b1f8c2e4-0000-0000-0000-000000000000";

    @TempDir
    Path basePath;

    private UserRepository repository;
    private CognitoUserService userService;
    private EfsService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        userService = mock(CognitoUserService.class);
        service = new EfsService(basePath.toString(), repository, userService);
    }

    /** Cria fisicamente basePath/img/{dir}/photo.jpeg, como um upload teria feito. */
    private void createStoredImage(String dir) throws IOException {
        Path folder = basePath.resolve("img").resolve(dir);
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("photo.jpeg"), "fake-image-bytes");
    }

    // ---------- getImg ----------

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
        // registro existe no banco, mas o arquivo nao foi gravado no EFS
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);

        assertThrows(FileNotExistInEfsException.class, () -> service.getImg(TOKEN));
    }

    @Test
    void getImg_pathTraversal_throwsSecurity() {
        // valor malicioso no banco tentando escapar do diretorio base
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);
        when(repository.getImgInUser(EMAIL)).thenReturn("../../evil");

        assertThrows(SecurityException.class, () -> service.getImg(TOKEN));
    }

    // ---------- uploud (validacoes) ----------

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

    // ---------- round-trip: uploud -> getImg ----------

    @Test
    void uploudThenGetImg_returnsSameContent() throws IOException {
        byte[] uploaded = "conteudo-da-imagem".getBytes(StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile("file", "foto.jpeg", "image/jpeg", uploaded);

        // mesmo diretorio no banco nas duas chamadas: uploud reaproveita, getImg le
        when(repository.getImgInUser(EMAIL)).thenReturn(IMG_DIR);
        when(userService.getEmail(TOKEN)).thenReturn(EMAIL);

        // grava no EFS
        service.uploud(file, TOKEN);

        // arquivo salvo como photo.jpeg no diretorio do usuario + registro atualizado
        Path stored = basePath.resolve("img").resolve(IMG_DIR).resolve("photo.jpeg");
        assertTrue(Files.exists(stored));
        verify(repository).setImgInUser(IMG_DIR, EMAIL);

        // le de volta e confere que o conteudo bate byte a byte
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
        // gera um PNG valido em memoria (10x10) com canal de transparencia
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

        // os bytes gravados sao realmente JPEG (magic bytes FF D8 FF), nao o PNG original
        byte[] storedBytes = Files.readAllBytes(stored);
        assertEquals((byte) 0xFF, storedBytes[0]);
        assertEquals((byte) 0xD8, storedBytes[1]);
        assertEquals((byte) 0xFF, storedBytes[2]);

        // e continua legivel como imagem, com as mesmas dimensoes
        BufferedImage reread = ImageIO.read(stored.toFile());
        assertNotNull(reread);
        assertEquals(10, reread.getWidth());
        assertEquals(10, reread.getHeight());
    }

    @Test
    void uploud_webp_isRejected() {
        // webp saiu de TIPOS_PERMITIDOS: deve ser recusado
        MultipartFile webp = new MockMultipartFile("file", "foto.webp", "image/webp", new byte[]{1, 2, 3});

        assertThrows(InvalidFileException.class,
                () -> service.uploud(webp, TOKEN));
    }
}
