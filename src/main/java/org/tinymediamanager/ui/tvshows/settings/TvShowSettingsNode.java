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

package org.tinymediamanager.ui.tvshows.settings;

import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.settings.TmmSettingsNode;

import java.util.ResourceBundle;

/**
 * the class {@link TvShowSettingsNode} provides all settings pages
 *
 * @author Manuel Laggner
 */
public class TvShowSettingsNode extends TmmSettingsNode {
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  public TvShowSettingsNode() {
    super(BUNDLE.getString("Settings.tvshow"), new TvShowSettingsPanel());//$NON-NLS-1$

    addChild(new TmmSettingsNode(BUNDLE.getString("Settings.source"), new TvShowDatasourceSettingsPanel()));//$NON-NLS-1$

    TmmSettingsNode scraperSettingsNode = new TmmSettingsNode(BUNDLE.getString("Settings.scraper"), new TvShowScraperSettingsPanel());//$NON-NLS-1$
    scraperSettingsNode.addChild(new TmmSettingsNode(BUNDLE.getString("Settings.scraper.options"), new TvShowScraperOptionsSettingsPanel()));//$NON-NLS-1$
    scraperSettingsNode.addChild(new TmmSettingsNode(BUNDLE.getString("Settings.nfo"), new TvShowScraperNfoSettingsPanel()));//$NON-NLS-1$
    addChild(scraperSettingsNode);

    TmmSettingsNode imageSettingsNode = new TmmSettingsNode(BUNDLE.getString("Settings.images"), new TvShowImageSettingsPanel());//$NON-NLS-1$
    imageSettingsNode.addChild(new TmmSettingsNode(BUNDLE.getString("Settings.artwork.naming"), new TvShowImageTypeSettingsPanel()));//$NON-NLS-1$
    addChild(imageSettingsNode);

    addChild(new TmmSettingsNode(BUNDLE.getString("Settings.trailer"), new TvShowTrailerSettingsPanel()));//$NON-NLS-1$
    addChild(new TmmSettingsNode(BUNDLE.getString("Settings.subtitle"), new TvShowSubtitleSettingsPanel()));//$NON-NLS-1$
    addChild(new TmmSettingsNode(BUNDLE.getString("Settings.renamer"), new TvShowRenamerSettingsPanel()));//$NON-NLS-1$
  }
}
