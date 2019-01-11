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
package org.tinymediamanager.ui.tvshows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.exceptions.UnsupportedMediaTypeException;
import org.tinymediamanager.scraper.mediaprovider.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.mediaprovider.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.UTF8Control;

/**
 * 
 * @author Manuel Laggner
 */
public class TvShowChooserModel extends AbstractModelObject {
  private static final ResourceBundle    BUNDLE          = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$
  private static final Logger            LOGGER          = LoggerFactory.getLogger(TvShowChooserModel.class);
  public static final TvShowChooserModel emptyResult     = new TvShowChooserModel();

  private MediaScraper                   mediaScraper    = null;
  private List<MediaScraper>             artworkScrapers = null;
  private MediaLanguages                 language        = null;
  private MediaSearchResult              result          = null;
  private MediaMetadata                  metadata        = null;

  private float                          score           = 0;
  private String                         title           = "";
  private String                         originalTitle   = "";
  private String                         overview        = "";
  private String                         year            = "";
  private String                         combinedName    = "";
  private String                         posterUrl       = "";
  private boolean                        scraped         = false;

  public TvShowChooserModel(MediaScraper mediaScraper, List<MediaScraper> artworkScrapers, MediaSearchResult result, MediaLanguages language) {
    this.mediaScraper = mediaScraper;
    this.artworkScrapers = artworkScrapers;
    this.result = result;
    this.language = language;

    setTitle(result.getTitle());
    setOriginalTitle(result.getOriginalTitle());

    if (result.getYear() != 0) {
      setYear(Integer.toString(result.getYear()));
    }
    else {
      setYear("");
    }
    // combined title (title (year))
    setCombinedName();

    score = result.getScore();
  }

  /**
   * create the empty search result.
   */
  private TvShowChooserModel() {
    setTitle(BUNDLE.getString("chooser.nothingfound")); //$NON-NLS-1$
    combinedName = title;
  }

  public float getScore() {
    return score;
  }

  public void setTitle(String title) {
    String oldValue = this.title;
    this.title = StrgUtils.getNonNullString(title);
    firePropertyChange("title", oldValue, this.title);
  }

  public void setOriginalTitle(String originalTitle) {
    String oldValue = this.originalTitle;
    this.originalTitle = StrgUtils.getNonNullString(originalTitle);
    firePropertyChange("originalTitle", oldValue, this.originalTitle);
  }

  public void setOverview(String overview) {
    String oldValue = this.overview;
    this.overview = StrgUtils.getNonNullString(overview);
    firePropertyChange("overview", oldValue, this.overview);
  }

  public String getTitle() {
    return title;
  }

  public String getOriginalTitle() {
    return originalTitle;
  }

  public String getOverview() {
    return overview;
  }

  public String getPosterUrl() {
    return posterUrl;
  }

  public void setPosterUrl(String newValue) {
    String oldValue = posterUrl;
    posterUrl = StrgUtils.getNonNullString(newValue);
    firePropertyChange("posterUrl", oldValue, newValue);
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    String oldValue = this.year;
    this.year = year;
    firePropertyChange("year", oldValue, this.year);
  }

  public void setCombinedName() {
    String oldValue = this.combinedName;

    if (StringUtils.isNotBlank(getYear())) {
      this.combinedName = getTitle() + " (" + getYear() + ")";
    }
    else {
      this.combinedName = getTitle();
    }
    firePropertyChange("combinedName", oldValue, this.combinedName);
  }

  public String getCombinedName() {
    return combinedName;
  }

