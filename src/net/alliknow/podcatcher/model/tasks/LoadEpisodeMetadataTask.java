/** Copyright 2012-2014 Kevin Hausmann
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

package net.alliknow.podcatcher.model.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import net.alliknow.podcatcher.listeners.OnLoadEpisodeMetadataListener;
import net.alliknow.podcatcher.model.EpisodeDownloadManager;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.tags.METADATA;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;
import net.alliknow.podcatcher.model.types.Progress;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Load the episode metadata from the file system.
 */
public class LoadEpisodeMetadataTask extends
        AsyncTask<Void, Progress, Map<String, EpisodeMetadata>> {

    /** Our context */
    private Context context;
    /** The listener callback */
    private OnLoadEpisodeMetadataListener listener;

    /**
     * Create new task.
     * 
     * @param context Context to read file from (not <code>null</code>).
     * @param listener Callback to be alerted on completion. Could be
     *            <code>null</code>, but then nobody would ever know that this
     *            task finished.
     * @see EpisodeManager#METADATA_FILENAME
     */
    public LoadEpisodeMetadataTask(Context context, OnLoadEpisodeMetadataListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected Map<String, EpisodeMetadata> doInBackground(Void... params) {
        // Create resulting data structure and file stream
        Map<String, EpisodeMetadata> result = new ConcurrentHashMap<>();
        InputStream fileStream = null;

        try {
            // 1. Build parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            // Create the parser to use
            XmlPullParser parser = factory.newPullParser();

            // 2. Open default podcast file
            fileStream = context.openFileInput(EpisodeManager.METADATA_FILENAME);
            parser.setInput(fileStream, StoreFileTask.FILE_ENCODING);

            // 3. Parse the OPML file
            int eventType = parser.next();

            // Read complete document
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // We only need start tags here
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();

                    // Metadata found
                    if (tagName.equalsIgnoreCase(METADATA.METADATA)) {
                        final String key = parser.getAttributeValue(null, METADATA.EPISODE_URL);
                        final EpisodeMetadata metadata = readMetadata(parser);

                        result.put(key, metadata);
                    }

                }

                // Done, get next parsing event
                eventType = parser.next();
            }

            // 4. Do some house keeping since file availability might have
            // changed
            cleanMetadata(result);
        } catch (Exception e) {
            // Pass, metadata might be empty, that's okay
        } finally {
            // Make sure we close the file stream
            if (fileStream != null)
                try {
                    fileStream.close();
                } catch (IOException e) {
                    // Nothing we can do here
                }
        }

        return result;
    }

    @Override
    protected void onPostExecute(Map<String, EpisodeMetadata> result) {
        if (listener != null)
            listener.onEpisodeMetadataLoaded(result);
    }

    private EpisodeMetadata readMetadata(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        // Create the resulting metadata record
        EpisodeMetadata result = new EpisodeMetadata();

        // Parse the metadata information
        int eventType = parser.next();

        // Read till the end of the metadata tag is reached
        while (!(eventType == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase(
                METADATA.METADATA))) {

            // We only need start tags here
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                // Metadata detail found
                if (tagName.equalsIgnoreCase(METADATA.EPISODE_NAME))
                    result.episodeName = parser.nextText();
                else if (tagName.equalsIgnoreCase(METADATA.EPISODE_DATE))
                    result.episodePubDate = new Date(Long.parseLong(parser.nextText()));
                else if (tagName.equalsIgnoreCase(METADATA.EPISODE_DESCRIPTION))
                    result.episodeDescription = parser.nextText();
                else if (tagName.equalsIgnoreCase(METADATA.PODCAST_NAME))
                    result.podcastName = parser.nextText();
                else if (tagName.equalsIgnoreCase(METADATA.PODCAST_URL))
                    result.podcastUrl = parser.nextText();
                else if (tagName.equalsIgnoreCase(METADATA.DOWNLOAD_ID))
                    result.downloadId = Long.parseLong(parser.nextText());
                else if (tagName.equalsIgnoreCase(METADATA.LOCAL_FILE_PATH))
                    result.filePath = parser.nextText();
            }

            // Done, get next parsing event
            eventType = parser.next();
        }

        return result;
    }

    private void cleanMetadata(Map<String, EpisodeMetadata> result) {
        // Find download folder
        File podcastDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);

        // Handle the case where the download finished while the application was
        // not running. In this case, there would be a downloadId but no
        // filePath while the episode media file is actually there.
        Iterator<Entry<String, EpisodeMetadata>> iterator = result.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, EpisodeMetadata> entry = iterator.next();
            // Skip all entries without a download id
            if (entry.getValue().downloadId == null)
                continue;

            final File downloadPath = new File(podcastDir,
                    EpisodeDownloadManager.sanitizeAsFilePath(entry.getValue().podcastName,
                            entry.getValue().episodeName, entry.getKey()));

            if (entry.getValue().filePath == null && downloadPath.exists())
                entry.getValue().filePath = downloadPath.getAbsolutePath();
        }

        // Handle the case that the media file has been delete from outside the
        // app. In this case, downloadId and and filePath would be there, but no
        // file.
        iterator = result.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, EpisodeMetadata> entry = iterator.next();
            // Skip all entries without a download id
            if (entry.getValue().downloadId == null)
                continue;

            // Invalidate file path and download id data
            if (entry.getValue().filePath != null && !new File(entry.getValue().filePath).exists()) {
                entry.getValue().downloadId = null;
                entry.getValue().filePath = null;
            }
        }
    }
}
