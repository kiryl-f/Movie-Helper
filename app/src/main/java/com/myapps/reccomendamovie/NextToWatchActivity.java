package com.myapps.reccomendamovie;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.myapps.reccomendamovie.databinding.ActivityNextToWatchBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class NextToWatchActivity extends AppCompatActivity {

    ActivityNextToWatchBinding binding;
    ArrayList <Movie> movies = new ArrayList<>();

    SearchView searchView;
    private boolean moviesReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme", false)) {
            setTheme(R.style.DarkTheme);
            getSupportActionBar().setBackgroundDrawable(getDrawable(R.drawable.toolbar_bg));
        } else {
            setTheme(R.style.LightTheme);
        }

        binding = ActivityNextToWatchBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        binding.bottomNav.setSelectedItemId(R.id.backItem);
        binding.bottomNav.setOnNavigationItemSelectedListener(item -> {
            if(item.getItemId() == R.id.backItem) {
                finish();
            } else {
                searchView.onActionViewExpanded();
            }
            return false;
        });

        getDataFromFirebase();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        MenuItem item = menu.findItem(R.id.searchItem);
        searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { Toast.makeText(NextToWatchActivity.this, "", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(moviesReady) {
                    setRecView(movies, newText);
                }
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        } else if(item.getItemId() == R.id.settingsItem) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if(item.getItemId() == R.id.sortItem) {
            createSortDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void createSortDialog() {
        RadioGroup group = new RadioGroup(this);
        RadioButton sortByDateButton = new RadioButton(this), sortByAlphButton = new RadioButton(this), sortByRatingButton = new RadioButton(this),
        sortByDateDescendingButton = new RadioButton(this), sortByAlphDescendingButton = new RadioButton(this), sortByRatingDescendingButton = new RadioButton(this),
        sortByYearButton = new RadioButton(this), sortByYearDescendingButton = new RadioButton(this);

        sortByAlphButton.setText(R.string.alph_sort_1);
        sortByDateButton.setText(R.string.date_sort_1);
        sortByRatingButton.setText(R.string.rating_sort_1);
        sortByYearButton.setText(R.string.year_sort_1);
        sortByAlphDescendingButton.setText(R.string.alph_sort_2);
        sortByDateDescendingButton.setText(R.string.date_sort_2);
        sortByRatingDescendingButton.setText(R.string.rating_sort_2);
        sortByYearDescendingButton.setText(R.string.year_sort_2);

        group.addView(sortByAlphButton);
        group.addView(sortByDateButton);
        group.addView(sortByRatingButton);
        group.addView(sortByYearButton);
        group.addView(sortByAlphDescendingButton);
        group.addView(sortByDateDescendingButton);
        group.addView(sortByRatingDescendingButton);
        group.addView(sortByYearDescendingButton);

        new AlertDialog.Builder(this).setView(group).setPositiveButton("Ok", (dialog, which) -> {
            if(sortByAlphButton.isChecked()) {
                sortByAlphabet(true);
            } else if(sortByDateButton.isChecked()){
                sortByDate(true);
            } else if(sortByRatingButton.isChecked()){
                sortByRating(false);
            } else if(sortByAlphDescendingButton.isChecked()) {
                sortByAlphabet(false);
            } else if(sortByDateDescendingButton.isChecked()) {
                sortByDate(false);
            } else if(sortByYearDescendingButton.isChecked()) {
                sortByRating(true);
            } else if(sortByYearButton.isChecked()) {
                sortByYear(false);
            } else {
                sortByYear(true);
            }
        }).show();
    }

    private void sortByDate(boolean reverse) {
        Collections.sort(movies, (o1, o2) -> {
            if(o1.getDate() > o2.getDate()) {
                return 1;
            } else if(o1.getDate() < o2.getDate()) {
                return -1;
            }
            return 0;
        });
        if(reverse) {
            Collections.reverse(movies);
        }
        setRecView(movies, "");
    }

    private void sortByAlphabet(boolean reverse) {
        Collections.sort(movies, (o1, o2) -> o1.getTitle().compareTo(o2.getTitle()));
        setRecView(movies, "");
        if(reverse) {
            Collections.reverse(movies);
        }
    }

    private void sortByRating(boolean reverse) {
        Collections.sort(movies, (o1, o2) -> {
            if(o1.getRating() > o2.getRating()) {
                return 1;
            } else if(o1.getRating() < o2.getRating()) {
                return -1;
            }
            return 0;
        });
        if(reverse) {
            Collections.reverse(movies);
        }
        setRecView(movies, "");
    }

    private void sortByYear(boolean reverse) {
        Collections.sort(movies, (o1, o2) -> {
            if(o1.getYear() > o2.getYear()) {
                return 1;
            } else if(o1.getYear() < o2.getYear()) {
                return -1;
            }
            return 0;
        });
        if(reverse) {
            Collections.reverse(movies);
        }
        setRecView(movies, "");
    }

    private void setRecView(final ArrayList <Movie> movies, String pref) {
        if(movies.size() == 0) {
            binding.movieImage.setVisibility(View.VISIBLE);
            binding.movieText.setVisibility(View.VISIBLE);
        } else {
            ArrayList <Movie> correctMovies = new ArrayList<>();
            if(pref.equals("")) {
                MoviesListViewAdapter adapter = new MoviesListViewAdapter(this, movies, getLayoutInflater());
                binding.movieRecView.setAdapter(adapter);
            } else {
                for (Movie movie:movies) {
                    if(movie.getTitle().toLowerCase().startsWith(pref.toLowerCase())) {
                        correctMovies.add(movie);
                    }
                }
                MoviesListViewAdapter adapter = new MoviesListViewAdapter(this, correctMovies, getLayoutInflater());
                binding.movieRecView.setAdapter(adapter);
            }
            moviesReady = true;
        }
    }

    private void getDataFromFirebase() {

        DatabaseReference reference
                = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("name", "")).child("to_watch");

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) {
                    if(!Objects.equals(dataSnapshot.getKey(), "password") && !Objects.equals(dataSnapshot.getKey(), "watched")) {
                        movies.add(dataSnapshot.getValue(Movie.class));
                    }
                }
                binding.progressBar.setVisibility(View.GONE);
                setRecView(movies, "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}