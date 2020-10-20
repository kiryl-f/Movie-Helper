package com.myapps.reccomendamovie;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ceylonlabs.imageviewpopup.ImagePopup;
import com.dmallcott.dismissibleimageview.DismissibleImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class MoviesListViewAdapter extends BaseAdapter {

    private Context context;
    private ArrayList <Movie> movies;

    private Movie lastMovie = null;

    public MoviesListViewAdapter(Context context, ArrayList<Movie> movies, LayoutInflater inflater) {
        this.context = context;
        this.movies = movies;
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
            deleteMovieFromFirebase(position, holder.layout);
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


        Glide.with(context).load(movie.getPosterPath()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                holder.progressBar.setVisibility(View.GONE);
                //holder.share.setOnClickListener(v1 -> share(new BitmapFactory.decoder, movie.getTitle()));
                return false;
            }
        }).into(holder.poster);


        final ImagePopup imagePopup = new ImagePopup(context);
        imagePopup.setFullScreen(false); // Optional
        imagePopup.setHideCloseIcon(true);  // Optional
        imagePopup.setImageOnClickClose(true);  // Optional

        imagePopup.initiatePopupWithGlide(movie.getPosterPath());

        return v;
    }

    private void share(Bitmap bitmap, String title) {

        Uri bmpUri = null;
        File file = new File("share_image_" + System.currentTimeMillis() + ".png");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Will fail for API >= 24, better to use FileProvider
        bmpUri = Uri.fromFile(file);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, title);
        shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
        shareIntent.setType("image/jpeg");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share images..."));
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
                                .getString("name", "")).child(movies.get(position).getTitle() + " ");
        reference.removeValue();

        movies.remove(position);
        notifyDataSetChanged();
        
        createSnackbar(view);
    }

    private void createSnackbar(View view) {
        Snackbar.make(view, R.string.movie_deleted, Snackbar.LENGTH_LONG).setAction(R.string.undo, v -> undo()).show();
    }

    private void undo() {
        FirebaseDatabase.getInstance().getReference().child("Users")
                .child(context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                        .getString("name", "")).child(lastMovie.getTitle()).setValue(lastMovie);

        movies.add(lastMovie);
        this.notifyDataSetChanged();
    }

    static class ViewHolder {
        DismissibleImageView poster;
        ImageView delete, share;
        TextView title, year, genre, country, rating;
        RelativeLayout layout;
        ProgressBar progressBar;
    }
}
