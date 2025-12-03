package dev.donhempsmyer.huntcozy.ui.main;


import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.ui.closet.ClosetFragment;
import dev.donhempsmyer.huntcozy.ui.conditions.ConditionsFragment;
import dev.donhempsmyer.huntcozy.ui.home.HomeFragment;
import dev.donhempsmyer.huntcozy.ui.locations.LocationsFragment;
import dev.donhempsmyer.huntcozy.ui.packing.PackingListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: started");
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Log.d(TAG, "onCreate: bottomNav item selected id=" + item.getItemId());
            switchTo(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: initial fragment = HomeFragment");
            bottomNav.setSelectedItemId(R.id.nav_home);
            switchTo(R.id.nav_home);
        }
    }

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

    // Alternate approach:
    // - Use Navigation Component with NavHostFragment and setupWithNavController(bottomNav)
    // - This keeps back stack per destination automatically, but requires nav_graph.xml.
}