package com.myapps.reccomendamovie;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dmallcott.dismissibleimageview.DismissibleImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;

public class MoviesListViewAdapter extends BaseAdapter {

    private Context context;
    private ArrayList <Movie> movies;

    private Movie lastMovie = null;

    private String mode = "";

    public MoviesListViewAdapter(Context context, ArrayList<Movie> movies, LayoutInflater inflater, String mode) {
        this.context = context;
        this.movies = movies;
        this.mode = mode;
    }

    @Override
    public int getCount() {
        return movies.size();
    }

    @Override
    public Object getItem(int position) {
        return movies.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint({"SetTextI18n", "InflateParams"})
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        Movie movie = movies.get(position);
        View v = convertView;
        ViewHolder holder;
        if(v == null) {
            LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = Objects.requireNonNull(layoutInflater).inflate(R.layout.movie_rec_view_item, null);
            holder = new ViewHolder();
            holder.title = v.findViewById(R.id.titleTextView);
            holder.country = v.findViewById(R.id.regionTextView);
            holder.year = v.findViewById(R.id.yearTextView);
            holder.genre = v.findViewById(R.id.genreTextView);
            holder.poster = v.findViewById(R.id.posterImageView);
            holder.rating = v.findViewById(R.id.ratingTextView);
            holder.layout = v.findViewById(R.id.movieInfoRL);
            holder.delete = v.findViewById(R.id.removeImageView);
            holder.share = v.findViewById(R.id.shareImageView);
            holder.progressBar = v.findViewById(R.id.progressBar);
            holder.main = v.findViewById(R.id.main);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }
        holder.title.setText(movie.getTitle());
        holder.year.setText("" + movie.getYear());

        holder.country.setText(movie.getCountries().get(0));
        if(holder.country.getText().toString().trim().equals("United States of America")) {
            holder.country.setText("USA");
        }

        StringBuilder genres = new StringBuilder();
        for(int i = 0;i < Math.min(movie.getGenres().size(), 3);i++) {
            genres.append(Character.toUpperCase(movie.getGenres().get(i).charAt(0)));
            genres.append(movie.getGenres().get(i).substring(1));
            genres.append(", ");
        }
        genres.deleteCharAt(genres.toString().length()-2);
        holder.genre.setText(genres.toString());

        holder.layout.setOnClickListener(v1 -> showPlot(movie.getPlot()));
        holder.delete.setOnClickListener(v12 -> {
            lastMovie = movies.get(position);
            deleteMovieFromFirebase(position, holder.main);
        });
        double rating = movie.getRating();
        if(rating < 5.0) {
            holder.rating.setTextColor(Color.RED);
        } else if(rating >= 5.0 && rating <= 7.0) {
            holder.rating.setTextColor(Color.GRAY);
        } else {
            holder.rating.setTextColor(Color.GREEN);
        }
        holder.rating.setText("" + rating);

        Picasso.get().load(movie.getPosterPath()).into(holder.poster, new Callback() {
            @Override
            public void onSuccess() {
                holder.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {

            }
        });

        return v;
    }

    private void showPlot(String plot) {
        new AlertDialog.Builder(context).setPositiveButton("Ok", (dialog, which) -> {}).setMessage(plot).show();
    }


    public void deleteMovieFromFirebase(int position, View view) {
        DatabaseReference reference =
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("Users")
                        .child(context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                                .getString("name", "")).child(mode);
        reference.child(movies.get(position).getTitle()).removeValue();
        reference.child(movies.get(position).getTitle() + " ").removeValue();

        movies.remove(position);
        notifyDataSetChanged();
        createSnackbar(view);

    }

    private void createSnackbar(View view) {
        Snackbar.make(view, R.string.movie_deleted, Snackbar.LENGTH_LONG).setAction(R.string.undo, v ->  {
            Log.d("adapter", "undo");
            undo(view);
        }).show();
    }

    private void undo(View view) {
        FirebaseDatabase.getInstance().getReference().child("Users")
                .child(context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                        .getString("name", "")).child(mode).child(lastMovie.getTitle()).setValue(lastMovie);

        movies.add(lastMovie);

        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        animation.setDuration(150);
        view.startAnimation(animation);

        this.notifyDataSetChanged();
    }

    static class ViewHolder {
        DismissibleImageView poster;
        ImageView delete, share;
        TextView title, year, genre, country, rating;
        RelativeLayout layout, main;
        ProgressBar progressBar;
    }
}
