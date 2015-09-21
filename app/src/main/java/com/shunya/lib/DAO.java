package com.shunya.lib;

import android.content.ContentValues;

import java.util.List;

/**
 * Created by nixit on 8/18/14.
 */
public interface DAO<T> {
    public void close();
    public void insert(T t);
    public T select(long id);
    public List<T> select (String query);
    public boolean update(ContentValues cv, long id);
    public boolean update(T t, long id);
    public boolean delete(long id);
}
