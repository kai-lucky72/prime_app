package com.prime.prime_app.controller;

import com.prime.prime_app.dto.common.MessageResponse;
import com.prime.prime_app.dto.user.UpdatePasswordRequest;
import com.prime.prime_app.dto.user.ProfileImageResponse;
import com.prime.prime_app.entities.User;
import com.prime.prime_app.service.AuthService;
import com.prime.prime_app.service.UserService;
import com.prime.prime_app.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management endpoints")
public class UserProfileController {

    private final AuthService authService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Operation(
        summary = "Update password",
        description = "Set or update the user's password for future logins"
    )
    @PostMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Password update request received for user: {}", currentUser.getEmail());
            
            userService.updatePassword(currentUser, request.getPassword());
            
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Password updated successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error updating password: {}", e.getMessage(), e);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Error updating password. Please try again.")
                    .build());
        }
    }
    
    @Operation(
        summary = "Upload profile picture",
        description = "Upload a new profile picture for the user"
    )
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileImageResponse> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Profile picture upload request received for user: {}", currentUser.getEmail());
            
            // If user already has a profile image, delete it
            if (currentUser.getProfileImageUrl() != null) {
                fileStorageService.deleteProfileImage(currentUser.getProfileImageUrl());
            }
            
            // Store the new file
            String imageUrl = fileStorageService.storeProfileImage(file);
            User updatedUser = userService.updateProfileImage(currentUser, imageUrl);
            
            return ResponseEntity.ok(ProfileImageResponse.builder()
                    .imageUrl(imageUrl)
                    .message("Profile picture uploaded successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error uploading profile picture: {}", e.getMessage(), e);
            return ResponseEntity.ok(ProfileImageResponse.builder()
                    .message("Error uploading profile picture. Please try again.")
                    .build());
        }
    }
    
    @Operation(
        summary = "Remove profile picture",
        description = "Remove the user's profile picture"
    )
    @DeleteMapping("/profile-image")
    public ResponseEntity<MessageResponse> removeProfilePicture() {
        try {
            User currentUser = authService.getCurrentUser();
            log.debug("Profile picture removal request received for user: {}", currentUser.getEmail());
            
            // Delete the file if it exists
            if (currentUser.getProfileImageUrl() != null) {
                fileStorageService.deleteProfileImage(currentUser.getProfileImageUrl());
            }
            
            // Update user record
            userService.removeProfileImage(currentUser);
            
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Profile picture removed successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error removing profile picture: {}", e.getMessage(), e);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Error removing profile picture. Please try again.")
                    .build());
        }
    }
    
    @Operation(
        summary = "Get profile information",
        description = "Get the user's profile information including the profile image URL"
    )
    @GetMapping
    public ResponseEntity<User> getProfile() {
        try {
            User currentUser = authService.getCurrentUser();
            return ResponseEntity.ok(currentUser);
        } catch (Exception e) {
            log.error("Error retrieving profile: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
} 