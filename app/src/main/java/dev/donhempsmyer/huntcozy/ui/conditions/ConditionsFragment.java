package dev.donhempsmyer.huntcozy.ui.conditions;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dev.donhempsmyer.huntcozy.R;

public class ConditionsFragment extends Fragment {

    private static final String TAG = "ConditionsFragment";

    public static ConditionsFragment newInstance() {
        return new ConditionsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_conditions, container, false);
    }
}