  /**
   * Scrape meta data.
   */
  public void scrapeMetaData() {
    try {
      // poster for preview
      setPosterUrl(result.getPosterUrl());

      MediaScrapeOptions options = new MediaScrapeOptions(MediaType.TV_SHOW);
      options.setResult(result);
      options.setLanguage(language.toLocale());
      options.setCountry(TvShowModuleManager.SETTINGS.getCertificationCountry());
      LOGGER.info("=====================================================");
      LOGGER.info("Scraper metadata with scraper: " + mediaScraper.getMediaProvider().getProviderInfo().getId());
      LOGGER.info(options.toString());
      LOGGER.info("=====================================================");
      metadata = ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getMetadata(options);
      setOverview(metadata.getPlot());

      if (StringUtils.isBlank(posterUrl) && !metadata.getMediaArt(MediaArtworkType.POSTER).isEmpty()) {
        setPosterUrl(metadata.getMediaArt(MediaArtworkType.POSTER).get(0).getPreviewUrl());
      }

      setScraped(true);

    }
    catch (ScrapeException e) {
      LOGGER.error("getMetadata", e);
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowChooser", "message.scrape.metadatatvshowfailed",
          new String[] { ":", e.getLocalizedMessage() }));
    }
    catch (MissingIdException e) {
      LOGGER.warn("missing id for scrape");
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowChooser", "scraper.error.missingid"));
    }
    catch (UnsupportedMediaTypeException | NothingFoundException ignored) {
    }
  }

  public List<TvShowEpisode> getEpisodesForDisplay() {
    List<TvShowEpisode> episodes = new ArrayList<>();

    if (!scraped) {
      return episodes;
    }

    MediaScrapeOptions options = new MediaScrapeOptions(MediaType.TV_EPISODE);
    options.setLanguage(language.toLocale());
    options.setCountry(TvShowModuleManager.SETTINGS.getCertificationCountry());
    for (Entry<String, Object> entry : metadata.getIds().entrySet()) {
      options.setId(entry.getKey(), entry.getValue().toString());
    }

    try {
      List<MediaMetadata> mediaEpisodes = ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getEpisodeList(options);
      for (MediaMetadata me : mediaEpisodes) {
        TvShowEpisode ep = new TvShowEpisode();
        ep.setEpisode(me.getEpisodeNumber());
        ep.setSeason(me.getSeasonNumber());
        ep.setDvdEpisode(me.getDvdEpisodeNumber());
        ep.setDvdSeason(me.getDvdSeasonNumber());
        ep.setTitle(me.getTitle());
        ep.setOriginalTitle(me.getOriginalTitle());
        ep.setPlot(me.getPlot());
        episodes.add(ep);
      }
    }
    catch (ScrapeException e) {
      LOGGER.error("getEpisodeList", e);
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowChooser", "message.scrape.episodelistfailed",
          new String[] { ":", e.getLocalizedMessage() }));
    }
    catch (MissingIdException e) {
      LOGGER.warn("missing id for scrape");
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowChooser", "scraper.error.missingid"));
    }
    catch (UnsupportedMediaTypeException ignored) {
    }
    return episodes;
  }

  public MediaMetadata getMetadata() {
    return metadata;
  }

  private void setScraped(boolean newvalue) {
    boolean oldValue = scraped;
    scraped = newvalue;
    firePropertyChange("scraped", oldValue, newvalue);
  }

  public boolean isScraped() {
    return scraped;
  }

  public MediaLanguages getLanguage() {
    return language;
  }

  public void startArtworkScrapeTask(TvShow tvShow, TvShowScraperMetadataConfig config) {
    TmmTaskManager.getInstance().addUnnamedTask(new ArtworkScrapeTask(tvShow, config));
  }

  private class ArtworkScrapeTask extends TmmTask {
    private TvShow                      tvShowToScrape;
    private TvShowScraperMetadataConfig config;

    public ArtworkScrapeTask(TvShow tvShow, TvShowScraperMetadataConfig config) {
      super(BUNDLE.getString("message.scrape.artwork") + " " + tvShow.getTitle(), 0, TaskType.BACKGROUND_TASK);
      this.tvShowToScrape = tvShow;
      this.config = config;
    }

    @Override
    protected void doInBackground() {
      if (!scraped) {
        return;
      }

      List<MediaArtwork> artwork = new ArrayList<>();

      MediaScrapeOptions options = new MediaScrapeOptions(MediaType.TV_SHOW);
      options.setArtworkType(MediaArtworkType.ALL);
      options.setMetadata(metadata);
      for (Entry<String, Object> entry : tvShowToScrape.getIds().entrySet()) {
        options.setId(entry.getKey(), entry.getValue().toString());
      }

      options.setLanguage(language.toLocale());
      options.setCountry(TvShowModuleManager.SETTINGS.getCertificationCountry());

      // scrape providers till one artwork has been found
      for (MediaScraper artworkScraper : artworkScrapers) {
        ITvShowArtworkProvider artworkProvider = (ITvShowArtworkProvider) artworkScraper.getMediaProvider();
        try {
          artwork.addAll(artworkProvider.getArtwork(options));
        }
        catch (ScrapeException e) {
          LOGGER.error("getArtwork", e);
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, tvShowToScrape, "message.scrape.tvshowartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
        }
        catch (MissingIdException ignored) {
        }
      }

      // at last take the poster from the result
      if (StringUtils.isNotBlank(getPosterUrl())) {
        MediaArtwork ma = new MediaArtwork(result.getProviderId(), MediaArtworkType.POSTER);
        ma.setDefaultUrl(getPosterUrl());
        ma.setPreviewUrl(getPosterUrl());
        artwork.add(ma);
      }

      tvShowToScrape.setArtwork(artwork, config);
    }
  }
}
