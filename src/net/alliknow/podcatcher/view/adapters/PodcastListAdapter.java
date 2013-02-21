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

package net.alliknow.podcatcher.view.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.view.HorizontalProgressView;

import java.util.List;

/**
 * Adapter class used for the list of podcasts.
 */
public class PodcastListAdapter extends PodcatcherBaseListAdapter {

    /** The list our data resides in */
    protected List<Podcast> list;
    /** Member flag to indicate whether we show the podcast logo */
    protected boolean showLogoView = false;

    /** String resources used: one episode */
    protected final String oneEpisode;
    /** String resources used: multiple episodes */
    protected final String episodes;

    /**
     * Create new adapter.
     * 
     * @param context The current context.
     * @param podcastList List of podcasts to wrap (not <code>null</code>).
     */
    public PodcastListAdapter(Context context, List<Podcast> podcastList) {
        super(context);

        this.list = podcastList;
        oneEpisode = context.getResources().getString(R.string.one_episode);
        episodes = context.getResources().getString(R.string.episodes);
    }

    /**
     * Set whether the podcast logo should be shown. This will redraw the list
     * and take effect immediately.
     * 
     * @param show Whether to show each podcast's logo.
     */
    public void setShowLogo(boolean show) {
        this.showLogoView = show;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the return view (possibly recycle a used one)
        convertView = findReturnView(convertView, parent, R.layout.list_item);

        // Set list item color background
        setBackgroundColorForPosition(convertView, position);

        // Find podcast to represent
        final Podcast podcast = list.get(position);
        final int episodeNumber = podcast.getEpisodeNumber();

        // Set the text to display for title
        setText(convertView, R.id.list_item_title, podcast.getName());
        // Set the text to display as caption
        TextView captionView = (TextView) convertView.findViewById(R.id.list_item_caption);
        captionView.setText(createCaption(episodeNumber));
        captionView.setVisibility(episodeNumber != 0 ? VISIBLE : GONE);

        // Set the podcast logo if available
        boolean show = showLogoView && podcast.getLogo() != null;

        ImageView logoView = (ImageView) convertView.findViewById(R.id.podcast_logo);
        logoView.setVisibility(show ? VISIBLE : GONE);
        logoView.setImageBitmap(show ? podcast.getLogo() : null);

        // Show progress on select all podcasts?
        HorizontalProgressView progressView = (HorizontalProgressView)
                convertView.findViewById(R.id.list_item_progress);
        progressView.setVisibility(PodcastManager.getInstance().isLoading(podcast)
                && selectAll ? VISIBLE : GONE);

        return convertView;
    }

    private String createCaption(int numberOfEpisodes) {
        return numberOfEpisodes == 1 ? oneEpisode : numberOfEpisodes + " " + episodes;
    }
}
