package com.myapps.reccomendamovie;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;

public class MovieCardAdapter extends ArrayAdapter<Movie> {

    Context context;
    ArrayList <Movie> movies;

    public MovieCardAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        this.context = context;
    }

    public void setMovies(ArrayList<Movie> movies) {
        this.movies = movies;
    }

    @Nullable
    @Override
    public Movie getItem(int position) {
        return movies.get(position);
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable final View convertView, @NonNull ViewGroup parent) {
        RequestOptions requestOptions = new RequestOptions();
        requestOptions = requestOptions.transforms(new RoundedCorners(16));


        Glide.with(context.getApplicationContext()).load(getItem(position).getPosterPath()).apply(requestOptions).addListener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                convertView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                return false;
            }
        }).into(((ImageView)convertView.findViewById(R.id.posterImageView)));

        ((TextView)convertView.findViewById(R.id.titleTextView)).setText(getItem(position).getTitle());
        ((TextView)convertView.findViewById(R.id.yearTextView)).setText("" + getItem(position).getYear());

        String genre = "";
        genre += Character.toUpperCase(getItem(position).getGenres().get(0).charAt(0));
        genre += getItem(position).getGenres().get(0).substring(1);
        ((TextView)convertView.findViewById(R.id.genreTextView)).setText(genre);

        double rating = getItem(position).getRating();
        if(rating < 5.0) {
            ((TextView)convertView.findViewById(R.id.ratingTextView)).setTextColor(Color.RED);
        } else if(rating >= 5.0 && rating <= 7.0) {
            ((TextView)convertView.findViewById(R.id.ratingTextView)).setTextColor(Color.GRAY);
        } else {
            ((TextView)convertView.findViewById(R.id.ratingTextView)).setTextColor(Color.GREEN);
        }
        ((TextView)convertView.findViewById(R.id.ratingTextView)).setText("" + rating);

        return convertView;
    }
}
