package com.scott.pornhub.services;

import com.scott.pornhub.entities.AppendToResponse;
import com.scott.pornhub.entities.BaseAccountStates;
import com.scott.pornhub.entities.Changes;
import com.scott.pornhub.entities.Credits;
import com.scott.pornhub.entities.Images;
import com.scott.pornhub.entities.PornhubDate;
import com.scott.pornhub.entities.RatingObject;
import com.scott.pornhub.entities.Status;
import com.scott.pornhub.entities.TvEpisode;
import com.scott.pornhub.entities.TvEpisodeExternalIds;
import com.scott.pornhub.entities.Videos;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface TvEpisodesService {

    /**
     * Get the primary information about a TV episode by combination of a season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param language            <em>Optional.</em> ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    Call<TvEpisode> episode(
            @Path("tv_id") String tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber,
            @Query("language") String language
    );

    /**
     * Get the primary information about a TV episode by combination of a season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param language            <em>Optional.</em> ISO 639-1 code.
     * @param appendToResponse    <em>Optional.</em> extra requests to append to the result.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    Call<TvEpisode> episode(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber,
            @Query("language") String language,
            @Query("append_to_response") AppendToResponse appendToResponse
    );

    /**
     * Get the primary information about a TV episode by combination of a season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param language            <em>Optional.</em> ISO 639-1 code.
     * @param appendToResponse    <em>Optional.</em> extra requests to append to the result.
     * @param options             <em>Optional.</em> parameters for the appended extra results.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    Call<TvEpisode> episode(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber,
            @Query("language") String language,
            @Query("append_to_response") AppendToResponse appendToResponse,
            @QueryMap Map<String, String> options
    );

    /**
     * Get the changes for a TV episode. By default only the last 24 hours are returned.
     *
     * You can query up to 14 days in a single query by using the start_date and end_date query parameters.
     *
     * @param tvShowEpisodeId A Tv Show TvEpisode TMDb id.
     * @param start_date      <em>Optional.</em> Starting date of changes occurred to a movie.
     * @param end_date        <em>Optional.</em> Ending date of changes occurred to a movie.
     * @param page            <em>Optional.</em> Minimum value is 1, expected value is an integer.
     */
    @GET("tv/episode/{episode_id}/changes")
    Call<Changes> changes(
            @Path("episode_id") int tvShowEpisodeId,
            @Query("start_date") PornhubDate start_date,
            @Query("end_date") PornhubDate end_date,
            @Query("page") Integer page
    );

    /**
     * Get the TV episode credits by combination of season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/credits")
    Call<Credits> credits(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber
    );

    /**
     * Get the external ids for a TV episode by combination of a season and episode number.
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/external_ids")
    Call<TvEpisodeExternalIds> externalIds(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber
    );

    /**
     * Get the images (episode stills) for a TV episode by combination of a season and episode number. Since episode
     * stills don't have a language, this call will always return all images.
     *  @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/images")
    Call<Images> images(
            @Path("tv_id") String tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber
    );

    /**
     * Get the videos that have been added to a TV episode (teasers, clips, etc...)
     *
     * @param tvShowId            A Tv Show TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param language            <em>Optional.</em> ISO 639-1 code.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/videos")
    Call<Videos> videos(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber,
            @Query("language") String language
    );

    /**
     * Grab the following account states for a session:
     *
     * * TV Episode rating
     *
     * <b>Requires an active Session.</b>
     *
     * @param tvShowId            TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}/account_states")
    Call<BaseAccountStates> accountStates(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber
    );

    /**
     * Rate a TV show.
     *
     * <b>Requires an active Session.</b>
     *
     * @param tvShowId            TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     * @param body                <em>Required.</em> A ReviewObject Object. Minimum value is 0.5 and Maximum 10.0, expected value is a number.
     */
    @POST("tv/{tv_id}/season/{season_number}/episode/{episode_number}/rating")
    Call<Status> addRating(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber,
            @Body RatingObject body
    );

    /**
     * Remove your rating for a TV show.
     *
     * <b>Requires an active Session.</b>
     *
     * @param tvShowId            TMDb id.
     * @param tvShowSeasonNumber  TvSeason Number.
     * @param tvShowEpisodeNumber TvEpisode Number.
     */
    @DELETE("tv/{tv_id}/season/{season_number}/episode/{episode_number}/rating")
    Call<Status> deleteRating(
            @Path("tv_id") int tvShowId,
            @Path("season_number") int tvShowSeasonNumber,
            @Path("episode_number") int tvShowEpisodeNumber
    );

}
