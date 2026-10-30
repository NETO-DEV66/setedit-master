package io.github.netodev66.setedit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.netodev66.setedit.BuildConfig;
import io.github.netodev66.setedit.HistoryActivity;
import io.github.netodev66.setedit.OnboardingActivity;
import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.data.AppPrefs;

/** Appearance, behaviour and about. */
public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.settings_toolbar);
        toolbar.setTitle(R.string.nav_settings);

        MaterialTextView themeValue = view.findViewById(R.id.theme_value);
        themeValue.setText(themeLabel(AppPrefs.getThemeMode(requireContext())));
        view.findViewById(R.id.row_theme).setOnClickListener(v -> showThemeDialog(themeValue));

        MaterialSwitch dynamic = view.findViewById(R.id.switch_dynamic);
        dynamic.setChecked(AppPrefs.isDynamicColor(requireContext()));
        dynamic.setOnCheckedChangeListener((b, checked) ->
                AppPrefs.setDynamicColor(requireContext(), checked));

        MaterialSwitch risky = view.findViewById(R.id.switch_risky);
        risky.setChecked(AppPrefs.isConfirmRisky(requireContext()));
        risky.setOnCheckedChangeListener((b, checked) ->
                AppPrefs.setConfirmRisky(requireContext(), checked));

        MaterialSwitch autoReconnect = view.findViewById(R.id.switch_auto_reconnect);
        autoReconnect.setChecked(AppPrefs.isAutoReconnect(requireContext()));
        autoReconnect.setOnCheckedChangeListener((b, checked) ->
                AppPrefs.setAutoReconnect(requireContext(), checked));

        MaterialSwitch advanced = view.findViewById(R.id.switch_advanced);
        advanced.setChecked(AppPrefs.isShowAdvanced(requireContext()));
        advanced.setOnCheckedChangeListener((b, checked) ->
                AppPrefs.setShowAdvanced(requireContext(), checked));

        MaterialSwitch checkUpdates = view.findViewById(R.id.switch_check_updates);
        checkUpdates.setChecked(AppPrefs.isCheckUpdates(requireContext()));
        checkUpdates.setOnCheckedChangeListener((b, checked) ->
                AppPrefs.setCheckUpdates(requireContext(), checked));

        view.findViewById(R.id.row_history).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoryActivity.class)));

        MaterialSwitch reduceMotion = view.findViewById(R.id.switch_reduce_motion);
        reduceMotion.setChecked(AppPrefs.isReduceMotion(requireContext()));
        reduceMotion.setOnCheckedChangeListener((b, checked) ->
                AppPrefs.setReduceMotion(requireContext(), checked));

        MaterialSwitch autoBackup = view.findViewById(R.id.switch_auto_backup);
        autoBackup.setChecked(AppPrefs.isAutoBackup(requireContext()));
        autoBackup.setOnCheckedChangeListener((b, checked) ->
                AppPrefs.setAutoBackup(requireContext(), checked));

        MaterialTextView version = view.findViewById(R.id.version_value);
        version.setText(getString(R.string.settings_version, BuildConfig.VERSION_NAME));

        view.findViewById(R.id.row_update).setOnClickListener(v ->
                startActivity(io.github.netodev66.setedit.update.UpdateActivity.intent(requireContext())));

        view.findViewById(R.id.btn_onboarding).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), OnboardingActivity.class)));
    }

    private void showThemeDialog(@NonNull MaterialTextView themeValue) {
        List<Integer> themeMap = new ArrayList<>(4);
        themeMap.add(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        themeMap.add(AppCompatDelegate.MODE_NIGHT_NO);
        themeMap.add(AppCompatDelegate.MODE_NIGHT_YES);
        themeMap.add(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
        int current = AppPrefs.getThemeMode(requireContext());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme)
                .setSingleChoiceItems(R.array.theme_options, themeMap.indexOf(current), (dialog, which) -> {
                    int mode = themeMap.get(which);
                    AppPrefs.setThemeMode(requireContext(), mode);
                    AppCompatDelegate.setDefaultNightMode(mode);
                    themeValue.setText(themeLabel(mode));
                    dialog.dismiss();
                })
                .show();
    }

    private CharSequence themeLabel(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return getString(R.string.theme_day);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return getString(R.string.theme_night);
        if (mode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY) return getString(R.string.theme_battery);
        return getString(R.string.theme_system);
    }
}
