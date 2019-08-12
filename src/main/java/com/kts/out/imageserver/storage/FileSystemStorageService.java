package com.kts.out.imageserver.storage;

import com.kts.out.imageserver.exception.StorageException;
import com.kts.out.imageserver.exception.StorageFileNotFoundException;
import com.kts.out.imageserver.service.StorageService;
import com.kts.out.imageserver.utils.ErrorConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileSystemStorageService implements StorageService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }


    @PostConstruct
    public void init() {
        try {
            if(!Files.isDirectory(rootLocation))
                Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new StorageException(ErrorConst.STORAGE_EXCEPTION, e);
        }
    }

    public void checkDir(Path dailyLocation) {
        try {
            if (!Files.isDirectory(dailyLocation)) {
                Files.createDirectories(dailyLocation);
            }
        }catch(IOException e) {
            throw new StorageException(ErrorConst.STORAGE_EXCEPTION, e);
        }
    }

    @Override
    public void store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new StorageException(ErrorConst.EMPTY_FILE_EXCEPTION + ", " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new StorageException(ErrorConst.NO_FILE_PATH_EXCEPTION + ", " +  filename);
            }
            if(filename.toUpperCase().endsWith("JPEG") || filename.toUpperCase().endsWith("JPG") || filename.toUpperCase().endsWith("PNG")) {
                try (FileInputStream inputStream = (FileInputStream)file.getInputStream()) {
                    Files.copy(inputStream, this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new StorageException(ErrorConst.SAVE_FILE_EXCEPTION + ", " +  filename, e);
                }
            }else
                throw new StorageException(ErrorConst.NOT_SUPPORTED_FILE);
        }catch(Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void store(String filename, MultipartFile file) {
        Path dayLocation = Paths.get(this.rootLocation + "/" + todaysDir());
        checkDir(dayLocation);
        try {
            if (file.isEmpty()) {
                throw new StorageException(ErrorConst.EMPTY_FILE_EXCEPTION + ", " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new StorageException(ErrorConst.NO_FILE_PATH_EXCEPTION + ", " + filename);
            }
            if(filename.toUpperCase().endsWith("JPEG") || filename.toUpperCase().endsWith("JPG") || filename.toUpperCase().endsWith("PNG")) {
                try (FileInputStream inputStream = (FileInputStream)file.getInputStream()) {
                    Files.copy(inputStream, dayLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new StorageException(ErrorConst.SAVE_FILE_EXCEPTION  + filename, e);
                }
            }else
                throw new StorageException(ErrorConst.NOT_SUPPORTED_FILE);
        }catch(Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                .filter(path -> !path.equals(this.rootLocation))
                .map(this.rootLocation::relativize);
        }
        catch (IOException e) {
            throw new StorageException("저장된 파일을 읽어올 수 없습니다.", e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("파일을 읽을 수 없습니다. " + filename, e);
        }
    }

    public void delete(String filename) {
        try {
            Path path = load(filename);
            Files.deleteIfExists(path);
        }catch(IOException e) {
            throw new StorageException("존재하지 않는 파일입니다");
        }
    }
    @Override
    public List<String> getLines(String filename) {

        List<String> retData = null;
        Path file = load(filename);
        try {
            Stream<String> lines = Files.lines(file);
            retData = lines.collect(Collectors.toList());
        }catch(Throwable e) {
            throw new StorageException(e.getMessage(), e.getCause());
        }
        return retData;
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(this.rootLocation.toFile());
    }

    public String todaysDir() {
        Calendar cal = Calendar.getInstance();
        String dateString;

        dateString = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        return dateString;
    }
}
