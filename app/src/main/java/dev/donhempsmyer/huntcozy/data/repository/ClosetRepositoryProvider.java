package dev.donhempsmyer.huntcozy.data.repository;

import androidx.annotation.NonNull;

/**
 * Simple singleton-style provider for the app's ClosetRepository.
 *
 * Usage:
 *
 *   // After Firebase sign-in (e.g. in MainActivity.onCreate):
 *   ClosetRepository repo = new FirestoreClosetRepository(db, user.getUid());
 *   ClosetRepositoryProvider.init(repo);
 *
 *   // Anywhere else in the app:
 *   ClosetRepository closetRepo = ClosetRepositoryProvider.get();
 */
public final class ClosetRepositoryProvider {

    private static ClosetRepository instance;

    private ClosetRepositoryProvider() {
        // no-op: prevent instantiation
    }

    /**
     * Initialize the global ClosetRepository instance.
     * Call this exactly once after you know the signed-in user.
     */
    public static synchronized void init(@NonNull ClosetRepository repository) {
        instance = repository;
    }

    /**
     * Get the global ClosetRepository instance.
     * Will throw if init() has not been called yet.
     */
    @NonNull
    public static synchronized ClosetRepository get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "ClosetRepositoryProvider not initialized. " +
                            "Call ClosetRepositoryProvider.init(...) after sign-in."
            );
        }
        return instance;
    }
}
