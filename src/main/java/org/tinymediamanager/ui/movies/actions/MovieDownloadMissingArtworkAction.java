package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieMissingArtworkDownloadTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.movies.dialogs.MovieScrapeMetadataDialog;

/**
 * The class MovieDownloadMissingArtworkAction is used to download missing artwork for the selected movies
 * 
 * @author Manuel Laggner
 */
public class MovieDownloadMissingArtworkAction extends TmmAction {
  private static final long           serialVersionUID = -4006932829840795735L;
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  public MovieDownloadMissingArtworkAction() {
    putValue(NAME, BUNDLE.getString("movie.downloadmissingartwork")); //$NON-NLS-1$
    putValue(SMALL_ICON, IconManager.IMAGE);
    putValue(LARGE_ICON_KEY, IconManager.IMAGE);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = new ArrayList<>(MovieUIModule.getInstance().getSelectionModel().getSelectedMovies());

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getActiveInstance(), BUNDLE.getString("tmm.nothingselected")); //$NON-NLS-1$
      return;
    }

    MovieScrapeMetadataDialog dialog = new MovieScrapeMetadataDialog(BUNDLE.getString("movie.downloadmissingartwork")); //$NON-NLS-1$
    dialog.setVisible(true);

    // get options from dialog
    MovieSearchAndScrapeOptions options = dialog.getMovieSearchAndScrapeOptions();
    List<MovieScraperMetadataConfig> config = dialog.getMovieScraperMetadataConfig();

    // do we want to scrape?
    if (dialog.shouldStartScrape()) {
      MovieMissingArtworkDownloadTask task = new MovieMissingArtworkDownloadTask(selectedMovies, options, config);
      TmmTaskManager.getInstance().addDownloadTask(task);
    }
  }
}
