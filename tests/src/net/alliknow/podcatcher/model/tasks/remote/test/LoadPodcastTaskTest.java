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

package net.alliknow.podcatcher.model.tasks.remote.test;

import android.test.InstrumentationTestCase;
import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadPodcastListener;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask;
import net.alliknow.podcatcher.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import net.alliknow.podcatcher.model.test.Utils;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("javadoc")
public class LoadPodcastTaskTest extends InstrumentationTestCase {

    private CountDownLatch signal = null;

    private List<Podcast> examplePodcasts;

    private class MockPodcastLoader implements OnLoadPodcastListener {

        Podcast result;
        boolean failed;
        PodcastLoadError code;

        @Override
        public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        }

        @Override
        public void onPodcastLoaded(Podcast podcast) {
            this.result = podcast;
            this.failed = false;

            signal.countDown();
        }

        @Override
        public void onPodcastLoadFailed(Podcast podcast, PodcastLoadError code) {
            this.result = podcast;
            this.failed = true;
            this.code = code;

            signal.countDown();
        }
    }

    @Override
    protected void setUp() throws Exception {
        Log.d(Utils.TEST_STATUS, "Set up test \"LoadPodcast\" by loading example podcasts...");

        final Date start = new Date();
        examplePodcasts = Utils.getExamplePodcasts(getInstrumentation().getTargetContext());

        Log.d(Utils.TEST_STATUS, "Waited " + (new Date().getTime() - start.getTime())
                + "ms for example podcasts...");
    }

    public final void testLoadExamplePodcasts() {
        final MockPodcastLoader mockLoader = new MockPodcastLoader();

        int size = examplePodcasts.size();
        int index = 0;
        int failed = 0;

        Log.d(Utils.TEST_STATUS, "Testing " + size + " example podcasts");

        // Actual example podcasts
        Iterator<Podcast> podcasts = examplePodcasts.iterator();
        while (podcasts.hasNext()) {
            Podcast ep = podcasts.next();
            Log.d(Utils.TEST_STATUS, "---- New Podcast (" + ++index + "/" + size +
                    ") ----");
            Log.d(Utils.TEST_STATUS, "Testing \"" + ep + "\"...");
            LoadPodcastTask task = loadAndWait(mockLoader, new Podcast(ep.getName(),
                    ep.getUrl()));

            if (mockLoader.failed) {
                Log.w(Utils.TEST_STATUS, "Podcast " + ep.getName() + " failed!");
                assertNull(mockLoader.result.getLastLoaded());

                failed++;
            } else {
                assertFalse(task.isCancelled());
                assertNotNull(mockLoader.result);
                assertFalse(mockLoader.result.getEpisodes().isEmpty());
                assertNotNull(mockLoader.result.getLastLoaded());

                Log.d(Utils.TEST_STATUS, "Tested \"" + mockLoader.result +
                        "\" - okay...");
            }

            // Discard the complete podcast because otherwise
            // the memory would fill up quickly...
            podcasts.remove();
        }

        Log.d(Utils.TEST_STATUS, "Tested all example podcast, failed on " +
                failed);
    }

    public final void testLoadDummyPodcasts() throws MalformedURLException {
        final MockPodcastLoader mockLoader = new MockPodcastLoader();

        // null
        loadAndWait(mockLoader, (Podcast) null);
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result);
        assertEquals(mockLoader.code, PodcastLoadError.UNKNOWN);

        // null URL
        loadAndWait(mockLoader, new Podcast(null, null));
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
        assertEquals(mockLoader.code, PodcastLoadError.UNKNOWN);

        // bad URL
        loadAndWait(mockLoader, new Podcast("Mist", "http://bla"));
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
        assertEquals(mockLoader.code, PodcastLoadError.NOT_REACHABLE);

        // no podcast feed URL
        loadAndWait(mockLoader, new Podcast("Google", "http://www.google.com"));
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
        assertEquals(mockLoader.code, PodcastLoadError.NOT_PARSEABLE);

        // Auth required
        loadAndWait(mockLoader, new Podcast("SGU", "https://www.theskepticsguide.org/premium"));
        assertTrue(mockLoader.failed);
        assertNull(mockLoader.result.getLastLoaded());
        assertEquals(mockLoader.code, PodcastLoadError.AUTH_REQUIRED);
    }

    private LoadPodcastTask loadAndWait(final MockPodcastLoader mockLoader, final Podcast podcast) {
        // Create task and latch
        final LoadPodcastTask task = new LoadPodcastTask(mockLoader);
        signal = new CountDownLatch(1);

        // Go load podcast
        final Date start = new Date();
        task.execute(podcast);

        // Wait for the podcast to load
        try {
            signal.await();
        } catch (InterruptedException e) {
            Log.e(Utils.TEST_STATUS, "Interrupted while waiting for podcast " + podcast.getName());
        }

        Log.d(Utils.TEST_STATUS, "Waited " + (new Date().getTime() - start.getTime())
                + "ms for Podcast \"" + podcast + "\"...");

        return task;
    }
}
