package com.servacode.directory.feature.ratings

object RatingValidator {
    fun isValid(stars: Int): Boolean = stars in 1..5

    fun requireValid(stars: Int): Int {
        require(isValid(stars)) { "Rating must be between 1 and 5." }
        return stars
    }
}
