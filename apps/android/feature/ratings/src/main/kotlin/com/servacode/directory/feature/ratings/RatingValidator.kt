package com.servacode.directory.feature.ratings

object RatingValidator {
    fun requireValid(stars: Int): Int {
        require(stars in 1..5) { "Rating must be between 1 and 5." }
        return stars
    }
}
