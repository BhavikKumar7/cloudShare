package in.com.cloudshareapi.service;


import in.com.cloudshareapi.document.ProfileDocument;
import in.com.cloudshareapi.dto.ProfileDTO;
import in.com.cloudshareapi.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTests {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateProfile_NewProfile() {
        // given
        ProfileDTO profileDTO = ProfileDTO.builder()
                .clerkId("clerk123")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .photoUrl("http://example.com/photo.jpg")
                .build();

        ProfileDocument savedProfile = ProfileDocument.builder()
                .id("1")
                .clerkId("clerk123")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .photoUrl("http://example.com/photo.jpg")
                .credits(5)
                .createdAt(Instant.now())
                .build();

        when(profileRepository.existsByClerkId("clerk123")).thenReturn(false);
        when(profileRepository.save(any(ProfileDocument.class))).thenReturn(savedProfile);

        // when
        ProfileDTO result = profileService.createProfile(profileDTO);

        // then
        assertNotNull(result);
        assertEquals("clerk123", result.getClerkId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(5, result.getCredits());
        verify(profileRepository, times(1)).save(any(ProfileDocument.class));
    }

    @Test
    void testUpdateProfile_ExistingProfile() {
        ProfileDocument existingProfile = ProfileDocument.builder()
                .id("1")
                .clerkId("clerk123")
                .email("old@example.com")
                .firstName("Old")
                .lastName("Name")
                .photoUrl("old.jpg")
                .credits(10)
                .createdAt(Instant.now())
                .build();

        ProfileDTO profileDTO = ProfileDTO.builder()
                .clerkId("clerk123")
                .email("new@example.com")
                .firstName("New")
                .lastName("Name")
                .photoUrl("new.jpg")
                .build();

        when(profileRepository.findByClerkId("clerk123")).thenReturn(existingProfile);
        when(profileRepository.save(any(ProfileDocument.class))).thenReturn(existingProfile);

        // when
        ProfileDTO result = profileService.updateProfile(profileDTO);

        // then
        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertEquals("new.jpg", result.getPhotoUrl());
        verify(profileRepository, times(1)).save(existingProfile);
    }

    @Test
    void testExistsByClerkId() {
        when(profileRepository.existsByClerkId("clerk123")).thenReturn(true);

        boolean exists = profileService.existsByClerkId("clerk123");

        assertTrue(exists);
        verify(profileRepository, times(1)).existsByClerkId("clerk123");
    }

    @Test
    void testDeleteProfile() {
        ProfileDocument existingProfile = ProfileDocument.builder()
                .clerkId("clerk123")
                .build();

        when(profileRepository.findByClerkId("clerk123")).thenReturn(existingProfile);

        profileService.deleteProfile("clerk123");

        verify(profileRepository, times(1)).delete(existingProfile);
    }

    @Test
    void testGetCurrentProfile_WhenAuthenticated() {
        // given
        String clerkId = "clerk123";
        ProfileDocument profile = ProfileDocument.builder()
                .id("1")
                .clerkId(clerkId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .credits(5)
                .createdAt(Instant.now())
                .build();

        // mock authentication
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(clerkId, null, null);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // mock repository
        when(profileRepository.findByClerkId(clerkId)).thenReturn(profile);

        // when
        ProfileDocument result = profileService.getCurrentProfile();

        // then
        assertNotNull(result);
        assertEquals(clerkId, result.getClerkId());
        assertEquals("test@example.com", result.getEmail());
        verify(profileRepository, times(1)).findByClerkId(clerkId);
    }

    @Test
    void testGetCurrentProfile_WhenNotAuthenticated() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // when / then
        assertThrows(UsernameNotFoundException.class, () -> profileService.getCurrentProfile());
    }
}
