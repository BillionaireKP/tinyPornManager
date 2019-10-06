package org.tinymediamanager.scraper.hdtrailersnet;

import org.junit.Test;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaTrailer;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.mediaprovider.IMovieTrailerProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class HDTrailersNetTrailerProviderTest {

  @Test
  public void testScrapeTrailer() {
    IMovieTrailerProvider mp;
    try {
      mp = new HDTrailersNetTrailerProvider();

      MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE);

      MediaMetadata md = new MediaMetadata("foo");
      md.setOriginalTitle("Iron Man 3");
      options.setMetadata(md);

      List<MediaTrailer> trailers = mp.getTrailers(options);
      assertThat(trailers).isNotNull().isNotEmpty();

      MediaTrailer trailer = trailers.get(0);
      assertThat(trailer.getName()).isNotEmpty();
      assertThat(trailer.getUrl()).isNotEmpty();
      assertThat(trailer.getProvider()).isNotEmpty();
      assertThat(trailer.getQuality()).isNotEmpty();
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
