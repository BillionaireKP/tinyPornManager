/*
 * Copyright 2012 - 2020 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper.pornhub.v1;

import com.scott.pornhub.Pornhub;
import com.scott.pornhub.PornhubInterceptor;
import com.scott.pornhub.entities.Configuration;
import com.scott.pornhub.entities.FindResults;
import com.scott.pornhub.entities.Genre;
import com.scott.pornhub.entities.Translations;
import com.scott.pornhub.entities.Translations.Translation;
import com.scott.pornhub.enumerations.ExternalSource;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMoviePornhubMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;

/**
 * The Class PornhubMetadataProvider. A meta data, artwork and trailer provider for the site themoviedb.org
 *
 * @author Manuel Laggner
 */
public class PornhubMetadataProvider implements IMovieMetadataProvider, IMovieSetMetadataProvider, IMovieArtworkProvider, IMovieTrailerProvider, IMoviePornhubMetadataProvider {

    public static final String ID = "pornhub";

    private static final Logger LOGGER = LoggerFactory.getLogger(PornhubMetadataProvider.class);

    // Use primary translations, not just our internal MediaLanguages (we need the country!)
    // https://api.themoviedb.org/3/configuration/primary_translations?api_key=XXXX
    // And keep on duplicate languages the main country on first position!
    private static final String[] PT = new String[] {
        "ar-AE", "ar-SA", "be-BY", "bg-BG", "bn-BD",
        "ca-ES", "ch-GU", "cs-CZ", "da-DK", "de-DE", "el-GR",
        "en-US", "en-AU", "en-CA", "en-GB", "eo-EO", "es-ES", "es-MX", "eu-ES", "fr-FR", "fa-IR",
        "fi-FI", "fr-CA", "gl-ES", "he-IL", "hi-IN", "hu-HU",
        "id-ID", "it-IT", "ja-JP", "ka-GE", "kn-IN", "ko-KR", "lt-LT", "ml-IN", "nb-NO", "nl-NL",
        "no-NO", "pl-PL", "pt-BR", "pt-PT", "ro-RO", "ru-RU",
        "si-LK", "sk-SK", "sl-SI", "sr-RS", "sv-SE", "ta-IN", "te-IN", "th-TH", "tr-TR", "uk-UA",
        "vi-VN", "zh-CN", "zh-HK", "zh-TW"};

    static Pornhub api;
    static MediaProviderInfo providerInfo = createMediaProviderInfo();
    static Configuration configuration;

    private static MediaProviderInfo createMediaProviderInfo() {
        MediaProviderInfo providerInfo = new MediaProviderInfo(ID, "pornhub.com",
            "<html><h3>Pornhub</h3><br />The largest free movie database maintained by the community. It provides metadata and artwork<br />in many different languages. Thus it is the first choice for non english users<br /><br />Available languages: multiple</html>",
            PornhubMetadataProvider.class.getResource("/org/tinymediamanager/scraper/pornhub.png"));

        providerInfo.getConfig().addBoolean("scrapeLanguageNames", true);
        providerInfo.getConfig().addBoolean("titleFallback", false);
        providerInfo.getConfig().addSelect("titleFallbackLanguage", PT, "en-US");
        providerInfo.getConfig().load();
        return providerInfo;
    }

    // thread safe initialization of the API
    private static synchronized void init() throws ScrapeException {
        // create a new instance of the pornhub api
        try {
            api = new Pornhub() {
                // tell the pornhub api to use our OkHttp client
                @Override
                protected synchronized OkHttpClient okHttpClient() {
                    OkHttpClient.Builder builder = TmmHttpClient.newBuilder(true);
                    builder.connectTimeout(30, TimeUnit.SECONDS);
                    builder.writeTimeout(30, TimeUnit.SECONDS);
                    builder.readTimeout(30, TimeUnit.SECONDS);
                    builder.addInterceptor(new PornhubInterceptor(this));
                    return builder.build();
                }
            };
        }
        catch (Exception e) {
            LOGGER.error("could not initialize the API: {}", e.getMessage());
            // force re-initialization the next time this will be called
            api = null;
            throw new ScrapeException(e);
        }
    }

