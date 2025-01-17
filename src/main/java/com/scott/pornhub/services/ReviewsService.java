package com.scott.pornhub.services;

import com.scott.pornhub.entities.Review;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ReviewsService {

    /**
     * Get a review by a Review Id.
     *
     * @param reviewId A TMDb Review id.
     */
    @GET("review/{review_id}")
    Call<Review> getDetails(
            @Path("review_id") String reviewId
    );
}
