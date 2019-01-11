/*
 * Copyright 2012 - 2019 Manuel Laggner
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
package org.tinymediamanager.core.movie.tasks;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieHelpers;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieTrailer;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaTrailer;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.exceptions.UnsupportedMediaTypeException;
import org.tinymediamanager.scraper.mediaprovider.IMovieArtworkProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieMetadataProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieTrailerProvider;
import org.tinymediamanager.scraper.trakttv.SyncTraktTvTask;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.movies.dialogs.MovieChooserDialog;

/**
 * The Class MovieScrapeTask.
 * 
 * @author Manuel Laggner
 */
public class MovieScrapeTask extends TmmThreadPool {
  private final static Logger         LOGGER = LoggerFactory.getLogger(MovieScrapeTask.class);
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  private List<Movie>                 moviesToScrape;
  private boolean                     doSearch;
  private MovieSearchAndScrapeOptions options;
  private List<Movie>                 smartScrapeList;

  public MovieScrapeTask(List<Movie> moviesToScrape, boolean doSearch, MovieSearchAndScrapeOptions options) {
    super(BUNDLE.getString("movie.scraping"));
    this.moviesToScrape = moviesToScrape;
    this.doSearch = doSearch;
    this.options = options;
  }

  @Override
  protected void doInBackground() {
    initThreadPool(3, "scrape");
    start();

    smartScrapeList = new ArrayList<>(0);

    for (Movie movie : moviesToScrape) {
      submitTask(new Worker(movie));
    }
    waitForCompletionOrCancel();

    // initiate smart scrape
    if (!smartScrapeList.isEmpty() && !GraphicsEnvironment.isHeadless()) {
      try {
        SwingUtilities.invokeAndWait(() -> {
          int selectedCount = smartScrapeList.size();
          int index = 0;

          do {
            Movie movie = smartScrapeList.get(index);
            MovieChooserDialog dialogMovieChooser = new MovieChooserDialog(movie, index, selectedCount);
            dialogMovieChooser.setVisible(true);

            if (!dialogMovieChooser.isContinueQueue()) {
              break;
            }

            if (dialogMovieChooser.isNavigateBack()) {
              index -= 1;
            }
            else {
              index += 1;
            }

          } while (index < selectedCount);
        });
      }
      catch (Exception e) {
        LOGGER.error("SmartScrape crashed " + e.getMessage());
      }
    }

    if (MovieModuleManager.SETTINGS.getSyncTrakt()) {
      TmmTask task = new SyncTraktTvTask(moviesToScrape, null);
      TmmTaskManager.getInstance().addUnnamedTask(task);
    }

    LOGGER.info("Done scraping movies)");
  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the text is never right
    publishState(progressDone);
  }

  /****************************************************************************************
   * Helper classes
   ****************************************************************************************/
  private class Worker implements Runnable {
    private MovieList movieList;
    private Movie     movie;

    public Worker(Movie movie) {
      this.movie = movie;
    }

