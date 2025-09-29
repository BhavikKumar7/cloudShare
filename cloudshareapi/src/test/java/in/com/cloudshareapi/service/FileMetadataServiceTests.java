package in.com.cloudshareapi.service;

import in.com.cloudshareapi.document.FileMetadataDocument;
import in.com.cloudshareapi.document.ProfileDocument;
import in.com.cloudshareapi.dto.FileMetadataDTO;
import in.com.cloudshareapi.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileMetadataServiceTests {

    @Mock
    private ProfileService profileService;

    @Mock
    private UserCreditsService userCreditsService;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @InjectMocks
    private FileMetadataService fileMetadataService;

    private ProfileDocument profile;

    @BeforeEach
    void setUp() {
        profile = ProfileDocument.builder()
                .clerkId("clerk123")
                .build();

        // Lenient stub to avoid unnecessary stubbing warnings
        lenient().when(profileService.getCurrentProfile()).thenReturn(profile);
    }

    @Test
    void uploadFiles_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
        );

        when(userCreditsService.hasEnoughCredits(1)).thenReturn(true);
        when(fileMetadataRepository.save(any(FileMetadataDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<FileMetadataDTO> result = fileMetadataService.uploadFiles(new MockMultipartFile[]{file});

        assertEquals(1, result.size());
        assertEquals("test.txt", result.get(0).getName());

        verify(userCreditsService, times(1)).consumeCredit();
        verify(fileMetadataRepository, times(1)).save(any(FileMetadataDocument.class));
    }

    @Test
    void uploadFiles_noCredits_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
        );

        when(userCreditsService.hasEnoughCredits(1)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fileMetadataService.uploadFiles(new MockMultipartFile[]{file})
        );

        assertEquals("Not enough credits to upload files. Please purchase more credits", ex.getMessage());
    }

    @Test
    void getFiles_returnsUserFiles() {
        FileMetadataDocument doc = FileMetadataDocument.builder()
                .id("file123")
                .clerkId(profile.getClerkId())
                .name("file.txt")
                .build();

        when(fileMetadataRepository.findByClerkId(profile.getClerkId()))
                .thenReturn(List.of(doc));

        List<FileMetadataDTO> files = fileMetadataService.getFiles();

        assertEquals(1, files.size());
        assertEquals("file.txt", files.get(0).getName());
    }

    @Test
    void togglePublic_changesVisibility() {
        FileMetadataDocument doc = FileMetadataDocument.builder()
                .id("file123")
                .clerkId(profile.getClerkId())
                .isPublic(false)
                .build();

        when(fileMetadataRepository.findById("file123")).thenReturn(Optional.of(doc));
        when(fileMetadataRepository.save(any(FileMetadataDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FileMetadataDTO result = fileMetadataService.togglePublic("file123");

        assertTrue(result.getIsPublic());
    }

    @TempDir
    Path tempDir;

    @Test
    void deleteFile_success() throws Exception {
        // Create a temporary file to simulate an uploaded file
        File tempFile = tempDir.resolve("test.txt").toFile();
        assertTrue(tempFile.createNewFile(), "Temp file should exist");

        FileMetadataDocument doc = FileMetadataDocument.builder()
                .id("file123")
                .clerkId(profile.getClerkId())
                .fileLocation(tempFile.getAbsolutePath())
                .build();

        when(fileMetadataRepository.findById("file123")).thenReturn(Optional.of(doc));

        fileMetadataService.deleteFile("file123");

        assertFalse(tempFile.exists(), "File should be deleted from disk");
        verify(fileMetadataRepository, times(1)).deleteById("file123");
    }

    @Test
    void deleteFile_wrongUser_throws() {
        FileMetadataDocument doc = FileMetadataDocument.builder()
                .id("file123")
                .clerkId("otherUser")
                .fileLocation("/path/to/file.txt")
                .build();

        when(fileMetadataRepository.findById("file123")).thenReturn(Optional.of(doc));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fileMetadataService.deleteFile("file123")
        );

        assertEquals("File is not belong to current user", ex.getMessage());
        verify(fileMetadataRepository, never()).deleteById(any());
    }

    @Test
    void deleteFile_notFound_throws() {
        when(fileMetadataRepository.findById("file123")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                fileMetadataService.deleteFile("file123")
        );

        assertEquals("File not found", ex.getMessage());
    }
}