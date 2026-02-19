package dev.donhempsmyer.huntcozy.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.locations.LocationsRepository;
import dev.donhempsmyer.huntcozy.data.remote.FirestoreClosetRepository;
import dev.donhempsmyer.huntcozy.data.remote.FirestoreLoadoutRepository;
import dev.donhempsmyer.huntcozy.data.remote.FirestorePackingStateRepository;
import dev.donhempsmyer.huntcozy.data.remote.UserFirestore;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepository;
import dev.donhempsmyer.huntcozy.data.repository.ClosetRepositoryProvider;
import dev.donhempsmyer.huntcozy.data.seed.FirebaseSeeder;
import dev.donhempsmyer.huntcozy.ui.auth.AuthActivity;
import dev.donhempsmyer.huntcozy.ui.closet.ClosetFragment;
import dev.donhempsmyer.huntcozy.ui.conditions.ConditionsFragment;
import dev.donhempsmyer.huntcozy.ui.home.HomeFragment;
import dev.donhempsmyer.huntcozy.ui.locations.LocationsFragment;
import dev.donhempsmyer.huntcozy.ui.packing.PackingListFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth auth;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: started");
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        // If no signed-in user, kick back to AuthActivity
        if (user == null) {
            Log.w(TAG, "onCreate: no signed-in user; redirecting to AuthActivity");
            navigateToAuthAndFinish();
            return;
        }

        // Seed Firestore structure for this user on entry (idempotent)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        UserFirestore userFs = new UserFirestore(db, user.getUid());

        FirebaseSeeder.seedStructureForUser(db, user.getUid());

        // Closet
        ClosetRepository closetRepo = new FirestoreClosetRepository(userFs);
        ClosetRepositoryProvider.init(closetRepo);

        // Locations
        LocationsRepository.init(userFs);

        //Loadouts
        FirestoreLoadoutRepository.init(userFs);

        //packing state
        FirestorePackingStateRepository.init(userFs);

        setupToolbar();
        setupBottomNav();

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: initial fragment = HomeFragment via bottom nav");
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    // ------------------------------------------------------------------------
    // Toolbar & sign-out
    // ------------------------------------------------------------------------

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.main_toolbar);
        if (toolbar == null) {
            Log.w(TAG, "setupToolbar: main_toolbar not found in layout");
            return;
        }

        // We are *not* using the classic ActionBar menu callbacks,
        // the menu is attached directly in XML with app:menu="@menu/menu_main"
        // and handled here.
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_sign_out) {
                Log.d(TAG, "setupToolbar: Sign out menu clicked");
                handleSignOut();
                return true;
            }
            return false;
        });
    }

    private void handleSignOut() {
        Log.d(TAG, "handleSignOut: signing out user");
        auth.signOut();
        navigateToAuthAndFinish();
    }

    private void navigateToAuthAndFinish() {
        Intent intent = new Intent(this, AuthActivity.class);
        // Clear back stack so user can't hit Back into MainActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ------------------------------------------------------------------------
    // Bottom navigation
    // ------------------------------------------------------------------------

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) {
            Log.e(TAG, "setupBottomNav: bottom_nav not found in layout");
            return;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "bottomNav item selected id=" + itemId);
            switchTo(itemId);
            return true;
        });
    }

    /**
     * Replace the root fragment for the selected bottom-nav item.
     * This is only called from the BottomNavigationView listener
     * and from the helper methods (openHomeTab, etc.).
     */
    private void switchTo(int itemId) {
        Fragment fragment;

        if (itemId == R.id.nav_home) {
            fragment = HomeFragment.newInstance();
        } else if (itemId == R.id.nav_locations) {
            fragment = LocationsFragment.newInstance();
        } else if (itemId == R.id.nav_conditions) {
            fragment = ConditionsFragment.newInstance();
        } else if (itemId == R.id.nav_closet) {
            fragment = ClosetFragment.newInstance();
        } else if (itemId == R.id.nav_packing_list) {
            fragment = PackingListFragment.newInstance();
        } else {
            Log.w(TAG, "switchTo: unknown menu id, defaulting to Home");
            fragment = HomeFragment.newInstance();
        }

        Log.d(TAG, "switchTo: replacing fragment with " + fragment.getClass().getSimpleName());

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    // ------------------------------------------------------------------------
    // Helpers for fragments to request a tab switch
    // ------------------------------------------------------------------------

    public void openHomeTab() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    public void openLocationsTab() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_locations);
        }
    }

    public void openConditionsTab() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_conditions);
        }
    }

    public void openClosetTab() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_closet);
        }
    }

    public void openPackingTab() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_packing_list);
        }
    }

    // Alternate approach:
    // - Use Navigation Component with NavHostFragment and setupWithNavController(bottomNav)
    // - This keeps back stack per destination automatically, but requires nav_graph.xml.
}