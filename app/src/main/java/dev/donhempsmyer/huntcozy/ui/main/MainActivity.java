package dev.donhempsmyer.huntcozy.ui.main;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.ui.closet.ClosetFragment;
import dev.donhempsmyer.huntcozy.ui.conditions.ConditionsFragment;
import dev.donhempsmyer.huntcozy.ui.home.HomeFragment;
import dev.donhempsmyer.huntcozy.ui.locations.LocationsFragment;
import dev.donhempsmyer.huntcozy.ui.packing.PackingListFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: started");
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);

        // Centralized tab switching via bottom nav
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "bottomNav item selected id=" + itemId);
            switchTo(itemId);
            return true;
        });

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: initial fragment = HomeFragment");
            // This will BOTH select the tab AND trigger the listener above,
            // which calls switchTo(R.id.nav_home)
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /**
     * Replace the root fragment for the selected bottom-nav item.
     * This is only called from the BottomNavigationView listener.
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

    // ---- Helpers for fragments to request a tab switch ----------------------

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