package com.prime.prime_app.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    /**
     * Initialize the file storage service
     */
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(uploadDir + "/profile-images"));
            Files.createDirectories(Paths.get(uploadDir + "/reports"));
            log.info("Created file upload directories");
        } catch (IOException e) {
            log.error("Could not initialize file storage", e);
            throw new RuntimeException("Could not initialize file storage", e);
        }
    }
    
    /**
     * Store a profile image file and return the path
     * 
     * @param file The uploaded file
     * @return The path to the stored file
     */
    public String storeProfileImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file");
        }
        
        try {
            // Generate a unique filename to prevent collisions
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + fileExtension;
            
            // Create the full path
            Path destinationDir = Paths.get(uploadDir + "/profile-images");
            Path destinationFile = destinationDir.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            
            // Ensure the file is within the target directory
            if (!destinationFile.getParent().equals(destinationDir.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory");
            }
            
            // Copy the file to the destination
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Return the relative path
            return "/profile-images/" + filename;
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new RuntimeException("Failed to store file", e);
        }
    }
    
    /**
     * Delete a profile image
     * 
     * @param filename The filename to delete
     * @return true if deletion was successful
     */
    public boolean deleteProfileImage(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        try {
            // Extract just the filename from the path
            String justFilename = filename;
            if (filename.contains("/")) {
                justFilename = filename.substring(filename.lastIndexOf("/") + 1);
            }
            
            Path file = Paths.get(uploadDir + "/profile-images/" + justFilename);
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Failed to delete file", e);
            return false;
        }
    }
    
    /**
     * Store a PDF report from a ByteArrayInputStream
     * 
     * @param inputStream The PDF content as a ByteArrayInputStream
     * @param filename The filename to use
     * @return The path to the stored file
     */
    public String storePdfReport(ByteArrayInputStream inputStream, String filename) {
        try {
            // Create the reports directory if it doesn't exist
            Path reportsDir = Paths.get(uploadDir + "/reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }
            
            // Create the full path
            Path destinationFile = reportsDir.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            
            // Ensure the file is within the target directory
            if (!destinationFile.getParent().equals(reportsDir.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory");
            }
            
            // Copy the input stream to the destination file
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Return the relative path
            return "/reports/" + filename;
        } catch (IOException e) {
            log.error("Failed to store PDF report", e);
            throw new RuntimeException("Failed to store PDF report", e);
        }
    }
} 