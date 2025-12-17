package dev.donhempsmyer.huntcozy.data.seed;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseSeeder ensures the expected Firestore structure exists
 * for a single user, without creating any real gear/locations/loadouts.
 *
 * Expected structure:
 *
 * users/{userId}
 *   - (root fields: createdAt, lastSeenAt, schemaVersion, etc.)
 *   - meta/schema
 *   - settings/prefs
 *   - gear/_meta
 *   - locations/_meta
 *   - loadouts/_meta
 *   - optionalCatalog/_meta
 *   - packingState/current
 *
 * This is safe to call multiple times; it uses merge writes and lightweight docs.
 */
public class FirebaseSeeder {

    private static final String TAG = "FirebaseSeeder";

    // Bump when you make breaking changes to your Firestore data model.
    private static final int SCHEMA_VERSION = 1;

    private final FirebaseFirestore db;
    private final String userId;

    /**
     * Convenience helper: call this with your Firestore instance + current userId.
     */
    public static void seedStructureForUser(@NonNull FirebaseFirestore db,
                                            @NonNull String userId) {
        new FirebaseSeeder(db, userId).seedStructure();
    }

    public FirebaseSeeder(@NonNull FirebaseFirestore db,
                          @NonNull String userId) {
        this.db = db;
        this.userId = userId;
    }

    /**
     * Entry point to seed the logical structure for this user.
     * This does NOT insert any actual gear, locations, or loadouts.
     */
    public void seedStructure() {
        seedUserRoot();
        seedSchemaMeta();          // /users/{userId}/meta/schema
        seedSettingsDoc();         // /users/{userId}/settings/prefs
        seedCollectionMeta("gear");
        seedCollectionMeta("locations");
        seedCollectionMeta("loadouts");
        seedCollectionMeta("optionalCatalog");
        seedPackingStateDoc();     // /users/{userId}/packingState/current
    }

    // --- Root user doc ------------------------------------------------------

    private void seedUserRoot() {
        DocumentReference userDoc = db.collection("users").document(userId);

        Map<String, Object> root = new HashMap<>();
        // NOTE: Firestore doesn't support "set if missing" server-side, so
        // createdAt will be overwritten on repeated seeding. If you want to
        // preserve the very first timestamp forever, you can change this
        // later to a read-then-conditional-write.
        root.put("createdAt", FieldValue.serverTimestamp());
        root.put("lastSeenAt", FieldValue.serverTimestamp());
        root.put("schemaVersion", SCHEMA_VERSION);

        userDoc.set(root, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "seedUserRoot: ensured users/" + userId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "seedUserRoot: failed", e));
    }

    // --- Schema meta doc ----------------------------------------------------

    /**
     * /users/{userId}/meta/schema
     *
     * Used to track which schema version this user's data is on.
     * You can use this later for migrations if you bump SCHEMA_VERSION.
     */
    private void seedSchemaMeta() {
        DocumentReference schemaDoc = db.collection("users")
                .document(userId)
                .collection("meta")
                .document("schema");

        Map<String, Object> schema = new HashMap<>();
        schema.put("schemaVersion", SCHEMA_VERSION);
        schema.put("updatedAt", FieldValue.serverTimestamp());

        schemaDoc.set(schema, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "seedSchemaMeta: ensured meta/schema"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "seedSchemaMeta: failed", e));
    }

    // --- Settings doc (placeholder only) -----------------------------------

    /**
     * /users/{userId}/settings/prefs
     *
     * Holds per-user preferences like default weapon/style and comfort bias.
     * No actual values are forced here; just null / default placeholders.
     */
    private void seedSettingsDoc() {
        DocumentReference settingsDoc = db.collection("users")
                .document(userId)
                .collection("settings")
                .document("prefs");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("defaultWeaponType", null);   // e.g. "RIFLE", "BOW" (filled later by user)
        prefs.put("defaultHuntingStyle", null); // e.g. "TREESTAND"
        prefs.put("comfortBias", 0);            // -1 colder, +1 warmer, 0 neutral
        prefs.put("createdAt", FieldValue.serverTimestamp());

        settingsDoc.set(prefs, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "seedSettingsDoc: ensured settings/prefs"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "seedSettingsDoc: failed", e));
    }

    // --- Generic _meta doc for any collection -------------------------------

    /**
     * Creates a lightweight `_meta` document inside the given collection
     * so it shows up in the console and you have a place to hang
     * collection-level metadata if needed later.
     *
     * Example: /users/{userId}/gear/_meta
     */
    private void seedCollectionMeta(@NonNull String collectionName) {
        DocumentReference metaDoc = db.collection("users")
                .document(userId)
                .collection(collectionName)
                .document("_meta");

        Map<String, Object> meta = new HashMap<>();
        meta.put("_schemaVersion", SCHEMA_VERSION);
        meta.put("_note", "Placeholder _meta doc so collection exists; safe to ignore in app UI.");
        meta.put("createdAt", FieldValue.serverTimestamp());

        metaDoc.set(meta, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "seedCollectionMeta: ensured " + collectionName + "/_meta"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "seedCollectionMeta: failed for " + collectionName, e));
    }

    // --- Packing state doc (optional, a single doc) -------------------------

    /**
     * /users/{userId}/packingState/current
     *
     * Optional snapshot spot for your packing UI state.
     * You can expand this later to mirror your ViewModel if desired.
     */
    private void seedPackingStateDoc() {
        DocumentReference packingDoc = db.collection("users")
                .document(userId)
                .collection("packingState")
                .document("current");

        Map<String, Object> state = new HashMap<>();
        state.put("lastUpdated", FieldValue.serverTimestamp());
        state.put("note", "Optional: current packing state snapshot");

        packingDoc.set(state, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Log.d(TAG, "seedPackingStateDoc: ensured packingState/current"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "seedPackingStateDoc: failed", e));
    }
}
