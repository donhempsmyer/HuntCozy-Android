package dev.donhempsmyer.huntcozy.data.repository;


/**
 * Simple provider to share a single ClosetRepository instance across ViewModels.
 * v1: wraps InMemoryClosetRepository.
 * future: swap in a FirebaseClosetRepository or RoomClosetRepository here.
 */
public class ClosetRepositoryProvider {

    private static ClosetRepository instance;

    public static synchronized ClosetRepository get() {
        if (instance == null) {
            instance = new InMemoryClosetRepository();
        }
        return instance;
    }

    private ClosetRepositoryProvider() {
        // no-op
    }
}
