package com.hostelgains.data.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "meals", indices = {@Index("dateIso")})
public class MealEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String dateIso;
    public String mealName;
    public String rawQuery;
    public float protein;
    public float carbs;
    public float fats;
    public int calories;
    public long loggedAt;

    public MealEntity(@NonNull String id, String dateIso, String mealName, String rawQuery,
                      float protein, float carbs, float fats, int calories, long loggedAt) {
        this.id = id;
        this.dateIso = dateIso;
        this.mealName = mealName;
        this.rawQuery = rawQuery;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.calories = calories;
        this.loggedAt = loggedAt;
    }
}
