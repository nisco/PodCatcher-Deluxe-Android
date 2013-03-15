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

import android.app.FragmentTransaction;
import android.content.Intent;
import android.view.View;
import android.widget.BaseAdapter;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastLogoListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.fragments.EpisodeFragment;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Show list of episodes activity. This is thought of an abstract activity for
 * an app only consisting of an episode list view, the player and the ability to
 * show an {@link ShowEpisodeActivity} on top. Sub-classes could extends or
 * simply show this layout.
 */
public abstract class EpisodeListActivity extends EpisodeActivity implements
        OnLoadPodcastListener, OnLoadPodcastLogoListener, OnSelectEpisodeListener {

    /** The current episode list fragment */
    protected EpisodeListFragment episodeListFragment;

    /**
     * Key used to save the current setting for
     * <code>multiplePodcastsMode</code> in bundle
     */
    public static final String MODE_KEY = "MODE_KEY";

    /** The options available for the content mode */
    public enum ContentMode {
        /** Show single podcast */
        SINGLE_PODCAST,

        /** Show all podcast */
        ALL_PODCASTS,

        /** Show downloads */
        DOWNLOADS
    };

    /** Member to indicate which mode we are in */
    protected ContentMode contentMode = ContentMode.SINGLE_PODCAST;

    /** The podcast we are showing episodes for */
    protected Podcast currentPodcast;
    /** Key used to store podcast URL in intent or bundle */
    public static final String PODCAST_URL_KEY = "podcast_url";

    /** The current episode list */
    protected List<Episode> currentEpisodeList;

    @Override
    protected void findFragments() {
        super.findFragments();

        // The episode list fragment
        if (episodeListFragment == null)
            episodeListFragment = (EpisodeListFragment) findByTagId(R.string.episode_list_fragment_tag);

        // We have to do this here instead of onCreate since we can only react
        // on the call-backs properly once we have our fragment
        podcastManager.addLoadPodcastListener(this);
        podcastManager.addLoadPodcastLogoListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Make sure dividers (if any) reflect selection state
        updateDivider();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Persist state of episode metadata
        episodeManager.saveState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        podcastManager.removeLoadPodcastListener(this);
        podcastManager.removeLoadPodcastLogoListener(this);
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        if (contentMode.equals(ContentMode.SINGLE_PODCAST) && podcast.equals(currentPodcast))
            episodeListFragment.showProgress(progress);
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // Update list fragment to show episode list
        // Select all podcasts
        if (contentMode.equals(ContentMode.ALL_PODCASTS)) {
            // TODO decide on this: episodeList.addAll(list.subList(0,
            // list.size() > 100 ? 100 : list.size() - 1));
            if (podcast.getEpisodeNumber() > 0) {
                currentEpisodeList.addAll(podcast.getEpisodes());
                Collections.sort(currentEpisodeList);
                // Make sure this is a copy
                episodeListFragment.setEpisodeList(new ArrayList<Episode>(currentEpisodeList));
            }
        } // Select single podcast
        else if (contentMode.equals(ContentMode.SINGLE_PODCAST) && podcast.equals(currentPodcast)) {
            currentEpisodeList = podcast.getEpisodes();
            episodeListFragment.setEpisodeList(currentEpisodeList);
        }

        // Additionally, if on large device, process clever selection update
        if (viewMode == LARGE_LANDSCAPE_VIEW || viewMode == LARGE_PORTRAIT_VIEW) {
            if (currentEpisodeList != null && currentEpisodeList.contains(currentEpisode))
                episodeListFragment.select(currentEpisodeList.indexOf(currentEpisode));

            updateDivider();
        }

        updateActionBar();
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        // TODO What happens in multiple podcast mode?
        if (contentMode.equals(ContentMode.SINGLE_PODCAST) && failedPodcast.equals(currentPodcast))
            episodeListFragment.showLoadFailed();
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        // pass
    }

    @Override
    public void onPodcastLogoLoadFailed(Podcast podcast) {
        // pass
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        this.currentEpisode = selectedEpisode;

        switch (viewMode) {
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                // Set episode in episode fragment
                episodeFragment.setEpisode(selectedEpisode);
                updateDownloadStatus();

                break;
            case SMALL_LANDSCAPE_VIEW:
                // Find, and if not already done create, episode fragment
                if (episodeFragment == null)
                    episodeFragment = new EpisodeFragment();

                // Add the fragment to the UI, replacing the list fragment if it
                // is not already there
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.right, episodeFragment,
                            getString(R.string.episode_fragment_tag));
                    transaction.addToBackStack(null);
                    transaction.commit();
                }

                // Set the episode and update the UI
                episodeFragment.setEpisode(selectedEpisode);
                episodeFragment.setShowEpisodeDate(true);
                updateDownloadStatus();

                break;
            case SMALL_PORTRAIT_VIEW:
                // Send intent to open episode as a new activity
                Intent intent = new Intent(this, ShowEpisodeActivity.class);
                intent.putExtra(EPISODE_URL_KEY, selectedEpisode.getMediaUrl().toString());

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }

        // Make sure selection matches in list fragment
        if (currentEpisodeList != null)
            episodeListFragment.select(currentEpisodeList.indexOf(selectedEpisode));

        updatePlayer();
        updateDivider();
    }

    @Override
    public void onReturnToPlayingEpisode() {
        if (service != null && service.getCurrentEpisode() != null) {
            Episode playingEpisode = service.getCurrentEpisode();

            onEpisodeSelected(playingEpisode);
        }
    }

    @Override
    public void onShowDownload(String episodeUri) {
        Episode episode = podcastManager.findEpisodeForUrl(episodeUri);

        if (episode != null)
            onEpisodeSelected(episode);
    }

    @Override
    public void onNoEpisodeSelected() {
        this.currentEpisode = null;

        // If there is a episode fragment, reset it
        if (episodeListFragment != null)
            episodeListFragment.selectNone();

        updatePlayer();
        updateDivider();
    }

    @Override
    protected void updateDownloadStatus() {
        super.updateDownloadStatus();

        if (episodeListFragment != null && episodeListFragment.getListAdapter() != null)
            ((BaseAdapter) episodeListFragment.getListAdapter()).notifyDataSetChanged();
    }

    /**
     * Update the divider views to reflect current selection state.
     */
    protected void updateDivider() {
        colorDivider(R.id.divider_first,
                currentPodcast != null || !contentMode.equals(ContentMode.SINGLE_PODCAST));
        colorDivider(R.id.divider_second,
                currentEpisodeList != null && currentEpisodeList.indexOf(currentEpisode) >= 0);
    }

    private void colorDivider(int dividerViewId, boolean colorId) {
        if (getWindow() != null && getWindow().findViewById(dividerViewId) != null) {
            View divider = getWindow().findViewById(dividerViewId);
            divider.setBackgroundResource(colorId ? R.color.divider_on : R.color.divider_off);
        }
    }
}
