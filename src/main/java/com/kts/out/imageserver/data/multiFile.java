package com.kts.out.imageserver.data;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class multiFile implements MultipartFile {

    private String filename;
    private final byte[] imageContent;

    public multiFile(String filename, byte[] imageContent) {
        this.filename = filename;
        this.imageContent = imageContent;
    }

    @Override
    public String getName() {
        return this.filename;
    }

    @Override
    public String getOriginalFilename() {
        return this.filename;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return imageContent == null || imageContent.length == 0;
    }

    @Override
    public long getSize() {
        return imageContent.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return imageContent;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(imageContent);
    }

    @Override
    public void transferTo(File file) throws IOException, IllegalStateException {
        new FileOutputStream(file).write(imageContent);
    }
}
