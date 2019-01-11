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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.KeyStroke;

import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.tasks.TvShowEpisodeScrapeTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowChooserDialog;

/**
 * The class TvShowScrapeNewItemsAction. Scrape all new items
 * 
 * @author Manuel Laggner
 */
public class TvShowScrapeNewItemsAction extends TmmAction {
  private static final long           serialVersionUID = -3365542777082781952L;
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  public TvShowScrapeNewItemsAction() {
    putValue(NAME, BUNDLE.getString("tvshow.scrape.newitems")); //$NON-NLS-1$
    putValue(LARGE_ICON_KEY, IconManager.SEARCH);
    putValue(SMALL_ICON, IconManager.SEARCH);
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK + ActionEvent.SHIFT_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShow> newTvShows = new ArrayList<>();
    List<TvShowEpisode> newEpisodes = new ArrayList<>();

    for (TvShow tvShow : new ArrayList<>(TvShowList.getInstance().getTvShows())) {
      // if there is at least one new episode and no scraper id we assume the TV show is new
      if (tvShow.isNewlyAdded() && !tvShow.isScraped()) {
        newTvShows.add(tvShow);
        continue;
      }
      // else: check every episode if there is a new episode
      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        if (episode.isNewlyAdded() && !episode.isScraped()) {
          newEpisodes.add(episode);
        }
      }
    }

    // now start the scrape tasks
    // epsiode scraping can run in background
    TvShowEpisodeScrapeTask task = new TvShowEpisodeScrapeTask(newEpisodes, TvShowList.getInstance().getDefaultMediaScraper(),
        TvShowModuleManager.SETTINGS.getScraperMetadataConfig());
    TmmTaskManager.getInstance().addUnnamedTask(task);

    // whereas tv show scraping has to run in foreground
    if (newTvShows.size() > 0) {
      int count = newTvShows.size();
      int index = 0;

      do {
        TvShow tvShow = newTvShows.get(index);
        TvShowChooserDialog chooser = new TvShowChooserDialog(tvShow, index, count);
        chooser.setVisible(true);

        if (!chooser.isContinueQueue()) {
          break;
        }

        if (chooser.isNavigateBack()) {
          index -= 1;
        }
        else {
          index += 1;
        }

      } while (index < count);
    }
  }
}
