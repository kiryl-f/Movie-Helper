package com.myapps.reccomendamovie;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Movie implements Comparable<Movie> {

    private String title, posterPath, language, plot;
    private ArrayList <String> genres, countries;
    private int year, genre1, genre2, genre3;
    private double rating;
    private long date;

    public Movie() {}


    @NonNull
    @Override
    public String toString() {
        return title + " " +  language + " " + getGenres();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public ArrayList<String> getGenres() {
        return genres;
    }

    public void setGenres(ArrayList<String> genres) {
        this.genres = genres;
    }

    public ArrayList<String> getCountries() {
        return countries;
    }

    public void setCountries(ArrayList<String> countries) {
        this.countries = countries;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getGenre1() {
        return genre1;
    }

    public void setGenre1(int genre1) {
        this.genre1 = genre1;
    }

    public int getGenre2() {
        return genre2;
    }

    public void setGenre2(int genre2) {
        this.genre2 = genre2;
    }

    public int getGenre3() {
        return genre3;
    }

    public void setGenre3(int genre3) {
        this.genre3 = genre3;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public Movie(String title, String posterPath, String language, String plot, ArrayList<String> genres, ArrayList<String> countries, int year, double rating) {
        this.title = title;
        this.posterPath = posterPath;
        this.language = language;
        this.plot = plot;
        this.genres = genres;
        this.countries = countries;
        this.year = year;
        this.rating = rating;
    }

    public Movie(ArrayList <String> countries, long date, int genre1, int genre2, int genre3, ArrayList <String> genres, String language, String plot, String posterPath, double rating,
                 String title, int year) {
        this.title = title;
        this.posterPath = posterPath;
        this.language = language;
        this.plot = plot;
        this.genres = genres;
        this.countries = countries;
        this.year = year;
        this.genre1 = genre1;
        this.genre2 = genre2;
        this.genre3 = genre3;
        this.rating = rating;
        this.date = date;
    }

    @Override
    public int compareTo(Movie o) {
        return o.getTitle().trim().compareTo(this.getTitle().trim());
    }

    public boolean equalToMovie(Movie movie) {

        if(!this.getLanguage().equals(" ")){
            return movie.getLanguage().equals(this.getLanguage());
        }
        
        if(this.getGenre1() != 0) {
           if(!(movie.getGenre1() == this.getGenre1() || movie.getGenre1() == this.getGenre2() || movie.getGenre1() == this.getGenre3())) {
               return false;
           }
        }

        if(this.getYear() != 0) {
            if(movie.getYear() < this.getYear()) {
                return false;
            }
        }
        return false;
    }
}
