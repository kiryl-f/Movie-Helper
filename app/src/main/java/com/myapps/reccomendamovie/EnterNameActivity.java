package com.myapps.reccomendamovie;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.myapps.reccomendamovie.databinding.ActivityEnterNameBinding;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class EnterNameActivity extends AppCompatActivity {

    ActivityEnterNameBinding binding;

    boolean loggingIn = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleHelper.setLocale(this, getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("language", Locale.getDefault().getLanguage()));

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = ActivityEnterNameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        checkIfRemembered();

        binding.registerOrLoginButton.setOnClickListener(v -> {
            if(loggingIn) {
                binding.registerOrLoginButton.setText(getString(R.string.log_in));
                binding.confirmNameButton.setText(getString(R.string.register));
                loggingIn = false;
            } else {
                binding.registerOrLoginButton.setText(getString(R.string.register));
                binding.confirmNameButton.setText(getString(R.string.log_in));
                loggingIn = true;
            }
        });

        binding.nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0) {
                    binding.confirmNameButton.setEnabled(true);
                } else {
                    binding.confirmNameButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.confirmNameButton.setOnClickListener(v -> {
            setAllViewsEnabled(false);
            checkEditText();
        });
    }

    private void createShortcuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        ShortcutInfo.Builder mainShortcut
                = new ShortcutInfo.Builder(this, "main").setIcon(Icon.createWithResource(this, R.drawable.ic_baseline_movie_24))
                .setShortLabel(getString(R.string.movies))
                .setIntent(new Intent(EnterNameActivity.this, MainActivity.class).setAction(Intent.ACTION_VIEW));
        ShortcutInfo.Builder nextToWatchShortcut = new ShortcutInfo.Builder(this, "to_watch")
                .setShortLabel(getString(R.string.next_to_watch))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_baseline_local_movies_black))
                .setIntent(new Intent(EnterNameActivity.this, MainActivity.class).setAction(Intent.ACTION_VIEW).putExtra("start_ntw", true));
        Objects.requireNonNull(shortcutManager).setDynamicShortcuts(Arrays.asList(mainShortcut.build(), nextToWatchShortcut.build()));

    }

    private void setAllViewsEnabled(boolean enabled) {
        binding.nameEditText.setEnabled(enabled);
        binding.passwordEditText.setEnabled(enabled);
        binding.rememberNameCheckBox.setEnabled(enabled);
        binding.registerOrLoginButton.setEnabled(enabled);
        binding.confirmNameButton.setEnabled(enabled);
        binding.progressBar.setVisibility(enabled?View.GONE:View.VISIBLE);
    }

    private void checkIfRemembered() {
        if(getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("remember", false)) {
            createShortcuts();
            startMainActivity();
        }
    }

    private void checkEditText() {
        String inputName = binding.nameEditText.getText().toString().trim(), password = binding.passwordEditText.getText().toString().trim();
        boolean $ = inputName.contains("#") || inputName.contains(".") || inputName.contains("$") || inputName.contains("[") || inputName.contains("]");
        if(loggingIn) {
            if($) {
                makeToast(getString(R.string.username_symbols));
            } else {
                nameIsValid(inputName, password);
            }
        } else {
            if($) {
                makeToast(getString(R.string.username_symbols));
            } else {
                if(password.length() > 0) {
                    checkIfUserExists(inputName, password);
                } else {
                    makeToast(getString(R.string.password_length));
                }
            }
        }
    }

    private void makeToast(String text) {
        FancyToast.makeText(this,text, FancyToast.LENGTH_SHORT, FancyToast.INFO, false).show();
        setAllViewsEnabled(true);
    }

    private void register(String name, String password) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users");
        reference.child(name).child("password").setValue(password);

        addNameToSharedPrefs(name);
        setRemembered();
        startMainActivity();
    }

    private void setRemembered() {
        if(binding.rememberNameCheckBox.isChecked()) {
            SharedPreferences preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("remember", true);
            editor.apply();
        }
    }

    private void checkIfUserExists(final String name, final String password) {
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean userExists = false;
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) {
                    if(Objects.equals(dataSnapshot.getKey(), name)) {
                        userExists = true;
                        break;
                    }
                }
                if(userExists) {
                    makeToast(getString(R.string.user_exists));
                } else {
                    register(name, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void startMainActivity() {
        startActivity(new Intent(EnterNameActivity.this, MainActivity.class));
        finish();
    }

    private void addNameToSharedPrefs(String name) {
        SharedPreferences preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("first_run", true);
        editor.putString("name", name);
        editor.apply();
    }

    private void nameIsValid(final String input, final String password) {
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean valid = false;
                String userName = "";
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) {
                    if(Objects.equals(dataSnapshot.getKey(), input)) {
                        valid = true;
                        userName = dataSnapshot.getKey();
                        break;
                    }
                }
                if(valid) {
                    DatabaseReference databaseReference = reference.child(Objects.requireNonNull(userName)).child("password");
                    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(Objects.equals(snapshot.getValue(String.class), password)) {
                                addNameToSharedPrefs(input);
                                setRemembered();
                                startMainActivity();
                            } else {
                                makeToast(getString(R.string.user_or_pw_incorrect));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                } else {
                    makeToast(getString(R.string.user_or_pw_incorrect));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}