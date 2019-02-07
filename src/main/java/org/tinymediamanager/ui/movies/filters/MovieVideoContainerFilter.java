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
package org.tinymediamanager.ui.movies.filters;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.movies.AbstractMovieUIFilter;

/**
 * this class is used for a video container movie filter
 * 
 * @author Manuel Laggner
 */
public class MovieVideoContainerFilter extends AbstractMovieUIFilter {
  private MovieList         movieList = MovieList.getInstance();

  private JComboBox<String> comboBox;

  public MovieVideoContainerFilter() {
    super();
    buildAndInstallContainerArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallContainerArray();
    movieList.addPropertyChangeListener(Constants.VIDEO_CONTAINER, propertyChangeListener);
  }

  @Override
  public String getId() {
    return "movieVideoContainer";
  }

  @Override
  public boolean accept(Movie movie) {
    String videoContainer = (String) comboBox.getSelectedItem();

    return videoContainer != null && videoContainer.equalsIgnoreCase(movie.getMediaInfoContainerFormat());
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(BUNDLE.getString("metatag.container")); //$NON-NLS-1$
  }

  @Override
  protected JComponent createFilterComponent() {
    comboBox = new JComboBox<>();
    return comboBox;
  }

  @Override
  public String getFilterValueAsString() {
    try {
      return (String) comboBox.getSelectedItem();
    }
    catch (Exception e) {
      return null;
    }
  }

  @Override
  public void setFilterValue(Object value) {
    comboBox.setSelectedItem(value);
  }

  private void buildAndInstallContainerArray() {
    // remove the listener to not firing unnecessary events
    comboBox.removeActionListener(actionListener);

    String oldValue = (String) comboBox.getSelectedItem();
    comboBox.removeAllItems();

    List<String> containers = new ArrayList<>(movieList.getVideoContainersInMovies());
    Collections.sort(containers);
    for (String container : containers) {
      comboBox.addItem(container);
    }

    if (oldValue != null) {
      comboBox.setSelectedItem(oldValue);
    }

    // re-add the itemlistener
    comboBox.addActionListener(actionListener);
  }
}
