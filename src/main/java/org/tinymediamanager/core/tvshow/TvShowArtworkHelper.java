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
package org.tinymediamanager.core.tvshow;

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BACKGROUND;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CHARACTERART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.CLEARLOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.KEYART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.LOGO;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_THUMB;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.IFileNaming;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.ImageUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.tasks.MediaEntityImageFetcherTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonBannerNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonPosterNaming;
import org.tinymediamanager.core.tvshow.filenaming.TvShowSeasonThumbNaming;
import org.tinymediamanager.core.tvshow.tasks.TvShowExtraImageFetcherTask;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;

/**
 * The class TvShowArtworkHelper . A helper class for managing TV show artwork
 * 
 * @author Manuel Laggner
 */
public class TvShowArtworkHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowArtworkHelper.class);

  /**
   * Manage downloading of the chosen artwork type
   * 
   * @param show
   *          the TV show for which artwork has to be downloaded
   * @param type
   *          the artwork type to be downloaded
   */
  public static void downloadArtwork(TvShow show, MediaFileType type) {
    // extra handling for extrafanart & extrathumbs
    if (type == MediaFileType.EXTRAFANART) {
      downloadExtraArtwork(show, type);
      return;
    }

    String url = show.getArtworkUrl(type);
    if (StringUtils.isBlank(url)) {
      return;
    }

    List<IFileNaming> fileNamings = new ArrayList<>();

    switch (type) {
      case FANART:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getFanartFilenames());
        break;

      case POSTER:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getPosterFilenames());
        break;

      case BANNER:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getBannerFilenames());
        break;

      case LOGO:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getLogoFilenames());
        break;

      case CLEARLOGO:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getClearlogoFilenames());
        break;

      case CHARACTERART:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getCharacterartFilenames());
        break;

      case CLEARART:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getClearartFilenames());
        break;

      case THUMB:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getThumbFilenames());
        break;

      case KEYART:
        fileNamings.addAll(TvShowModuleManager.SETTINGS.getKeyartFilenames());
        break;

      default:
        return;
    }

    int i = 0;
    for (IFileNaming naming : fileNamings) {
      boolean firstImage = false;
      String filename = naming.getFilename("", Utils.getArtworkExtension(url));

      if (StringUtils.isBlank(filename)) {
        continue;
      }

      if (++i == 1) {
        firstImage = true;
      }

      // get image in thread
      MediaEntityImageFetcherTask task = new MediaEntityImageFetcherTask(show, url, MediaFileType.getMediaArtworkType(type), filename, firstImage);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }

    // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
    if (url.startsWith("file:")) {
      show.removeArtworkUrl(type);
    }
  }

  /**
   * set & download missing artwork for the given TV show
   *
   * @param tvShow
   *          the TV show to set the artwork for
   * @param artwork
   *          a list of all artworks to be set
   */
  public static void downloadMissingArtwork(TvShow tvShow, List<MediaArtwork> artwork) {
    // sort artwork once again (langu/rating)
    artwork.sort(new MediaArtwork.MediaArtworkComparator(TvShowModuleManager.SETTINGS.getScraperLanguage().name()));

    // poster
    if (tvShow.getMediaFiles(MediaFileType.POSTER).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.POSTER);
    }

    // fanart
    if (tvShow.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.BACKGROUND);
    }

    // logo
    if (tvShow.getMediaFiles(MediaFileType.LOGO).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.LOGO);
    }

    // clearlogo
    if (tvShow.getMediaFiles(MediaFileType.CLEARLOGO).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.CLEARLOGO);
    }

    // clearart
    if (tvShow.getMediaFiles(MediaFileType.CLEARART).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.CLEARART);
    }

    // banner
    if (tvShow.getMediaFiles(MediaFileType.BANNER).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.BANNER);
    }

    // thumb
    if (tvShow.getMediaFiles(MediaFileType.THUMB).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.THUMB);
    }

    // discart
    if (tvShow.getMediaFiles(MediaFileType.DISC).isEmpty()) {
      setBestArtwork(tvShow, artwork, MediaArtworkType.DISC);
    }

    // characterart
    if (tvShow.getMediaFiles(MediaFileType.CHARACTERART).isEmpty()) {
      setBestArtwork(tvShow, artwork, CHARACTERART);
    }

    // keyart
    if (tvShow.getMediaFiles(MediaFileType.KEYART).isEmpty()) {
      setBestArtwork(tvShow, artwork, KEYART);
    }

    for (TvShowSeason season : tvShow.getSeasons()) {
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_POSTER))) {
        for (MediaArtwork art : artwork) {
          if (art.getSeason() == season.getSeason()) {
            tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_POSTER);
            downloadSeasonPoster(tvShow, art.getSeason());
          }
        }
      }
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_BANNER))) {
        for (MediaArtwork art : artwork) {
          if (art.getSeason() == season.getSeason()) {
            tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_BANNER);
            downloadSeasonBanner(tvShow, art.getSeason());
          }
        }
      }
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_THUMB))) {
        for (MediaArtwork art : artwork) {
          if (art.getSeason() == season.getSeason()) {
            tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_THUMB);
            downloadSeasonThumb(tvShow, art.getSeason());
          }
        }
      }
    }

    // update DB
    tvShow.saveToDb();
  }

  /**
   * choose the best artwork for this tv show
   *
   * @param tvShow
   *          our tv show
   * @param artwork
   *          the artwork list
   * @param type
   *          the type to download
   */
  private static void setBestArtwork(TvShow tvShow, List<MediaArtwork> artwork, MediaArtworkType type) {
    for (MediaArtwork art : artwork) {
      if (art.getType() == type && StringUtils.isNotBlank(art.getDefaultUrl())) {
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.getMediaFileType(type));
        downloadArtwork(tvShow, MediaFileType.getMediaFileType(type));
        break;
      }
    }
  }

  /**
   * detect if there is missing artwork for the given TV show
   * 
   * @param tvShow
   *          the TV show to check artwork for
   * @return true/false
   */
  public static boolean hasMissingArtwork(TvShow tvShow) {
    if (tvShow.getMediaFiles(MediaFileType.POSTER).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.FANART).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.BANNER).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.DISC).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.LOGO).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.CLEARLOGO).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.CLEARART).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.THUMB).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.CHARACTERART).isEmpty()) {
      return true;
    }
    if (tvShow.getMediaFiles(MediaFileType.KEYART).isEmpty()) {
      return true;
    }
    for (TvShowSeason season : tvShow.getSeasons()) {
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_POSTER))) {
        return true;
      }
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_BANNER))) {
        return true;
      }
      if (StringUtils.isBlank(season.getArtworkFilename(SEASON_THUMB))) {
        return true;
      }
    }

    return false;
  }

  /**
   * detect if there is missing artwork for the given episode
   * 
   * @param episode
   *          the episode to check artwork for
   * @return true/false
   */
  public static boolean hasMissingArtwork(TvShowEpisode episode) {
    return episode.getMediaFiles(MediaFileType.THUMB).isEmpty();
  }

  public static void downloadSeasonArtwork(TvShow show, int season, MediaArtworkType artworkType) {
    switch (artworkType) {
      case SEASON_POSTER:
        downloadSeasonPoster(show, season);
        break;

      case SEASON_BANNER:
        downloadSeasonBanner(show, season);
        break;

      case SEASON_THUMB:
        downloadSeasonThumb(show, season);
        break;

      default:
        return;
    }
  }

  /**
   * Download the season poster
   * 
   * @param show
   *          the TV show
   * @param season
   *          the season to download the poster for
   */
  private static void downloadSeasonPoster(TvShow show, int season) {
    String seasonPosterUrl = show.getSeasonArtworkUrl(season, SEASON_POSTER);

    TvShowSeason tvShowSeason = null;
    // try to get a season instance
    for (TvShowSeason s : show.getSeasons()) {
      if (s.getSeason() == season) {
        tvShowSeason = s;
        break;
      }
    }

    for (TvShowSeasonPosterNaming seasonPosterNaming : TvShowModuleManager.SETTINGS.getSeasonPosterFilenames()) {
      Path destFile = Paths
          .get(show.getPathNIO() + File.separator + seasonPosterNaming.getFilename(show, season, Utils.getArtworkExtension(seasonPosterUrl)));

      SeasonArtworkImageFetcher task = new SeasonArtworkImageFetcher(show, destFile, tvShowSeason, seasonPosterUrl, SEASON_POSTER);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }

    // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
    if (tvShowSeason != null && seasonPosterUrl.startsWith("file:")) {
      tvShowSeason.removeArtworkUrl(SEASON_POSTER);
    }
  }

  /**
   * Download the season banner
   *
   * @param show
   *          the TV show
   * @param season
   *          the season to download the banner for
   */
  private static void downloadSeasonBanner(TvShow show, int season) {
    String seasonBannerUrl = show.getSeasonArtworkUrl(season, SEASON_BANNER);

    TvShowSeason tvShowSeason = null;
    // try to get a season instance
    for (TvShowSeason s : show.getSeasons()) {
      if (s.getSeason() == season) {
        tvShowSeason = s;
        break;
      }
    }

    for (TvShowSeasonBannerNaming seasonBannerNaming : TvShowModuleManager.SETTINGS.getSeasonBannerFilenames()) {
      Path destFile = Paths
          .get(show.getPathNIO() + File.separator + seasonBannerNaming.getFilename(show, season, Utils.getArtworkExtension(seasonBannerUrl)));

      SeasonArtworkImageFetcher task = new SeasonArtworkImageFetcher(show, destFile, tvShowSeason, seasonBannerUrl, SEASON_BANNER);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }

    // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
    if (tvShowSeason != null && seasonBannerUrl.startsWith("file:")) {
      tvShowSeason.removeArtworkUrl(SEASON_BANNER);
    }
  }

  /**
   * Download the season thumb
   *
   * @param show
   *          the TV show
   * @param season
   *          the season to download the thumb for
   */
  private static void downloadSeasonThumb(TvShow show, int season) {
    String seasonThumbUrl = show.getSeasonArtworkUrl(season, SEASON_THUMB);

    TvShowSeason tvShowSeason = null;
    // try to get a season instance
    for (TvShowSeason s : show.getSeasons()) {
      if (s.getSeason() == season) {
        tvShowSeason = s;
        break;
      }
    }

    for (TvShowSeasonThumbNaming seasonThumbNaming : TvShowModuleManager.SETTINGS.getSeasonThumbFilenames()) {
      Path destFile = Paths
          .get(show.getPathNIO() + File.separator + seasonThumbNaming.getFilename(show, season, Utils.getArtworkExtension(seasonThumbUrl)));

      SeasonArtworkImageFetcher task = new SeasonArtworkImageFetcher(show, destFile, tvShowSeason, seasonThumbUrl, SEASON_THUMB);
      TmmTaskManager.getInstance().addImageDownloadTask(task);
    }

    // if that has been a local file, remove it from the artwork urls after we've already started the download(copy) task
    if (tvShowSeason != null && seasonThumbUrl.startsWith("file:")) {
      tvShowSeason.removeArtworkUrl(SEASON_THUMB);
    }
  }

  private static class SeasonArtworkImageFetcher implements Runnable {
    private TvShow           tvShow;
    private TvShowSeason     tvShowSeason;
    private MediaArtworkType artworkType;
    private Path             destinationPath;
    private String           filename;
    private String           url;

    SeasonArtworkImageFetcher(TvShow show, Path destFile, TvShowSeason tvShowSeason, String url, MediaArtworkType type) {
      this.tvShow = show;
      this.destinationPath = destFile.getParent();
      this.filename = destFile.getFileName().toString();
      this.artworkType = type;
      this.tvShowSeason = tvShowSeason;
      this.url = url;
    }

    @Override
    public void run() {
      String oldFilename = "";

      if (tvShowSeason != null) {
        oldFilename = tvShow.getSeasonArtwork(tvShowSeason.getSeason(), artworkType);
        tvShowSeason.clearArtwork(artworkType);
      }

      LOGGER.debug("writing season artwork {}", filename);

      // fetch and store images
      try {
        Path destFile = ImageUtils.downloadImage(url, destinationPath, filename);

        // invalidate image cache
        if (tvShowSeason != null) {
          tvShowSeason.setArtwork(destFile, artworkType);
        }

        // build up image cache
        ImageCache.invalidateCachedImage(destFile);
        ImageCache.cacheImageSilently(destFile);
      }
      catch (InterruptedException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.error("fetch image {} - {}", this.url, e);
        // fallback
        if (tvShowSeason != null && !oldFilename.isEmpty()) {
          tvShowSeason.setArtwork(Paths.get(oldFilename), artworkType);
        }
        // build up image cache
        ImageCache.invalidateCachedImage(Paths.get(oldFilename));
        ImageCache.cacheImageSilently(Paths.get(oldFilename));
      }
      finally {
        tvShow.saveToDb();
      }
    }
  }

  public static void setArtwork(TvShow tvShow, List<MediaArtwork> artwork) {
    // poster
    for (MediaArtwork art : artwork) {
      if (art.getType() == POSTER) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.POSTER);
        // and download it
        downloadArtwork(tvShow, MediaFileType.POSTER);
        break;
      }
    }

    // fanart
    for (MediaArtwork art : artwork) {
      if (art.getType() == BACKGROUND) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.FANART);
        // and download it
        downloadArtwork(tvShow, MediaFileType.FANART);
        break;
      }
    }

    // banner
    for (MediaArtwork art : artwork) {
      if (art.getType() == BANNER) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.BANNER);
        // and download it
        downloadArtwork(tvShow, MediaFileType.BANNER);
        break;
      }
    }

    // logo
    for (MediaArtwork art : artwork) {
      if (art.getType() == LOGO) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.LOGO);
        // and download it
        downloadArtwork(tvShow, MediaFileType.LOGO);
        break;
      }
    }

    // clearlogo
    for (MediaArtwork art : artwork) {
      if (art.getType() == CLEARLOGO) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.CLEARLOGO);
        // and download it
        downloadArtwork(tvShow, MediaFileType.CLEARLOGO);
        break;
      }
    }

    // clearart
    for (MediaArtwork art : artwork) {
      if (art.getType() == CLEARART) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.CLEARART);
        // and download it
        downloadArtwork(tvShow, MediaFileType.CLEARART);
        break;
      }
    }

    // thumb
    for (MediaArtwork art : artwork) {
      if (art.getType() == THUMB) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.THUMB);
        // and download it
        downloadArtwork(tvShow, MediaFileType.THUMB);
        break;
      }
    }

    // characterart
    for (MediaArtwork art : artwork) {
      if (art.getType() == CHARACTERART) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.CHARACTERART);
        // and download it
        downloadArtwork(tvShow, MediaFileType.CHARACTERART);
        break;
      }
    }

    // keyart
    for (MediaArtwork art : artwork) {
      if (art.getType() == KEYART) {
        // set url
        tvShow.setArtworkUrl(art.getDefaultUrl(), MediaFileType.KEYART);
        // and download it
        downloadArtwork(tvShow, MediaFileType.KEYART);
        break;
      }
    }

    // season poster
    HashMap<Integer, String> seasonPosters = new HashMap<>();
    for (MediaArtwork art : artwork) {
      if (art.getType() == MediaArtworkType.SEASON_POSTER && art.getSeason() >= 0) {
        // check if there is already an artwork for this season
        String url = seasonPosters.get(art.getSeason());
        if (StringUtils.isBlank(url)) {
          tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_POSTER);
          TvShowArtworkHelper.downloadSeasonArtwork(tvShow, art.getSeason(), SEASON_POSTER);
          seasonPosters.put(art.getSeason(), art.getDefaultUrl());
        }
      }
    }

    // season banner
    HashMap<Integer, String> seasonBanners = new HashMap<>();
    for (MediaArtwork art : artwork) {
      if (art.getType() == MediaArtworkType.SEASON_BANNER && art.getSeason() >= 0) {
        // check if there is already an artwork for this season
        String url = seasonBanners.get(art.getSeason());
        if (StringUtils.isBlank(url)) {
          tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_BANNER);
          TvShowArtworkHelper.downloadSeasonArtwork(tvShow, art.getSeason(), SEASON_BANNER);
          seasonBanners.put(art.getSeason(), art.getDefaultUrl());
        }
      }
    }

    // season thumb
    HashMap<Integer, String> seasonThumbs = new HashMap<>();
    for (MediaArtwork art : artwork) {
      if (art.getType() == MediaArtworkType.SEASON_THUMB && art.getSeason() >= 0) {
        // check if there is already an artwork for this season
        String url = seasonThumbs.get(art.getSeason());
        if (StringUtils.isBlank(url)) {
          tvShow.setSeasonArtworkUrl(art.getSeason(), art.getDefaultUrl(), SEASON_THUMB);
          TvShowArtworkHelper.downloadSeasonArtwork(tvShow, art.getSeason(), SEASON_THUMB);
          seasonThumbs.put(art.getSeason(), art.getDefaultUrl());
        }
      }
    }

    // extrafanart
    List<String> extrafanarts = new ArrayList<>();
    if (TvShowModuleManager.SETTINGS.isImageExtraFanart() && TvShowModuleManager.SETTINGS.getImageExtraFanartCount() > 0) {
      for (MediaArtwork art : artwork) {
        // only get artwork in desired resolution
        if (art.getType() == MediaArtworkType.BACKGROUND) {
          extrafanarts.add(art.getDefaultUrl());
          if (extrafanarts.size() >= TvShowModuleManager.SETTINGS.getImageExtraFanartCount()) {
            break;
          }
        }
      }
      tvShow.setExtraFanartUrls(extrafanarts);
      if (!extrafanarts.isEmpty()) {
        downloadArtwork(tvShow, MediaFileType.EXTRAFANART);
      }
    }

    // update DB
    tvShow.saveToDb();
    tvShow.writeNFO(); // to get the artwork urls into the NFO
  }

  private static void downloadExtraArtwork(TvShow tvShow, MediaFileType type) {
    // get images in thread
    TvShowExtraImageFetcherTask task = new TvShowExtraImageFetcherTask(tvShow, type);
    TmmTaskManager.getInstance().addImageDownloadTask(task);
  }
}
