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
package org.tinymediamanager.scraper.animated;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.animated.entities.Base;
import org.tinymediamanager.scraper.animated.entities.Entry;
import org.tinymediamanager.scraper.animated.entities.Movie;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.mediaprovider.IMovieArtworkProvider;
import org.tinymediamanager.scraper.util.ListUtils;

import com.google.gson.Gson;

import net.xeoh.plugins.base.annotations.PluginImplementation;

/**
 * The Class FanartTvMetadataProvider. An artwork provider for the site fanart.tv
 *
 * @author Manuel Laggner
 */
@PluginImplementation
public class AnimatedMetadataProvider implements IMovieArtworkProvider {
  private static final Logger      LOGGER       = LoggerFactory.getLogger(AnimatedMetadataProvider.class);
  private static MediaProviderInfo providerInfo = createMediaProviderInfo();
  private static final String      BASE_URL     = "http://consiliumb.com/animatedgifs/";
  private Base                     json         = null;

  private static MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo providerInfo = new MediaProviderInfo("animated", "AnimatedPosters",
        "<html><h3>Animated Movie Posters</h3><br />as seen on http://forum.kodi.tv/showthread.php?tid=215727 :)</html>",
        AnimatedMetadataProvider.class.getResource("/animated.png"));
    providerInfo.setVersion(AnimatedMetadataProvider.class);
    return providerInfo;
  }

  public AnimatedMetadataProvider() {
  }

  Base getJson() {
    initJson();
    return json;
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public List<MediaArtwork> getArtwork(MediaScrapeOptions options) throws MissingIdException {
    LOGGER.debug("getArtwork() " + options.toString());

    // lazy loading of the json
    initJson();

    List<MediaArtwork> artwork;

    switch (options.getType()) {
      case MOVIE:
        artwork = getMovieArtwork(options);
        break;

      default:
        artwork = new ArrayList<>(1);
    }

    // buffer the artwork
    MediaMetadata md = options.getMetadata();
    if (md != null && artwork.size() > 0) {
      md.addMediaArt(artwork);
    }

    return artwork;
  }

  private synchronized void initJson() {
    if (json == null) {
      json = loadJson();
    }
  }

  private List<MediaArtwork> getMovieArtwork(MediaScrapeOptions options) throws MissingIdException {
    List<MediaArtwork> returnArtwork = new ArrayList<>();

    MediaArtworkType artworkType = options.getArtworkType();
    if (artworkType != MediaArtworkType.POSTER && artworkType != MediaArtworkType.BACKGROUND && artworkType != MediaArtworkType.ALL) {
      // we only have these two
      return returnArtwork;
    }

    String imdbId = options.getImdbId();
    if (StringUtils.isBlank(imdbId)) {
      LOGGER.info("no IMDB id set - returning");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    LOGGER.info("getArtwork for IMDB id: " + imdbId);

    if (json == null) {
      // whoops. can this be?
      return returnArtwork;
    }
    Movie m = json.getMovieByImdbId(imdbId);
    if (m == null) {
      LOGGER.info("no movie with IMDB id " + imdbId + " found");
      return returnArtwork;
    }

    returnArtwork = prepareArtwork(m, artworkType);

    String language = "";
    if (options.getLanguage() != null) {
      language = options.getLanguage().getLanguage();
    }
    returnArtwork.sort(new MediaArtwork.MediaArtworkComparator(language));
    return returnArtwork;
  }

  private List<MediaArtwork> prepareArtwork(Movie m, MediaArtworkType artworkType) {
    List<MediaArtwork> artworks = new ArrayList<>();

    switch (artworkType) {
      case POSTER:
        artworks.addAll(genMA(m.getPosters(), artworkType));
        break;
      case BACKGROUND:
        artworks.addAll(genMA(m.getBackgrounds(), artworkType));
        break;
      case ALL:
        artworks.addAll(genMA(m.getPosters(), MediaArtworkType.POSTER));
        artworks.addAll(genMA(m.getBackgrounds(), MediaArtworkType.BACKGROUND));
        break;
      default:
        break;
    }
    return artworks;
  }

  private List<MediaArtwork> genMA(List<Entry> entries, MediaArtworkType type) {
    List<MediaArtwork> artworks = new ArrayList<>();
    for (Entry image : ListUtils.nullSafe(entries)) {
      MediaArtwork ma = new MediaArtwork(providerInfo.getId(), type);
      ma.setDefaultUrl(BASE_URL + image.getOriginal());
      ma.setPreviewUrl(BASE_URL + image.getImage());
      ma.setAnimated(true);
      // ma.setImdbId(m.getImdbid());
      ma.setLanguage(image.getLanguage().toLowerCase(Locale.ROOT));
      artworks.add(ma);
    }
    return artworks;
  }

  private Base loadJson() {
    Base b = null;
    Gson gson = new Gson();

    try {
      Url url = new OnDiskCachedUrl(BASE_URL + "movies.json", 1, TimeUnit.DAYS);
      b = gson.fromJson(new InputStreamReader(url.getInputStream()), Base.class);
    }
    catch (Exception e) {
      LOGGER.warn("Error downloading json", e);
    }

    return b;
  }
}
