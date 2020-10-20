package com.myapps.reccomendamovie;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.myapps.reccomendamovie.databinding.ActivityMainBinding;
import com.myapps.reccomendamovie.databinding.MovieAttributesDialogBinding;
import com.sayantan.advancedspinner.SpinnerListener;
import com.wenchao.cardstack.CardStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    String imageBaseUrl = "https://image.tmdb.org/t/p/w780";


    boolean moviesReady = false;
    String url;
    //= "https://api.themoviedb.org/3/movie/top_rated?api_key=33d65e0ed0777308653502b72db75fd0&language=en-US&region=RU&page=";

    HashMap <String, String> languageMap;

    BottomSheetFragment bottomSheetFragment;

    boolean rlVisible = true;
    private LoadMoviesTask loadMoviesTask;

    ArrayDeque <Movie> swipedMovies = new ArrayDeque<>();

    HashMap <String, Integer> genreMap;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setChoosenTheme();

        setGenreMap();

        url = "https://api.themoviedb.org/3/movie/top_rated?api_key=33d65e0ed0777308653502b72db75fd0&language=ru-RU&region=RU&page=";
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        runNtwActivity();
        binding.swipeStack.setContentResource(R.layout.movie_card);

        if(!isNetworkConnected()) {
            showNoConnectionMessage();
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
        setWatchedMovies();


        //loadMoviesTask = new LoadMoviesTask(movies, getApplicationContext());
        //loadMoviesTask.execute();

        loadMovies();

        bottomSheetFragment = new BottomSheetFragment();

        binding.bottomNav.setSelectedItemId(R.id.moviesItem);
        binding.bottomNav.setOnNavigationItemSelectedListener(item -> {
            if(item.getItemId() == R.id.nextToWatchItem) {
                startActivity(new Intent(MainActivity.this, NextToWatchActivity.class));
            } else if(item.getItemId() == R.id.moviesItem) {
                finish();
            } else if(item.getItemId() == R.id.filterItem) {
                if(moviesReady) {
                    showBottomSheet();
                }
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


        checkFirstRun();

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

    }

    private void setChoosenTheme() {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme", false)) {
            setTheme(R.style.DarkTheme);
            getSupportActionBar().setBackgroundDrawable(getDrawable(R.drawable.toolbar_bg));
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
        String s = Locale.getDefault().getLanguage().equals("ru")?"ru":"en";
        String path = "films_" +  s;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(path);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int c = 0;
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) {
                    Movie movie = dataSnapshot.getValue(Movie.class);
                    if(movie != null && movie.getRating() != 0.0 && movie.getTitle().length() > 0) {
                        movies.add(movie);
                    } else {
                        c++;
                    }

                    Log.d("movies", movie.toString());
                }Toast.makeText(MainActivity.this, "" + c, Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.INVISIBLE);
                binding.swipeStack.setVisibility(View.VISIBLE);
                binding.filmText.setVisibility(View.VISIBLE);
                binding.filmImage.setVisibility(View.VISIBLE);
                moviesReady = true;

                //findMovie();
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
                                .getString("name", "")).child(swipedMovies.getLast().getTitle() + " ");
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
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(getName());
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) {
                    if(!Objects.equals(dataSnapshot.getKey(), "password")) {
                        moviesToWatchTitles.add(Objects.requireNonNull(dataSnapshot.getValue(Movie.class)).getTitle());
                    }
                }
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
                    moviesToWatchTitles.add(Objects.requireNonNull(dataSnapshot.getValue(Movie.class)).getTitle());
                    Log.d("to_watch", Objects.requireNonNull(dataSnapshot.getValue(Movie.class)).getTitle());
                }
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
                return distance > 100;
            }

            @Override
            public boolean swipeStart(int section, float distance) {
                return true;
            }

            @Override
            public boolean swipeContinue(int section, float distanceX, float distanceY) {
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
                    Log.d("swipes", movie.toString());
                    swipedMovies.addLast(movie);
                    if(swipedMovies.size() > 0) {
                        binding.undoImageView.setImageResource(R.drawable.ic_baseline_undo_24);
                        binding.undoImageView.setClickable(true);
                        binding.undoImageView.setEnabled(true);
                    }
                }
            }

            @Override
            public void topCardTapped() {
                showPlot((Movie) Objects.requireNonNull(binding.swipeStack.getAdapter().getItem(binding.swipeStack.getCurrIndex())));
            }
        });
    }

    private void saveFilmToFirebase(final Movie movie) {
        movie.setTitle(getRefactoredString(movie.getTitle()));
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(getName()).child(getRefactoredString(movie.getTitle()));
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

    }

    public void findMovie() {
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
        Collections.shuffle(correctMovies);
        if(correctMovies.size() == 0) {
            binding.filmImage.setVisibility(View.VISIBLE);
            binding.filmText.setVisibility(View.VISIBLE);
            binding.swipeStack.setVisibility(View.GONE);
            binding.filmText.setText(R.string.no_movies_found);
            Toast.makeText(this, "Nothing found", Toast.LENGTH_SHORT).show();
        } else {
            binding.filmImage.setVisibility(View.GONE);
            binding.filmText.setVisibility(View.GONE);
            binding.swipeStack.setVisibility(View.VISIBLE);
            Toast.makeText(this, "" + correctMovies, Toast.LENGTH_SHORT).show();
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
        public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            binding = MovieAttributesDialogBinding.inflate(inflater, container, false);

            setMovieAttributesChoise();

            return binding.getRoot();
        }

        private void setMovieAttributesChoise() {
            final boolean[] valuesSet = {false, false};
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
                    = new ArrayList<>(Arrays.asList(getString(R.string.usa),
                    getString(R.string.russia), getString(R.string.france)
                    ,getString(R.string.italy), getString(R.string.spain), getString(R.string.germany), getString(R.string.south_korea)));

            binding.countrySpinner.setSpinnerList(countries);
            binding.countrySpinner.addOnItemChoosenListener((s, i) -> {
                if(i != -1) {
                    activity.specifications.country = s;
                    activity.specifications.language = activity.languageMap.get(s);
                } else {
                    activity.specifications.country = " ";
                    activity.specifications.language = " ";
                }
                valuesSet[0] = true;
            });

            ArrayList <String> genres =
                    new ArrayList<>(Arrays.asList(getString(R.string.drama), getString(R.string.history)
                            , getString(R.string.thriller), getString(R.string.comedy), getString(R.string.action), getString(R.string.horror), getString(R.string.music), getString(R.string.war), getString(R.string.western), getString(R.string.sci_fi)));
            binding.genreSpinner.setSpinnerList(genres);
            binding.genreSpinner.addOnItemChoosenListener(new SpinnerListener() {
                @Override
                public void onItemChoosen(String s, int i) {
                    if(i != -1) {
                        activity.specifications.genre = genreMap.get(s);
                    }
                    valuesSet[1] = true;
                }
            });

            binding.minYearButton.setOnClickListener(v -> {
                NumberPickerDialog dialog = new NumberPickerDialog(getContext(), 1950, 2019, value -> {
                    activity.specifications.minYear = value;
                    binding.minYearButton.setText("" + value);
                });
                dialog.show();
            });

            binding.confirmButton.setOnClickListener(v -> {
                activity.swipedMovies.clear();
                activity.binding.undoImageView.setEnabled(false);
                activity.binding.undoImageView.setClickable(false);
                activity.binding.undoImageView.setImageResource(R.drawable.ic_baseline_undo_transparent_24);
                MainActivity activity = (MainActivity)getActivity();
                Objects.requireNonNull(activity).findMovie();
                this.dismiss();
            });
        }
    }

    @SuppressLint("StaticFieldLeak")
    class LoadMoviesTask extends AsyncTask <Void, Void, Void> {

        private ArrayList<Movie> movies;
        private RequestQueue requestQueue, detailsRequestQueue;

        public LoadMoviesTask(ArrayList<Movie> movies, Context context) {
            this.movies = movies;
            requestQueue = Volley.newRequestQueue(context);
            detailsRequestQueue = Volley.newRequestQueue(context);
        }


        @Override
        protected void onCancelled() {
            onDestroy();
            super.onCancelled();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for(int i = 1; i <= 366; i++) {
                if(isCancelled()) {
                    break;
                }
                final JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url+i, null, response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for(int i1 = 0; i1 < results.length(); i1++) {
                            if(isCancelled()) {
                                break;
                            }
                            setMovieDetails(results.getJSONObject(i1).getInt("id"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {

                });
                requestQueue.add(request);
            }
            return null;
        }

        private void setMovieDetails(int movieId) {
            String url = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=33d65e0ed0777308653502b72db75fd0&language=ru-RU";
            final JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
                try {
                    Movie movie;

                    ArrayList <String> movieGenres = new ArrayList<>(), movieCountries = new ArrayList<>();
                    String movieTitle, language;
                    Integer year;
                    Double rating;

                    JSONArray genres = response.getJSONArray("genres");

                    int genre1 = 0, genre2 = 0, genre3 = 0;

                    for(int i = 0;i < Math.min(genres.length(), 3);i++) {
                        JSONObject genre = genres.getJSONObject(i);
                        if(i == 0) {
                            genre1 = genre.getInt("id");
                        } else if(i == 1){
                            genre2 = genre.getInt("id");
                        } else {
                            genre3 = genre.getInt("id");
                        }

                        movieGenres.add(genre.getString("name"));
                    }



                    JSONArray countries = response.getJSONArray("production_countries");
                    for(int i = 0;i < countries.length();i++) {
                        JSONObject country = countries.getJSONObject(i);
                        movieCountries.add(country.getString("name"));
                    }
                    movieTitle = response.getString("title");
                    year = Integer.parseInt(response.getString("release_date").substring(0,4));
                    rating = response.getDouble("vote_average");
                    language = response.getString("original_language");

                    movie = new Movie(movieTitle,
                            imageBaseUrl + response.getString("poster_path"),
                            language,
                            response.getString("overview"),
                            movieGenres,
                            movieCountries,
                            year,
                            rating
                            );
                    movie.setGenre1(genre1);
                    movie.setGenre2(genre2);
                    movie.setGenre3(genre3);
                    movies.add(movie);
                    if(movies.size() == 2027) {
                        Log.d("loading", "ok");
                        runOnUiThread(() -> {
                            Log.d("movie_tag", "ok");
                            binding.progressBar.setVisibility(View.INVISIBLE);
                            binding.swipeStack.setVisibility(View.VISIBLE);
                            binding.filmText.setVisibility(View.VISIBLE);
                            binding.filmImage.setVisibility(View.VISIBLE);
                            moviesReady = true;
                            DatabaseReference database = FirebaseDatabase.getInstance().getReference().child("films_ru");
                            for(Movie movie1:movies) {
                                Movie movie2;
                                movie2 = movie1;

                                String s = movie1.getTitle();

                                s = s.replace(".", "");
                                s = s.replace("$", "");
                                s = s.replace("#", "");
                                s = s.replace("[", "");
                                s = s.replace("]", "");
                                //title.replace(0, title.length(), ".");
                                //Log.d("fire_base",title.toString() + "   :" + s);
                                movie2.setTitle(s);

                                database.child(movie2.getTitle()).setValue(movie2);
                            }
                            //findMovie();
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
            });

            detailsRequestQueue.add(request);
        }
    }
}