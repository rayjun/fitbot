package com.fitness.data

import kotlin.test.*

class ExerciseProviderTest {

    @Test
    fun testExercisesListNotEmpty() {
        assertTrue(ExerciseProvider.exercises.isNotEmpty())
    }

    @Test
    fun testCategoriesList() {
        val categories = ExerciseProvider.categories
        assertTrue(categories.contains("category_all"))
        assertTrue(categories.contains("cat_chest"))
        assertTrue(categories.contains("cat_legs"))
    }

    @Test
    fun testExercisesByCategory() {
        val chestExercises = ExerciseProvider.exercises.filter { it.categoryKey == "cat_chest" }
        assertTrue(chestExercises.any { it.id == "benchpress" })
        assertTrue(chestExercises.any { it.id == "pushup" })
        
        val legExercises = ExerciseProvider.exercises.filter { it.categoryKey == "cat_legs" }
        assertTrue(legExercises.any { it.id == "squat" })
    }

    @Test
    fun testBodyweightExercises() {
        val bodyweight = ExerciseProvider.exercises.filter { it.isBodyweight }
        assertTrue(bodyweight.any { it.id == "pushup" })
        assertTrue(bodyweight.any { it.id == "plank" })
        
        val weighted = ExerciseProvider.exercises.filter { !it.isBodyweight }
        assertTrue(weighted.any { it.id == "benchpress" })
        assertTrue(weighted.any { it.id == "deadlift" })
    }
}
