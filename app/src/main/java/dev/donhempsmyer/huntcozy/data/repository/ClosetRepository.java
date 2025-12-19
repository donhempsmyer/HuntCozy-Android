package dev.donhempsmyer.huntcozy.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import dev.donhempsmyer.huntcozy.data.model.GearItem;

/**
 * Abstraction over wherever the user's closet is stored.
 *
 * Current implementation is intended to be Firestore-backed (remote),
 * but this interface stays stable so the UI doesn't care about source.
 */
public interface ClosetRepository {

    interface LoadClosetCallback {
        void onClosetLoaded(@NonNull List<GearItem> items);
        void onError(@NonNull Throwable t);
    }

    interface SaveClosetCallback {
        void onClosetSaved();
        void onError(@NonNull Throwable t);
    }

    interface SimpleCallback {
        void onComplete();
        void onError(@NonNull Throwable t);
    }

    /**
     * Returns a LiveData that mirrors the current closet items.
     * Implementation is free to back this with Firestore listeners, etc.
     */
    @NonNull
    LiveData<List<GearItem>> getClosetLiveData();

    /** Load the latest closet snapshot from the backing store. */
    void loadCloset(@NonNull LoadClosetCallback callback);

    /**
     * Replace the closet contents in the backing store
     * with exactly the given list.
     */
    void saveCloset(@NonNull List<GearItem> items,
                    @NonNull SaveClosetCallback callback);

    /** Insert or update a single gear item. */
    void addOrUpdateGear(@NonNull GearItem item,
                         @NonNull SimpleCallback callback);

    /** Delete a gear item by its id. */
    void deleteGear(@NonNull String gearId,
                    @NonNull SimpleCallback callback);
}