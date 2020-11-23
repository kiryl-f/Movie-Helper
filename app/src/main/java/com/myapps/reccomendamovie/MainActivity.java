package com.myapps.reccomendamovie;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;
import com.myapps.reccomendamovie.databinding.ActivityMainBinding;
import com.myapps.reccomendamovie.databinding.MovieAttributesDialogBinding;
import com.wenchao.cardstack.CardStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import adil.dev.lib.materialnumberpicker.dialog.NumberPickerDialog;
import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    ArrayList <Movie> movies = new ArrayList<>();
    ArrayList <String> moviesToWatchTitles = new ArrayList<>();

    Specifications specifications = new Specifications();
    boolean moviesReady = false;

    HashMap <String, String> languageMap;

    BottomSheetFragment bottomSheetFragment;

    boolean rlVisible = true;

    ArrayDeque <Movie> swipedMovies = new ArrayDeque<>();

    HashMap <String, Integer> genreMap;

    InterstitialAd interstitialAd;
    long adCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setChosenTheme();

        setGenreMap();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        runNtwActivity();
        binding.swipeStack.setContentResource(R.layout.movie_card);

        if(!isNetworkConnected()) {
            showNoConnectionMessage();
        }

        if(getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("language", "en").equals("en")) {
            setTitle("Movie helper");
        } else {
            setTitle("Поиск фильмов");
        }

        languageMap = new HashMap<String, String>() {
            {
                put(getString(R.string.usa), "en");
                put(getString(R.string.russia), "ru");
                put(getString(R.string.spain), "es");
                put(getString(R.string.france), "fr");
                put(getString(R.string.germany), "de");
                put(getString(R.string.italy), "it");
                put(getString(R.string.south_korea), "ko");
            }
        };

        setMoviesToWatch();

        bottomSheetFragment = new BottomSheetFragment();

        binding.bottomNav.setSelectedItemId(R.id.filterItem);

        binding.bottomNav.setOnNavigationItemSelectedListener(item -> {
            if(item.getItemId() == R.id.nextToWatchItem) {
                startActivity(new Intent(MainActivity.this, NextToWatchActivity.class).putExtra("mode", "to_watch").putExtra("text", getString(R.string.no_movie_to_watch)));
            } else if(item.getItemId() == R.id.moviesItem) {
                finish();
            } else if(item.getItemId() == R.id.filterItem) {
                if(moviesReady) {
                    showBottomSheet();
                }
            } else if(item.getItemId() == R.id.watchedItem) {
                startActivity(new Intent(MainActivity.this, NextToWatchActivity.class).putExtra("mode", "watched").putExtra("text", getString(R.string.no_movie_watched)));
            } else {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            return false;
        });

        binding.filmText.setOnClickListener(v -> showBottomSheet());

        binding.filmImage.setOnClickListener(v -> showBottomSheet());

        binding.thumbUpImageView.setOnClickListener(v -> {
            if(moviesReady && binding.filmImage.getVisibility() != View.VISIBLE) {
                binding.swipeStack.discardTop(1);
            }
        });

        binding.thumbDownImageView.setOnClickListener(v -> {
            if(moviesReady && binding.filmImage.getVisibility() != View.VISIBLE) {
                binding.swipeStack.discardTop(0);
            }
        });

        binding.eyeImageView.setOnClickListener(v -> {
            if(moviesReady && binding.filmImage.getVisibility() != View.VISIBLE) {
                binding.swipeStack.discardTop(2);
            }
        });


        binding.undoImageView.setOnClickListener(v -> {
            if(moviesReady) {
                binding.filmText.setVisibility(View.GONE);
                binding.filmImage.setVisibility(View.GONE);
                undo();
            }
        });

        binding.arrowImageView.setOnClickListener(v -> binding.RL.animate().translationY(rlVisible?binding.RL.getHeight()*2:0)
                .alpha(binding.RL.getVisibility()==View.VISIBLE?1f:1f)
                .setDuration(250).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                binding.arrowImageView.animate().rotationBy(180f).setDuration(300);
                binding.arrowImageView.setClickable(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                rlVisible = !rlVisible;
                binding.arrowImageView.setClickable(true);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }));

        AsyncTask.execute(this::iniAds);
    }

    private void iniAds() {
        MobileAds.initialize(this, initializationStatus -> {
            loadAd();
            //findMovie(3);
        });
    }

    private void loadAd() {
        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId("ca-app-pub-6532809968895987/4979212985");
        interstitialAd.loadAd(new AdRequest.Builder().build());
    }

    private void setGenreMap() {
        genreMap = new HashMap<String, Integer>() {
            {
                put(getString(R.string.action), 28);
                put(getString(R.string.adventure), 12);
                put(getString(R.string.animation), 16);
                put(getString(R.string.comedy), 35);
                put(getString(R.string.crime), 80);
                put(getString(R.string.documentary), 99);
                put(getString(R.string.drama), 18);
                put(getString(R.string.family), 10751);
                put(getString(R.string.fantasy), 14);
                put(getString(R.string.history), 36);
                put(getString(R.string.horror), 27);
                put(getString(R.string.music), 10402);
                put(getString(R.string.mystery), 9648);
                put(getString(R.string.romance), 10749);
                put(getString(R.string.sci_fi), 878);
                put(getString(R.string.thriller), 53);
                put(getString(R.string.war), 10752);
                put(getString(R.string.western), 37);
            }
        };
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setChosenTheme() {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme", false)) {
            setTheme(R.style.DarkTheme);
            Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(getDrawable(R.drawable.toolbar_bg));
        } else {
            setTheme(R.style.LightTheme);
        }
    }

    private void runNtwActivity() {
        Intent intent = getIntent();
        if(intent.getBooleanExtra("start_ntw", false)) {
            startActivity(new Intent(this, NextToWatchActivity.class));
        }
    }

    private void loadMovies() {
        String s = getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("language", Locale.getDefault().getLanguage()).equals("ru")?"ru":"en";
        String path = "films_" +  s;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(path);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) {
                    Movie movie = dataSnapshot.getValue(Movie.class);
                    if(movie != null && movie.getRating() != 0.0 && movie.getTitle().length() > 0 && Collections.frequency(moviesToWatchTitles, movie.getTitle()) == 0) {
                        movies.add(movie);
                    }
                }
                binding.progressBar.setVisibility(View.INVISIBLE);
                binding.swipeStack.setVisibility(View.VISIBLE);
                binding.filmText.setVisibility(View.VISIBLE);
                binding.filmImage.setVisibility(View.VISIBLE);
                moviesReady = true;

                findMovie(3);

                //iniAds();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void undo() {
        binding.swipeStack.undo();
        if(swipedMovies.size() > 0) {
            removeLastMovieFromFirebase();
            swipedMovies.removeLast();
        }
        if(swipedMovies.size() == 0) {
            binding.undoImageView.setImageResource(R.drawable.ic_baseline_undo_transparent_24);
        } else {
            binding.undoImageView.setImageResource(R.drawable.ic_baseline_undo_24);
        }
    }

    private void removeLastMovieFromFirebase() {
        DatabaseReference reference =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("Users")
                        .child(this.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                                .getString("name", "")).child("to_watch").child(swipedMovies.getLast().getTitle() + " ");
        reference.removeValue();
        reference = FirebaseDatabase.getInstance()
                        .getReference()
                        .child("Users")
                        .child(this.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                                .getString("name", "")).child("watched").child(swipedMovies.getLast().getTitle());
        reference.removeValue();
    }

    private void checkFirstRun() {
        SharedPreferences preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if(!preferences.getBoolean("guide_shown", false)) {
            showGuide();
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("guide_shown", true);
        editor.apply();
    }

    private void showGuide() {
        GuideView.Builder filterGuideView =
                new GuideView.Builder(this).setTargetView(binding.filterView).setTitle(getString(R.string.set_spec)).setDismissType(DismissType.outside);
        GuideView.Builder nextToWatchGuideView
                = new GuideView.Builder(this).setTargetView(binding.nextToWatchView).setTitle(getString(R.string.list_liked)).setDismissType(DismissType.outside);
        GuideView.Builder descriptionGuideView
                = new GuideView.Builder(this).setTargetView(binding.descriptionView).setTitle(getString(R.string.click_to_see_desc)).setDismissType(DismissType.outside);

        GuideView.Builder swipeGuideView
                = new GuideView.Builder(this).setTargetView(binding.descriptionView).setTitle(getString(R.string.swide_desc)).setDismissType(DismissType.outside);
        filterGuideView.setGuideListener(view -> nextToWatchGuideView.build().show());
        nextToWatchGuideView.setGuideListener(view -> descriptionGuideView.build().show());
        descriptionGuideView.setGuideListener(view -> swipeGuideView.build().show());
        filterGuideView.build().show();
    }

    private void filmWatched(Movie movie) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(getName()).child("watched").child(movie.getTitle());
        reference.setValue(movie);
    }

    private void setMoviesToWatch() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(getName()).child("to_watch");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) {
                    moviesToWatchTitles.add(Objects.requireNonNull(dataSnapshot.getValue(Movie.class)).getTitle().trim());
                }
                setWatchedMovies();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setWatchedMovies() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(getName()).child("watched");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) {
                    moviesToWatchTitles.add(Objects.requireNonNull(dataSnapshot.getValue(Movie.class)).getTitle().trim());
                }
                loadMovies();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showPlot(Movie movie) {
        new AlertDialog.Builder(this).setMessage(movie.getPlot()).setTitle(movie.getTitle())
                .setPositiveButton(getString(R.string.will_watch), (dialog, which) -> binding.swipeStack.discardTop(3)).setNegativeButton(getString(R.string.skip), (dialog, which)
                -> binding.swipeStack.discardTop(0)).setNeutralButton(getString(R.string.watched), (d,w) -> binding.swipeStack.discardTop(2)).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.settingsItem) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNoConnectionMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(getString(R.string.no_connection))
                .setPositiveButton("Ok", (dialog, which) -> finish());
        builder.show();
    }

    private void showBottomSheet() {
        bottomSheetFragment.show(getSupportFragmentManager(), "title");
    }

    private void setSwipeListener() {
        binding.swipeStack.setListener(new CardStack.CardEventListener() {
            @Override
            public boolean swipeEnd(int section, float distance) {
                resetTextViews();
                return distance > 100;
            }

            @Override
            public boolean swipeStart(int section, float distance) {
                return true;
            }

            @Override
            public boolean swipeContinue(int section, float distanceX, float distanceY) {
                if(section == 1 || section == 3) {
                    binding.willWatchTextView.setVisibility(View.VISIBLE);
                    binding.alreadyWatchedTextView.setVisibility(View.INVISIBLE);
                    binding.skipTextView.setVisibility(View.INVISIBLE);
                    binding.willWatchTextView.setAlpha(distanceX/100);
                } else if(section == 2) {
                    binding.willWatchTextView.setVisibility(View.INVISIBLE);
                    binding.alreadyWatchedTextView.setVisibility(View.VISIBLE);
                    binding.skipTextView.setVisibility(View.INVISIBLE);
                    binding.alreadyWatchedTextView.setAlpha(distanceX/100);
                } else {
                    binding.willWatchTextView.setVisibility(View.INVISIBLE);
                    binding.alreadyWatchedTextView.setVisibility(View.INVISIBLE);
                    binding.skipTextView.setVisibility(View.VISIBLE);
                    binding.skipTextView.setAlpha(distanceX/100);
                }
                return true;
            }

            @Override
            public void discarded(int mIndex, int direction) {
                if(mIndex == binding.swipeStack.getAdapter().getCount()) {
                    binding.filmText.setVisibility(View.VISIBLE);
                    binding.filmImage.setVisibility(View.VISIBLE);
                } else {
                    Movie movie = (Movie) Objects.requireNonNull(binding.swipeStack.getAdapter().getItem(binding.swipeStack.getCurrIndex() - 1));
                    movie.setDate(System.currentTimeMillis());
                    if(direction == 1 || direction == 3) {
                        saveFilmToFirebase(movie);
                    } else if(direction == 2) {

                        filmWatched(movie);
                    }

                    swipedMovies.addLast(movie);
                    if(swipedMovies.size() > 0) {
                        binding.undoImageView.setImageResource(R.drawable.ic_baseline_undo_24);
                        binding.undoImageView.setClickable(true);
                        binding.undoImageView.setEnabled(true);
                    }

                    adCount++;
                    if(adCount % 15 == 0) {
                        interstitialAd.show();
                        loadAd();
                    }
                    resetTextViews();
                }
            }

            @Override
            public void topCardTapped() {
                showPlot((Movie) Objects.requireNonNull(binding.swipeStack.getAdapter().getItem(binding.swipeStack.getCurrIndex())));
            }
        });
    }

    private void resetTextViews() {
        binding.willWatchTextView.setVisibility(View.INVISIBLE);
        binding.alreadyWatchedTextView.setVisibility(View.INVISIBLE);
        binding.skipTextView.setVisibility(View.INVISIBLE);
        binding.willWatchTextView.setAlpha(1F);
        binding.alreadyWatchedTextView.setAlpha(1F);
        binding.skipTextView.setAlpha(1F);
    }

    private void saveFilmToFirebase(final Movie movie) {
        movie.setTitle(getRefactoredString(movie.getTitle()));
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(getName()).child("to_watch").child(getRefactoredString(movie.getTitle()));
        reference.setValue(movie);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return Objects.requireNonNull(cm).getActiveNetworkInfo() != null && Objects.requireNonNull(cm.getActiveNetworkInfo()).isConnected();
    }

    private String getRefactoredString(String s) {
        return s.replaceAll("#", " ").replaceAll("$", " ");
    }

    private String getName() {
        SharedPreferences preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        return preferences.getString("name", "");
    }

    private void setSwipe(ArrayList <Movie> movies) {
        MovieCardAdapter adapter = new MovieCardAdapter(this, 0);

        for(Movie movie:movies) {
            adapter.add(movie);
        }
        adapter.setMovies(movies);
        binding.swipeStack.setAdapter(adapter);

        checkFirstRun();
    }

    public void findMovie(int sortMode) {
        ArrayList <Movie> correctMovies = new ArrayList<>();

        Movie movie = new Movie();
        movie.setGenre1(specifications.genre);
        movie.setLanguage(specifications.language);
        movie.setYear(specifications.minYear);

        for(Movie curMovie:movies) {
            if(movie.equalToMovie(curMovie)) {
                correctMovies.add(curMovie);
            }
        }
        if(sortMode < 1) {
            Collections.shuffle(correctMovies);
        } else if(sortMode == 1) {
            Collections.sort(correctMovies, (o1, o2) -> {
                if(o1.getRating() < o2.getRating()) {
                    return 1;
                } else if(o1.getRating() > o2.getRating()) {
                    return -1;
                }
                return 0;
            });
        } else if(sortMode == 2){
            Collections.sort(correctMovies, (o1, o2) -> o1.getTitle().compareTo(o2.getTitle()));
        } else {
            Collections.shuffle(correctMovies);

            int c = 0;
            for(int i = 0;i < correctMovies.size();i++) {
                if(correctMovies.get(i).getRating() >= 6.5) {
                    correctMovies.add(0, correctMovies.get(i));
                    correctMovies.remove(i+1);
                    c++;
                    if(c == 10) {
                        break;
                    }
                }
            }
        }
        if(correctMovies.size() == 0) {
            binding.filmImage.setVisibility(View.VISIBLE);
            binding.filmText.setVisibility(View.VISIBLE);
            binding.swipeStack.setVisibility(View.GONE);
            binding.filmText.setText(R.string.no_movies_found);
        } else {
            binding.filmImage.setVisibility(View.GONE);
            binding.filmText.setVisibility(View.GONE);
            binding.swipeStack.setVisibility(View.VISIBLE);
            if(binding.swipeStack.getAdapter() != null) {
                binding.swipeStack.getAdapter().clear();
            }
            setSwipe(correctMovies);
            setSwipeListener();
        }
    }

    @Override
    protected void onDestroy() {
        //loadMoviesTask.cancel(true);
        super.onDestroy();
    }

    public static class Specifications {
        ArrayList <String> genres;

        int minYear, maxYear, genre;
        String country, language;

        @NonNull
        @Override
        public String toString() {
            return "" + country + '/' + language + " " + "; " + minYear + ',' + maxYear + "; " + genres;
        }

        Specifications() {
            genres = new ArrayList<>();
            minYear = 0;
            maxYear = 0;
            language = " ";
            genre = 0;
            country = " ";
        }
    }

    public static class BottomSheetFragment extends SuperBottomSheetFragment {

        MovieAttributesDialogBinding binding;
        MainActivity activity;
        HashMap <String, Integer> genreMap;

        @Override
        public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            activity = (MainActivity) getActivity();
        }

        @Nullable
        @Override

        public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            binding = MovieAttributesDialogBinding.inflate(inflater, container, false);

            setMovieAttributesChoice();

            return binding.getRoot();
        }

        private void setMovieAttributesChoice() {
            activity.specifications = new Specifications();

            genreMap = new HashMap<String, Integer>() {
                {
                    put(getString(R.string.action), 28);
                    put(getString(R.string.adventure), 12);
                    put(getString(R.string.animation), 16);
                    put(getString(R.string.comedy), 35);
                    put(getString(R.string.crime), 80);
                    put(getString(R.string.documentary), 99);
                    put(getString(R.string.drama), 18);
                    put(getString(R.string.family), 10751);
                    put(getString(R.string.fantasy), 14);
                    put(getString(R.string.history), 36);
                    put(getString(R.string.horror), 27);
                    put(getString(R.string.music), 10402);
                    put(getString(R.string.mystery), 9648);
                    put(getString(R.string.romance), 10749);
                    put(getString(R.string.sci_fi), 878);
                    put(getString(R.string.thriller), 53);
                    put(getString(R.string.war), 10752);
                    put(getString(R.string.western), 37);
                }
            };

            ArrayList <String> countries
                    = new ArrayList<>(Arrays.asList(getString(R.string.any_country),getString(R.string.usa),
                    getString(R.string.russia), getString(R.string.france)
                    ,getString(R.string.italy), getString(R.string.spain), getString(R.string.germany), getString(R.string.south_korea)));

            binding.countrySpinner.setSpinnerTitle(getString(R.string.country));
            binding.countrySpinner.setSpinnerList(countries);
            binding.countrySpinner.setSelected(0);
            binding.countrySpinner.addOnItemChoosenListener((s, i) -> {
                if(i != -1) {
                    if(!s.equals(getString(R.string.any_country))) {
                        activity.specifications.country = s;
                        activity.specifications.language = activity.languageMap.get(s);
                    } else {
                        activity.specifications.country = " ";
                        activity.specifications.language = " ";
                    }
                } else {
                    activity.specifications.country = " ";
                    activity.specifications.language = " ";
                }
            });

            ArrayList <String> genres =
                    new ArrayList<>(Arrays.asList(getString(R.string.any_genre),getString(R.string.drama), getString(R.string.history), getString(R.string.adventure)
                            , getString(R.string.thriller), getString(R.string.comedy), getString(R.string.action), getString(R.string.horror), getString(R.string.music), getString(R.string.war), getString(R.string.western), getString(R.string.sci_fi), getString(R.string.animation)));

            binding.genreSpinner.setSpinnerTitle(getString(R.string.genre));
            binding.genreSpinner.setSpinnerList(genres);
            binding.genreSpinner.setSelected(0);
            binding.genreSpinner.addOnItemChoosenListener((s, i) -> {
                if(i != -1) {
                    if(!s.equals(getString(R.string.any_genre))) {
                        activity.specifications.genre = genreMap.get(s);
                    } else {
                        activity.specifications.genre = 0;
                    }
                } else {
                    activity.specifications.genre = 0;
                }
            });

            ArrayList <String> sortBy = new ArrayList<>(Arrays.asList(getString(R.string.random_order),getString(R.string.rating_sort_1), getString(R.string.alph_sort_1)));

            binding.minYearButton.setOnClickListener(v -> {
                NumberPickerDialog dialog = new NumberPickerDialog(getContext(), 1950, 2019, value -> {
                    activity.specifications.minYear = value;
                    binding.minYearButton.setText("" + value);
                });
                dialog.show();
            });

            binding.sortSpinner.setSpinnerList(sortBy);
            binding.sortSpinner.setSelected(0);
            binding.sortSpinner.setSpinnerTitle(getString(R.string.sort));

            binding.confirmButton.setOnClickListener(v -> {
                activity.swipedMovies.clear();
                activity.binding.undoImageView.setEnabled(false);
                activity.binding.undoImageView.setClickable(false);
                activity.binding.undoImageView.setImageResource(R.drawable.ic_baseline_undo_transparent_24);
                MainActivity activity = (MainActivity)getActivity();

                Objects.requireNonNull(activity).findMovie(binding.sortSpinner.getSelected());
                this.dismiss();
            });

            binding.getRandomButton.setOnClickListener(v -> {
                activity.specifications.country = " ";
                activity.specifications.genre = 0;
                activity.specifications.language = " ";
                activity.specifications.minYear = 0;

                activity.swipedMovies.clear();
                activity.binding.undoImageView.setEnabled(false);
                activity.binding.undoImageView.setClickable(false);
                activity.binding.undoImageView.setImageResource(R.drawable.ic_baseline_undo_transparent_24);
                MainActivity activity = (MainActivity)getActivity();
                Objects.requireNonNull(activity).findMovie(-1);
                this.dismiss();
            });
        }

    }

}