package in.com.cloudshareapi.service;

import in.com.cloudshareapi.document.ProfileDocument;
import in.com.cloudshareapi.document.UserCredits;
import in.com.cloudshareapi.repository.UserCreditsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserCreditsServiceTests {

    @Mock
    private UserCreditsRepository userCreditsRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private UserCreditsService userCreditsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateInitialCredits() {
        UserCredits newCredits = UserCredits.builder()
                .clerkId("clerk123")
                .credits(5)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.save(any(UserCredits.class))).thenReturn(newCredits);

        UserCredits result = userCreditsService.createInitialCredits("clerk123");

        assertNotNull(result);
        assertEquals("clerk123", result.getClerkId());
        assertEquals(5, result.getCredits());
        assertEquals("BASIC", result.getPlan());
        verify(userCreditsRepository, times(1)).save(any(UserCredits.class));
    }

    @Test
    void testGetUserCredits_WhenExists() {
        UserCredits existing = UserCredits.builder()
                .clerkId("clerk123")
                .credits(10)
                .plan("PREMIUM")
                .build();

        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.of(existing));

        UserCredits result = userCreditsService.getUserCredits("clerk123");

        assertNotNull(result);
        assertEquals(10, result.getCredits());
        assertEquals("PREMIUM", result.getPlan());
    }

    @Test
    void testGetUserCredits_WhenNotExists() {
        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.empty());

        UserCredits newCredits = UserCredits.builder()
                .clerkId("clerk123")
                .credits(5)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.save(any(UserCredits.class))).thenReturn(newCredits);

        UserCredits result = userCreditsService.getUserCredits("clerk123");

        assertNotNull(result);
        assertEquals(5, result.getCredits());
        assertEquals("BASIC", result.getPlan());
    }

    @Test
    void testGetUserCredits_WithoutParam() {
        ProfileDocument profile = ProfileDocument.builder().clerkId("clerk123").build();
        when(profileService.getCurrentProfile()).thenReturn(profile);

        UserCredits credits = UserCredits.builder()
                .clerkId("clerk123")
                .credits(7)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.of(credits));

        UserCredits result = userCreditsService.getUserCredits();

        assertNotNull(result);
        assertEquals("clerk123", result.getClerkId());
        assertEquals(7, result.getCredits());
    }

    @Test
    void testHasEnoughCredits_WhenEnough() {
        ProfileDocument profile = ProfileDocument.builder().clerkId("clerk123").build();
        when(profileService.getCurrentProfile()).thenReturn(profile);

        UserCredits credits = UserCredits.builder()
                .clerkId("clerk123")
                .credits(10)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.of(credits));

        boolean result = userCreditsService.hasEnoughCredits(5);

        assertTrue(result);
    }

    @Test
    void testHasEnoughCredits_WhenNotEnough() {
        ProfileDocument profile = ProfileDocument.builder().clerkId("clerk123").build();
        when(profileService.getCurrentProfile()).thenReturn(profile);

        UserCredits credits = UserCredits.builder()
                .clerkId("clerk123")
                .credits(2)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.of(credits));

        boolean result = userCreditsService.hasEnoughCredits(5);

        assertFalse(result);
    }

    @Test
    void testConsumeCredit_WhenHasCredits() {
        ProfileDocument profile = ProfileDocument.builder().clerkId("clerk123").build();
        when(profileService.getCurrentProfile()).thenReturn(profile);

        UserCredits credits = UserCredits.builder()
                .clerkId("clerk123")
                .credits(3)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.of(credits));
        when(userCreditsRepository.save(any(UserCredits.class))).thenReturn(credits);

        UserCredits result = userCreditsService.consumeCredit();

        assertNotNull(result);
        assertEquals(2, result.getCredits()); // should decrement by 1
        verify(userCreditsRepository, times(1)).save(credits);
    }

    @Test
    void testConsumeCredit_WhenNoCredits() {
        ProfileDocument profile = ProfileDocument.builder().clerkId("clerk123").build();
        when(profileService.getCurrentProfile()).thenReturn(profile);

        UserCredits credits = UserCredits.builder()
                .clerkId("clerk123")
                .credits(0)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.of(credits));

        UserCredits result = userCreditsService.consumeCredit();

        assertNull(result); // should return null if no credits left
        verify(userCreditsRepository, never()).save(any(UserCredits.class));
    }

    @Test
    void testAddCredits_WhenExists() {
        UserCredits existing = UserCredits.builder()
                .clerkId("clerk123")
                .credits(5)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.of(existing));
        when(userCreditsRepository.save(any(UserCredits.class))).thenReturn(existing);

        UserCredits result = userCreditsService.addCredits("clerk123", 10, "PREMIUM");

        assertNotNull(result);
        assertEquals(15, result.getCredits()); // 5 + 10
        assertEquals("PREMIUM", result.getPlan());
    }

    @Test
    void testAddCredits_WhenNotExists() {
        when(userCreditsRepository.findByClerkId("clerk123")).thenReturn(Optional.empty());

        // Mock saving new user with default 5 credits
        UserCredits newCredits = UserCredits.builder()
                .clerkId("clerk123")
                .credits(5)
                .plan("BASIC")
                .build();

        when(userCreditsRepository.save(any(UserCredits.class))).thenReturn(newCredits);

        UserCredits result = userCreditsService.addCredits("clerk123", 500, "PREMIUM");

        assertNotNull(result);
        assertEquals(505, result.getCredits());
        assertEquals("PREMIUM", result.getPlan());
    }

}
