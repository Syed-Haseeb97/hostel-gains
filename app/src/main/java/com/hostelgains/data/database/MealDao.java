package com.hostelgains.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MealDao {
    @Query("SELECT * FROM meals ORDER BY loggedAt ASC")
    List<MealEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MealEntity meal);

    @Query("DELETE FROM meals WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM meals")
    void clear();
}
