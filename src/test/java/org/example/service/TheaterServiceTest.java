package org.example.service;

import org.example.model.*;
import org.example.repository.TheaterRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TheaterServiceTest {

    @Mock
    private TheaterRepository theaterRepository;

    private TheaterService theaterService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        theaterService = new TheaterService(theaterRepository);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTheaterWithNullName() throws SQLException {
        theaterService.createTheater(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTheaterWithEmptyName() throws SQLException {
        theaterService.createTheater("");
    }

    @Test
    public void testCreateTheaterSuccess() throws SQLException {
        when(theaterRepository.createTheater("Test Theater")).thenReturn(1L);

        Long theaterId = theaterService.createTheater("Test Theater");

        assertEquals(Long.valueOf(1L), theaterId);
        verify(theaterRepository).createTheater("Test Theater");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTheaterLayoutWithInvalidParameters() throws SQLException {
        theaterService.createTheaterLayout("Theater", 0, 10, 10);
    }

    @Test
    public void testCreateTheaterLayout() throws SQLException {
        when(theaterRepository.createTheater("Test Theater")).thenReturn(1L);
        doNothing().when(theaterRepository).bulkInsertTheaterLayout(any(Theater.class));

        Theater theater = theaterService.createTheaterLayout("Test Theater", 2, 3, 10);

        assertNotNull(theater);
        assertEquals("Test Theater", theater.getName());
        assertEquals(2, theater.getSections().size());
        assertEquals(3, theater.getSections().get(0).getRows().size());
        assertEquals(10, theater.getSections().get(0).getRows().get(0).getSeats().size());
        assertEquals(60, theater.getTotalSeats()); // 2 sections * 3 rows * 10 seats
    }

    @Test
    public void testGetAllTheaters() throws SQLException {
        List<Theater> expectedTheaters = Arrays.asList(
                new Theater(1L, "Theater 1"),
                new Theater(2L, "Theater 2")
        );

        when(theaterRepository.findAllTheaters()).thenReturn(expectedTheaters);

        List<Theater> theaters = theaterService.getAllTheaters();

        assertEquals(2, theaters.size());
        assertEquals("Theater 1", theaters.get(0).getName());
        assertEquals("Theater 2", theaters.get(1).getName());
    }

    @Test
    public void testBookSeat() throws SQLException {
        when(theaterRepository.bookSeat(1L)).thenReturn(true);

        boolean result = theaterService.bookSeat(1L);

        assertTrue(result);
        verify(theaterRepository).bookSeat(1L);
    }

    @Test
    public void testCancelBooking() throws SQLException {
        when(theaterRepository.cancelBooking(1L)).thenReturn(true);

        boolean result = theaterService.cancelBooking(1L);

        assertTrue(result);
        verify(theaterRepository).cancelBooking(1L);
    }

    @Test
    public void testFindSeat() {
        // Create test theater structure
        Theater theater = new Theater(1L, "Test Theater");
        Section section = new Section(1L, "Orchestra");
        Row row = new Row(1L, 1);
        Seat seat = new Seat(1L, 5, SeatStatus.AVAILABLE);

        row.addSeat(seat);
        section.addRow(row);
        theater.addSection(section);

        // Test finding the seat
        Seat foundSeat = theaterService.findSeat(theater, "Orchestra", 1, 5);

        assertNotNull(foundSeat);
        assertEquals(5, foundSeat.getNumber());
        assertEquals(SeatStatus.AVAILABLE, foundSeat.getStatus());
    }

    @Test
    public void testFindSeatNotFound() {
        Theater theater = new Theater(1L, "Test Theater");

        Seat foundSeat = theaterService.findSeat(theater, "Orchestra", 1, 5);

        assertNull(foundSeat);
    }
}
