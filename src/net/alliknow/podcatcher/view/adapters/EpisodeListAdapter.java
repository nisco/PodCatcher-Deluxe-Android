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

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.types.Episode;

import java.util.Date;
import java.util.List;

/**
 * Adapter class used for the list of episodes.
 */
public class EpisodeListAdapter extends PodcatcherBaseListAdapter {

    /** The list our data resides in */
    protected List<Episode> list;
    /** Whether the podcast name should be shown */
    protected boolean showPodcastNames = false;

    /** String to use if no episode publication date available */
    private static final String NO_DATE = "---";
    /** Separator for date and podcast name */
    private static final String SEPARATOR = " - ";

    /**
     * Create new adapter.
     * 
     * @param context The activity.
     * @param episodeList The list of episodes to show in list.
     */
    public EpisodeListAdapter(Context context, List<Episode> episodeList) {
        super(context);

        this.list = episodeList;
    }

    /**
     * Set whether the podcast name for the episode should be shown. This will
     * redraw the list and take effect immediately.
     * 
     * @param show Whether to show each episode's podcast name.
     */
    public void setShowPodcastNames(boolean show) {
        this.showPodcastNames = show;

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

        // Find episode to represent
        final Episode episode = list.get(position);

        // Set the text to display for title
        setText(convertView, R.id.list_item_title, episode.getName());
        // Set the text to display as caption
        setText(convertView, R.id.list_item_caption, createCaption(episode));

        return convertView;
    }

    private String createCaption(Episode episode) {
        // This should not happen (but we cover it)
        if (episode.getPubDate() == null && !showPodcastNames)
            return NO_DATE;
        // Episode has no date, should not happen
        else if (episode.getPubDate() == null && showPodcastNames)
            return episode.getPodcastName();
        // This is the interesting case
        else {
            // Get a nice time span string for the age of the episode
            CharSequence date = DateUtils.getRelativeTimeSpanString(episode.getPubDate().getTime(),
                    new Date().getTime(), DateUtils.HOUR_IN_MILLIS);

            // Append podcast name
            if (showPodcastNames)
                return date + SEPARATOR + episode.getPodcastName();
            // Omit podcast name
            else
                return date.toString();
        }
    }
}