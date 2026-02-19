package dev.donhempsmyer.huntcozy.data.remote;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserFirestore {

    private final FirebaseFirestore db;
    private final String userId;

    public UserFirestore(@NonNull FirebaseFirestore db, @NonNull String userId) {
        this.db = db;
        this.userId = userId;
    }

    public static UserFirestore fromCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("UserFirestore: no signed-in user");
        }
        return new UserFirestore(FirebaseFirestore.getInstance(), user.getUid());
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public String getUserId() {
        return userId;
    }

    public CollectionReference gearCollection() {
        return db.collection("users")
                .document(userId)
                .collection("gear");
    }

    public CollectionReference locationsCollection() {
        return db.collection("users")
                .document(userId)
                .collection("locations");
    }

    public CollectionReference loadoutsCollection() {
        return db.collection("users")
                .document(userId)
                .collection("loadouts");
    }

    public CollectionReference optionalCatalogCollection() {
        return db.collection("users")
                .document(userId)
                .collection("optionalCatalog");
    }

    public CollectionReference packingStateCollection() {
        return db.collection("users")
                .document(userId)
                .collection("packingState");
    }
}