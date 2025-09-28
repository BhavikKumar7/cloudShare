package in.com.cloudshareapi.service;

import in.com.cloudshareapi.document.FileMetadataDocument;
import in.com.cloudshareapi.document.ProfileDocument;
import in.com.cloudshareapi.dto.FileMetadataDTO;
import in.com.cloudshareapi.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final FileMetadataRepository fileMetadataRepository;

public List<FileMetadataDTO> uploadFiles(MultipartFile[] files) throws IOException {
    ProfileDocument currentProfile = profileService.getCurrentProfile();
    List<FileMetadataDocument> savedFiles = new ArrayList<>();

    if (files == null || files.length == 0) {
        throw new IllegalArgumentException("No files provided for upload");
    }

    // Check if user has enough credits
    if (!userCreditsService.hasEnoughCredits(files.length)) {
        throw new RuntimeException("Not enough credits to upload files. Please purchase more credits");
    }

    Path uploadPath = Paths.get("upload").toAbsolutePath().normalize();
    Files.createDirectories(uploadPath);

    for (MultipartFile file : files) {
        // 1. Null or empty validation
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("One of the files is empty or invalid");
        }

        // 2. File size validation (server-side enforcement beyond properties)
        long maxFileSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File " + file.getOriginalFilename() + " exceeds maximum allowed size of 10MB");
        }

        // 3. File name and extension validation
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (!StringUtils.hasText(originalFileName)) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (extension == null || extension.trim().isEmpty()) {
            throw new IllegalArgumentException("File must have a valid extension");
        }

        // Optional: restrict allowed extensions
        List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "pdf", "txt", "docx", "xlsx");
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type ." + extension + " is not supported");
        }

        // 4. Safe unique file naming
        String fileName = UUID.randomUUID() + "." + extension;
        Path targetLocation = uploadPath.resolve(fileName);

        // 5. Copy file to storage
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // 6. Save metadata
        FileMetadataDocument fileMetadata = FileMetadataDocument.builder()
                .fileLocation(targetLocation.toString())
                .name(originalFileName)
                .size(file.getSize())
                .type(file.getContentType())
                .clerkId(currentProfile.getClerkId())
                .isPublic(false)
                .uploadedAt(LocalDateTime.now())
                .build();

        userCreditsService.consumeCredit();
        savedFiles.add(fileMetadataRepository.save(fileMetadata));
    }

    return savedFiles.stream().map(this::mapToDTO).collect(Collectors.toList());
}

    private FileMetadataDTO mapToDTO(FileMetadataDocument fileMetadataDocument) {
        return FileMetadataDTO.builder()
                .id(fileMetadataDocument.getId())
                .fileLocation(fileMetadataDocument.getFileLocation())
                .name(fileMetadataDocument.getName())
                .size(fileMetadataDocument.getSize())
                .type(fileMetadataDocument.getType())
                .clerkId(fileMetadataDocument.getClerkId())
                .isPublic(fileMetadataDocument.getIsPublic())
                .uploadedAt(fileMetadataDocument.getUploadedAt())
                .build();
    }

    public List<FileMetadataDTO> getFiles() {
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetadataDocument> files = fileMetadataRepository.findByClerkId(currentProfile.getClerkId());
        return files.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public FileMetadataDTO getPublicFile(String id) {
        Optional<FileMetadataDocument> fileOptional = fileMetadataRepository.findById(id);
        if (fileOptional.isEmpty() || !fileOptional.get().getIsPublic()) {
            throw new RuntimeException("Unable to get the file");
        }

        FileMetadataDocument document = fileOptional.get();
        return mapToDTO(document);
    }

    public FileMetadataDTO getDownloadableFile(String id) {
        FileMetadataDocument file = fileMetadataRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
        return mapToDTO(file);
    }

    public void deleteFile(String id) {
        ProfileDocument currentProfile = profileService.getCurrentProfile();

        FileMetadataDocument file = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getClerkId().equals(currentProfile.getClerkId())) {
            throw new RuntimeException("File is not belong to current user");
        }

        try {
            Path filePath = Paths.get(file.getFileLocation());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error deleting the file", e);
        }

        fileMetadataRepository.deleteById(id);
    }


    public FileMetadataDTO togglePublic(String id) {
        FileMetadataDocument file = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        file.setIsPublic(!file.getIsPublic());
        fileMetadataRepository.save(file);
        return mapToDTO(file);
    }
}
