package com.scott.pornhub.services;

import com.scott.pornhub.entities.AccountStatesResults;
import com.scott.pornhub.entities.AppendToResponse;
import com.scott.pornhub.entities.Changes;
import com.scott.pornhub.entities.Credits;
import com.scott.pornhub.entities.Images;
import com.scott.pornhub.entities.PornhubDate;
import com.scott.pornhub.entities.TvSeason;
import com.scott.pornhub.entities.TvSeasonExternalIds;
import com.scott.pornhub.entities.Videos;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface TvSeasonsService {

    /**
     * Get the primary information about a TV season by its season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           <em>Optional.</em> ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}")
    Call<TvSeason> season(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Query("language") String language
    );

    /**
     * Get the primary information about a TV season by its season number.
     *  @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           <em>Optional.</em> ISO 639-1 code.
     * @param appendToResponse   <em>Optional.</em> extra requests to append to the result.
     */
    @GET("tv/{tv_id}/season/{season_number}")
    Call<TvSeason> season(
            @Path("tv_id") String tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Query("language") String language,
            @Query("append_to_response") AppendToResponse appendToResponse
    );

    /**
     * Get the primary information about a TV season by its season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           <em>Optional.</em> ISO 639-1 code.
     * @param appendToResponse   <em>Optional.</em> extra requests to append to the result.
     * @param options            <em>Optional.</em> parameters for the appended extra results.
     */
    @GET("tv/{tv_id}/season/{season_number}")
    Call<TvSeason> season(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Query("language") String language,
            @Query("append_to_response") AppendToResponse appendToResponse,
            @QueryMap Map<String, String> options
    );

    /**
     * Grab the following account states for a session:
     *
     * Returns all of the user ratings for the season's episodes.
     *
     * <b>Requires an active Session.</b>
     *
     * @param pornhubId             A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/account_states")
    Call<AccountStatesResults> accountStates(
            @Path("tv_id") int pornhubId,
            @Path("season_number") int tvShowSeasonNumber
    );

    /**
     * Get the changes for a TV show. By default only the last 24 hours are returned.
     *
     * Get the changes for a TV season. By default only the last 24 hours are returned.
     *
     * You can query up to 14 days in a single query by using the start_date and end_date query parameters.
     *
     * @param tvShowSeasonId A Tv Show TvSeason TMDb id.
     * @param start_date     <em>Optional.</em> Starting date of changes occurred to a movie.
     * @param end_date       <em>Optional.</em> Ending date of changes occurred to a movie.
     * @param page           <em>Optional.</em> Minimum value is 1, expected value is an integer.
     */
    @GET("tv/season/{season_id}/changes")
    Call<Changes> changes(
            @Path("season_id") int tvShowSeasonId,
            @Query("start_date") PornhubDate start_date,
            @Query("end_date") PornhubDate end_date,
            @Query("page") Integer page
    );

    /**
     * Get the cast and crew credits for a TV season by season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/credits")
    Call<Credits> credits(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber
    );

    /**
     * Get the external ids that we have stored for a TV season by season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           <em>Optional.</em> ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/external_ids")
    Call<TvSeasonExternalIds> externalIds(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Query("language") String language
    );

    /**
     * Get the images (posters) that we have stored for a TV season by season number.
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           <em>Optional.</em> ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/images")
    Call<Images> images(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Query("language") String language
    );

    /**
     * Get the videos that have been added to a TV season (trailers, teasers, etc...)
     *
     * @param tvShowId           A Tv Show TvSeason TMDb id.
     * @param tvShowSeasonNumber TvSeason Number.
     * @param language           <em>Optional.</em> ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/videos")
    Call<Videos> videos(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Query("language") String language
    );


}