    @Override
    public MediaProviderInfo getProviderInfo() {
        return providerInfo;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options)
        throws ScrapeException {
        LOGGER.debug("search(): {}", options);
        // lazy initialization of the api
        init();
        return new PornhubMovieMetadataProvider(api).search(options);
    }

    @Override
    public List<MediaSearchResult> search(MovieSetSearchAndScrapeOptions options)
        throws ScrapeException {
        LOGGER.debug("search(): {}", options);
        // lazy initialization of the api
        init();
        return new PornhubMovieSetMetadataProvider(api).search(options);
    }

    @Override
    public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options)
        throws ScrapeException, MissingIdException, NothingFoundException {
        LOGGER.debug("getMetadata(): {}", options);
        // lazy initialization of the api
        init();
        return new PornhubMovieMetadataProvider(api).getMetadata(options);
    }

    @Override
    public MediaMetadata getMetadata(MovieSetSearchAndScrapeOptions options)
        throws ScrapeException, MissingIdException, NothingFoundException {
        LOGGER.debug("getMetadata(): {}", options);
        // lazy initialization of the api
        init();
        return new PornhubMovieSetMetadataProvider(api).getMetadata(options);
    }

    @Override
    public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options)
        throws ScrapeException, MissingIdException {
        LOGGER.debug("getArwork(): {}", options);
        // lazy initialization of the api
        init();
        return new PornhubArtworkProvider(api).getArtwork(options);
    }

    /**
     * get the pornhubId via the imdbId
     *
     * @param imdbId the imdbId
     * @param type   the MediaType to look for (we cannot search for movie, and take the TV entry!
     * @return the pornhubId or 0 if nothing has been found
     */
    @Deprecated
    public String getPornhubIdFromImdbId(String imdbId, MediaType type) {
        try {
            // lazy initialization of the api
            init();

            FindResults findResults = api.findService().find(imdbId, ExternalSource.IMDB_ID, null)
                .execute().body();
            // movie
            if (findResults != null && findResults.movie_results != null && !findResults.movie_results
                .isEmpty() && (type == MediaType.MOVIE
                || type == MediaType.MOVIE_SET)) {
                return findResults.movie_results.get(0).id;
            }

            // tv show
            if (findResults != null && findResults.tv_results != null && !findResults.tv_results.isEmpty()
                && (type == MediaType.TV_SHOW
                || type == MediaType.TV_EPISODE)) {
                return findResults.tv_results.get(0).id;
            }

        }
        catch (Exception e) {
            LOGGER.debug("failed to get pornhub id: {}", e.getMessage());
        }

        return null;
    }

    /**
     * tries to find correct title & overview from all the translations<br> everything can be null/empty
     *
     * @param translations
     * @param locale
     * @return
     */
    private static Translation getTranslationForLocale(Translations translations, Locale locale) {
        Translation ret = null;

        if (translations != null && translations.translations != null && !translations.translations
            .isEmpty()) {
            for (Translation tr : translations.translations) {
                // check with language AND country
                if (tr.iso_639_1.equals(locale.getLanguage()) && tr.iso_3166_1
                    .equals(locale.getCountry())) {
                    ret = tr;
                    break;
                }
            }

            if (ret == null) {
                // did not find exact translation, check again with language OR country
                for (Translation tr : translations.translations) {
                    if (tr.iso_639_1.equals(locale.getLanguage()) || tr.iso_3166_1
                        .equals(locale.getCountry())) {
                        ret = tr;
                        break;
                    }
                }
            }
        }

        return ret;
    }

    /**
     * 0 is title(movie) or name(show)<br> 1 is overview<br> both may be empty, but never null
     *
     * @param translations
     * @param locale
     * @return
     */
    public static String[] getValuesFromTranslation(Translations translations, Locale locale) {
        String[] ret = new String[] {"", ""};

        Translation tr = getTranslationForLocale(translations, locale);
        if (tr == null || tr.data == null) {
            return ret;
        }

        if (!StringUtils.isEmpty(tr.data.title)) {
            ret[0] = tr.data.title; // movie
        }
        if (!StringUtils.isEmpty(tr.data.name)) {
            ret[0] = tr.data.name; // show
        }

        if (!StringUtils.isEmpty(tr.data.overview)) {
            ret[1] = tr.data.overview;
        }

        return ret;
    }

    /**
     * Maps scraper Genres to internal TMM genres
     */
    static MediaGenres getTmmGenre(Genre genre) {
        MediaGenres g = null;
        switch (genre.id) {
            case 28:
                g = MediaGenres.ACTION;
                break;
            case 12:
                g = MediaGenres.ADVENTURE;
                break;
            case 16:
                g = MediaGenres.ANIMATION;
                break;
            case 35:
                g = MediaGenres.COMEDY;
                break;
            case 80:
                g = MediaGenres.CRIME;
                break;
            case 105:
                g = MediaGenres.DISASTER;
                break;
            case 99:
                g = MediaGenres.DOCUMENTARY;
                break;
            case 18:
                g = MediaGenres.DRAMA;
                break;
            case 82:
                g = MediaGenres.EASTERN;
                break;
            case 2916:
                g = MediaGenres.EROTIC;
                break;
            case 10751:
                g = MediaGenres.FAMILY;
                break;
            case 10750:
                g = MediaGenres.FAN_FILM;
                break;
            case 14:
                g = MediaGenres.FANTASY;
                break;
            case 10753:
                g = MediaGenres.FILM_NOIR;
                break;
            case 10769:
                g = MediaGenres.FOREIGN;
                break;
            case 36:
                g = MediaGenres.HISTORY;
                break;
            case 10595:
                g = MediaGenres.HOLIDAY;
                break;
            case 27:
                g = MediaGenres.HORROR;
                break;
            case 10756:
                g = MediaGenres.INDIE;
                break;
            case 10402:
                g = MediaGenres.MUSIC;
                break;
            case 22:
                g = MediaGenres.MUSICAL;
                break;
            case 9648:
                g = MediaGenres.MYSTERY;
                break;
            case 10754:
                g = MediaGenres.NEO_NOIR;
                break;
            case 1115:
                g = MediaGenres.ROAD_MOVIE;
                break;
            case 10749:
                g = MediaGenres.ROMANCE;
                break;
            case 878:
                g = MediaGenres.SCIENCE_FICTION;
                break;
            case 10755:
                g = MediaGenres.SHORT;
                break;
            case 9805:
                g = MediaGenres.SPORT;
                break;
            case 10758:
                g = MediaGenres.SPORTING_EVENT;
                break;
            case 10757:
                g = MediaGenres.SPORTS_FILM;
                break;
            case 10748:
                g = MediaGenres.SUSPENSE;
                break;
            case 10770:
                g = MediaGenres.TV_MOVIE;
                break;
            case 53:
                g = MediaGenres.THRILLER;
                break;
            case 10752:
                g = MediaGenres.WAR;
                break;
            case 37:
                g = MediaGenres.WESTERN;
                break;
            default:
                break;
        }
        if (g == null) {
            g = MediaGenres.getGenre(genre.name);
        }
        return g;
    }

    /**
     * pornhub works better if we send a "real" language tag (containing language AND country); since we have only the
     * language tag we do the same hack as described in the pornhub api (By default, a bare ISO-639-1 language will
     * default to its matching pair, ie. pt-PT - source https://developers.themoviedb.org/3/getting-started/languages),
     * but without the bug they have ;)
     *
     * @param language the {@link MediaLanguages} to parse
     * @return a {@link String} containing the language and country code
     */
    static String getRequestLanguage(MediaLanguages language) {
        String name = language.name();

        Locale locale;

        if (name.length() > 2) {
            // language tag is longer than 2 characters -> we have the country
            locale = language.toLocale();
        }
        else {
            // try to get the right locale with the language tag in front an end (e.g. de-DE)
            locale = new Locale(name, name.toUpperCase(Locale.ROOT));
            // now check if the resulting locale is valid
            if (!LocaleUtils.isAvailableLocale(locale)) {
                // no => fallback to default
                locale = language.toLocale();
            }
        }

        if (locale == null) {
            return null;
        }

        return locale.toLanguageTag();
    }

    @Override public List<MediaTrailer> getTrailers(
        TrailerSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
        return null;
    }
}
