package com.myapps.reccomendamovie;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.settings_activity);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        SwitchPreference switchPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            findPreference("log_out").setOnPreferenceClickListener(preference -> {
                SharedPreferences preferences = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("name");
                editor.putBoolean("remember", false);
                editor.apply();
                Intent intent = new Intent(getActivity(), EnterNameActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requireActivity().finish();
                return false;
            });

            ListPreference listPreference = findPreference("language");
            listPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                setLocale(newValue.toString().equals("Русский")?"ru":"en");
                Toast.makeText(getActivity(), R.string.changes_accepted, Toast.LENGTH_SHORT).show();
                return false;
            });

            switchPreference = findPreference("theme");
            switchPreference.setOnPreferenceClickListener((preference) -> {
                Toast.makeText(getActivity(), R.string.changes_accepted, Toast.LENGTH_SHORT).show();
                return false;
            });
        }

        private void setLocale(String lang) {
            LocaleHelper.setLocale(getContext(), lang);

            SharedPreferences preferences = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("language", lang);
            editor.apply();
        }
    }
}