package com.siurbinfo.srs.service.efs;

import com.siurbinfo.srs.dto.efs.EfsResponseDTO;
import com.siurbinfo.srs.exception.*;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.CognitoUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

@Service
public class EfsService {

    private final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg","image/png");
    private final Path basePath;
    private final UserRepository repository;
    private final CognitoUserService userService;

    public EfsService(@Value("${app.efs.base-path}") String basePath, UserRepository repository, CognitoUserService userService){
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
        this.repository = repository;
        this.userService = userService;
    }

    @Transactional
    public EfsResponseDTO uploud(MultipartFile file, String accessToken){

        if(file.isEmpty()){
            throw new InvalidFileException("Arquivo vazio");
        }

        if (file.getContentType() == null || !TIPOS_PERMITIDOS.contains(file.getContentType())){
            throw new InvalidFileException("Tipo não permitido: " + file.getContentType());
        }

        String email = userService.getEmail(accessToken);

        String imgInUser = repository.getImgInUser(email);
        if (imgInUser == null || imgInUser.isBlank()){
            imgInUser = UUID.randomUUID().toString();
        }

        Path pathClient = basePath.resolve("img").resolve(imgInUser).normalize();
        if (!pathClient.startsWith(basePath)){
            throw new SecurityException("Path invalido");
        }

        Path destination = pathClient.resolve("photo.jpeg");

        try {
            Files.createDirectories(pathClient);
            writeAsJpeg(file, destination);
        } catch (IOException e){
            throw new FailedToSaveInEfs("Falha ao gravar no EFS");
        }
        repository.setImgInUser(imgInUser, email);
        // sempre armazenado como JPEG, independente do formato enviado
        return new EfsResponseDTO(imgInUser, file.getSize(), "image/jpeg");
    }

    private void writeAsJpeg(MultipartFile file, Path destination) throws IOException {
        if ("image/jpeg".equals(file.getContentType())){
            try(InputStream in = file.getInputStream()){
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        BufferedImage source;
        try(InputStream in = file.getInputStream()){
            source = ImageIO.read(in);
        }
        if (source == null){
            throw new InvalidFileException("Não foi possível ler a imagem enviada");
        }

        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(source, 0, 0, Color.WHITE, null);
        g.dispose();

        try(OutputStream out = Files.newOutputStream(destination)){
            if (!ImageIO.write(rgb, "jpeg", out)){
                throw new FailedToSaveInEfs("Nenhum encoder JPEG disponível");
            }
        }
    }

    public Resource getImg(String acessToken) {

        String email = userService.getEmail(acessToken);

        String pathClient = repository.getImgInUser(email);
        if (pathClient == null || pathClient.isBlank()){
            throw new PathNotExistException("O caminho não existe no banco");
        }

        Path file = basePath.resolve("img").resolve(pathClient).resolve("photo.jpeg").normalize();
        if (!file.startsWith(basePath)){
            throw new SecurityException("Path invalido");
        }

        if (!Files.exists(file) || !Files.isReadable(file)){
            throw new FileNotExistInEfsException("O arquivo não existe");
        }

        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e){
            throw new InvalidUrlException("A url foi mal formada");
        }
    }
}