    @Override
    public void run() {
      try {
        movieList = MovieList.getInstance();
        // set up scrapers
        MovieScraperMetadataConfig scraperMetadataConfig = options.getScraperMetadataConfig();
        MediaScraper mediaMetadataScraper = options.getMetadataScraper();
        List<MediaScraper> artworkScrapers = options.getArtworkScrapers();
        List<MediaScraper> trailerScrapers = options.getTrailerScrapers();

        // search movie
        MediaSearchResult result1 = null;
        if (doSearch) {
          result1 = searchForMovie(mediaMetadataScraper);
          if (result1 == null) {
            // append this search request to the UI with search & scrape dialog
            synchronized (smartScrapeList) {
              smartScrapeList.add(movie);
              return;
            }
          }
        }

        // get metadata, artwork and trailers
        if ((doSearch && result1 != null) || !doSearch) {
          MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE);
          options.setResult(result1);
          options.setLanguage(MovieModuleManager.SETTINGS.getScraperLanguage().toLocale());
          options.setCountry(MovieModuleManager.SETTINGS.getCertificationCountry());
          options.setFanartSize(MovieModuleManager.SETTINGS.getImageFanartSize());
          options.setPosterSize(MovieModuleManager.SETTINGS.getImagePosterSize());

          // we didn't do a search - pass imdbid and tmdbid from movie object
          if (!doSearch) {
            for (Entry<String, Object> entry : movie.getIds().entrySet()) {
              options.setId(entry.getKey(), entry.getValue().toString());
            }
          }
          else {
            // override scraper with one from search result
            mediaMetadataScraper = movieList.getMediaScraperById(result1.getProviderId());
          }

          // scrape metadata if wanted
          MediaMetadata md = null;

          if (mediaMetadataScraper != null && mediaMetadataScraper.getMediaProvider() != null) {
            LOGGER.info("=====================================================");
            LOGGER.info("Scraper metadata with scraper: " + mediaMetadataScraper.getMediaProvider().getProviderInfo().getId() + ", "
                + mediaMetadataScraper.getMediaProvider().getProviderInfo().getVersion());
            LOGGER.info(options.toString());
            LOGGER.info("=====================================================");
            try {
              md = ((IMovieMetadataProvider) mediaMetadataScraper.getMediaProvider()).getMetadata(options);
            }
            catch (ScrapeException e) {
              LOGGER.error("searchMovieFallback", e);
              MessageManager.instance.pushMessage(
                  new Message(MessageLevel.ERROR, movie, "message.scrape.metadatamoviefailed", new String[] { ":", e.getLocalizedMessage() }));
            }
            catch (MissingIdException e) {
              LOGGER.warn("missing id for scrape");
              MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, movie, "scraper.error.missingid"));
            }
            catch (UnsupportedMediaTypeException e) {
              LOGGER.warn("unsupported media type: " + mediaMetadataScraper.getMediaProvider().getProviderInfo().getId());
            }

            if (scraperMetadataConfig.isMetadata()) {
              movie.setMetadata(md, scraperMetadataConfig);
            }

            // scrape artwork if wanted
            if (scraperMetadataConfig.isArtwork()) {
              movie.setArtwork(getArtwork(movie, md, artworkScrapers), scraperMetadataConfig);
            }

            // scrape trailer if wanted
            if (scraperMetadataConfig.isTrailer()) {
              movie.setTrailers(getTrailers(movie, md, trailerScrapers));
              movie.saveToDb();
              movie.writeNFO();

              // start automatic movie trailer download
              MovieHelpers.startAutomaticTrailerDownload(movie);
            }
          }
        }
      }
      catch (Exception e) {
        LOGGER.error("Thread crashed", e);
        MessageManager.instance.pushMessage(
            new Message(MessageLevel.ERROR, "MovieScraper", "message.scrape.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
      }
    }

    private MediaSearchResult searchForMovie(MediaScraper mediaMetadataProvider) {
      List<MediaSearchResult> results = movieList.searchMovie(movie.getTitle(), movie, mediaMetadataProvider);
      MediaSearchResult result = null;

      if (results != null && !results.isEmpty()) {
        result = results.get(0);
        // check if there is an other result with 100% score
        if (results.size() > 1) {
          MediaSearchResult result2 = results.get(1);
          // if both results have 100% score - do not take any result
          if (result.getScore() == 1 && result2.getScore() == 1) {
            LOGGER.info("two 100% results, can't decide which to take - ignore result");
            MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, movie, "movie.scrape.toosimilar"));
            return null;
          }
        }

        // get threshold from settings (default 0.75) - to minimize false positives
        final double scraperTreshold = MovieModuleManager.SETTINGS.getScraperThreshold();
        LOGGER.info("using treshold from settings of {}", scraperTreshold);
        if (result.getScore() < scraperTreshold) {
          LOGGER.info("score is lower than " + scraperTreshold + " (" + result.getScore() + ") - ignore result");
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, movie, "movie.scrape.toolowscore", new String[] { String.format("%.2f", scraperTreshold) }));
          return null;
        }
      }
      else {
        LOGGER.info("no result found for " + movie.getTitle());
        MessageManager.instance.pushMessage(new Message(MessageLevel.ERROR, movie, "movie.scrape.nomatchfound"));
      }

      return result;
    }

    private List<MediaArtwork> getArtwork(Movie movie, MediaMetadata metadata, List<MediaScraper> artworkScrapers) {
      List<MediaArtwork> artwork = new ArrayList<>();

      MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE);
      options.setArtworkType(MediaArtworkType.ALL);
      options.setMetadata(metadata);
      options.setImdbId(movie.getImdbId());
      options.setTmdbId(movie.getTmdbId());
      options.setLanguage(MovieModuleManager.SETTINGS.getImageScraperLanguage().toLocale());
      options.setCountry(MovieModuleManager.SETTINGS.getCertificationCountry());
      options.setFanartSize(MovieModuleManager.SETTINGS.getImageFanartSize());
      options.setPosterSize(MovieModuleManager.SETTINGS.getImagePosterSize());

      // scrape providers till one artwork has been found
      for (MediaScraper scraper : artworkScrapers) {
        IMovieArtworkProvider artworkProvider = (IMovieArtworkProvider) scraper.getMediaProvider();
        try {
          artwork.addAll(artworkProvider.getArtwork(options));
        }
        catch (ScrapeException e) {
          LOGGER.error("getArtwork", e);
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, movie, "message.scrape.movieartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
        }
        catch (MissingIdException ignored) {
        }
      }

      return artwork;
    }

    private List<MovieTrailer> getTrailers(Movie movie, MediaMetadata metadata, List<MediaScraper> trailerScrapers) {
      List<MovieTrailer> trailers = new ArrayList<>();

      // add local trailers!
      for (MediaFile mf : movie.getMediaFiles(MediaFileType.TRAILER)) {
        LOGGER.debug("adding local trailer " + mf.getFilename());
        MovieTrailer mt = new MovieTrailer();
        mt.setName(mf.getFilename());
        mt.setProvider("downloaded");
        mt.setQuality(mf.getVideoFormat());
        mt.setInNfo(false);
        mt.setUrl(mf.getFile().toUri().toString());
        trailers.add(mt);
      }

      MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE);
      options.setMetadata(metadata);
      options.setImdbId(movie.getImdbId());
      options.setTmdbId(movie.getTmdbId());
      options.setLanguage(MovieModuleManager.SETTINGS.getScraperLanguage().toLocale());
      options.setCountry(MovieModuleManager.SETTINGS.getCertificationCountry());

      // scrape trailers
      for (MediaScraper trailerScraper : trailerScrapers) {
        try {
          IMovieTrailerProvider trailerProvider = (IMovieTrailerProvider) trailerScraper.getMediaProvider();
          List<MediaTrailer> foundTrailers = trailerProvider.getTrailers(options);
          for (MediaTrailer mediaTrailer : foundTrailers) {
            MovieTrailer movieTrailer = new MovieTrailer(mediaTrailer);
            trailers.add(movieTrailer);
          }
        }
        catch (ScrapeException e) {
          LOGGER.error("getTrailers", e);
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, movie, "message.scrape.movietrailerfailed", new String[] { ":", e.getLocalizedMessage() }));
        }
        catch (MissingIdException | UnsupportedMediaTypeException ignored) {
        }
      }

      return trailers;
    }
  }
}
