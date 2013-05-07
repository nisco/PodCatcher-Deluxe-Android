/** Copyright 2012, 2013 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package net.alliknow.podcatcher;

import android.os.Bundle;
import android.view.MenuItem;

import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

/**
 * Activity to show only the episode list and possibly the player. Used in small
 * portrait view mode only.
 */
public class ShowEpisodeListActivity extends EpisodeListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if we need this activity at all
        if (!viewMode.isSmallPortrait())
            finish();
        else {
            // Set the content view
            setContentView(R.layout.main);
            // Set fragment members
            findFragments();

            // During initial setup, plug in the episode list fragment.
            if (savedInstanceState == null && episodeListFragment == null) {
                episodeListFragment = new EpisodeListFragment();
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.content, episodeListFragment,
                                getString(R.string.episode_list_fragment_tag))
                        .commit();
            }

            // Get the load mode
            selection.setMode((ContentMode) getIntent().getSerializableExtra(MODE_KEY));
            // Get URL of podcast to load
            String podcastUrl = getIntent().getExtras().getString(PODCAST_URL_KEY);
            Podcast selectedPodcast = podcastManager.findPodcastForUrl(podcastUrl);

            // Act accordingly
            if (selection.isAllMode())
                onAllPodcastsSelected();
            else if (selectedPodcast != null)
                onPodcastSelected(selectedPodcast);
            else
                episodeListFragment.showLoadFailed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                finish();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        super.onPodcastSelected(podcast);

        // Init the list view...
        episodeListFragment.resetAndSpin();
        // ...and start loading
        podcastManager.load(podcast);
    }

    @Override
    public void onAllPodcastsSelected() {
        super.onAllPodcastsSelected();

        // Init the list view...
        episodeListFragment.resetAndSpin();
        episodeListFragment.setShowPodcastNames(true);
        // ...and go get the data
        for (Podcast podcast : podcastManager.getPodcastList())
            podcastManager.load(podcast);

        updateActionBar();
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        super.onPodcastLoaded(podcast);

        updateActionBar();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        super.onPodcastLoadFailed(failedPodcast);

        updateActionBar();
    }

    @Override
    protected void updateActionBar() {
        // Single podcast selected
        if (selection.getPodcast() != null) {
            getActionBar().setTitle(selection.getPodcast().getName());

            if (selection.getPodcast().getEpisodes().isEmpty())
                getActionBar().setSubtitle(null);
            else {
                int episodeCount = selection.getPodcast().getEpisodeNumber();
                getActionBar().setSubtitle(
                        episodeCount == 1 ? getString(R.string.one_episode) :
                                episodeCount + " " + getString(R.string.episodes));
            }
        } // Multiple podcast mode
        else if (selection.isAllMode()) {
            getActionBar().setTitle(R.string.app_name);

            updateActionBarSubtitleOnMultipleLoad();
        } else
            getActionBar().setTitle(R.string.app_name);

        // Enable navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Set the action bar subtitle to reflect multiple podcast load progress
     */
    private void updateActionBarSubtitleOnMultipleLoad() {
        if (podcastManager.getPodcastList() != null) {
            final int podcastCount = podcastManager.size();
            final int loadingPodcastCount = podcastManager.getLoadCount();

            final String onePodcast = getString(R.string.one_podcast_selected);
            final String morePodcasts = getString(R.string.podcasts_selected);
            final String of = getString(R.string.of);

            if (loadingPodcastCount == 0) {
                getActionBar().setSubtitle(podcastCount == 1 ?
                        onePodcast : podcastCount + " " + morePodcasts);
            } else
                getActionBar().setSubtitle(
                        (podcastCount - loadingPodcastCount) + " "
                                + of + " " + podcastCount + " " + morePodcasts);
        }
    }

    @Override
    protected void updatePlayer() {
        super.updatePlayer();

        // Make sure to show episode title in player
        if (playerFragment != null) {
            playerFragment.setLoadMenuItemVisibility(false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
}
