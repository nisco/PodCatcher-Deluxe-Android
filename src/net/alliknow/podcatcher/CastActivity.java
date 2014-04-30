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

package net.alliknow.podcatcher;

import static com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
import static com.google.android.gms.cast.CastMediaControlIntent.categoryForCast;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import net.alliknow.podcatcher.model.types.Episode;

import java.io.IOException;

/**
 * Podcatcher cast activity. Implements all the details needed to support Google
 * Cast.
 */
public abstract class CastActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        RemoteMediaPlayer.OnStatusUpdatedListener, RemoteMediaPlayer.OnMetadataUpdatedListener {

    protected boolean casting = false;
    private boolean castPlaying = false;
    protected RemoteMediaPlayer castPlayer = new RemoteMediaPlayer();

    private static final String APP_ID = DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
    private static final String CAST_CATEGORY = categoryForCast(APP_ID);
    private static final String TAG = "CAST";

    protected MediaRouter mediaRouter;
    protected MediaRouteSelector mediaRouteSelector;
    protected CastDevice selectedDevice;
    protected GoogleApiClient apiClient;
    protected MediaRouteButton mediaRouteButton;

    private MediaRouter.Callback mediaRouterCallback = new MediaRouter.Callback() {

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            selectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();
            Log.d(TAG, "Route selected");
            Log.d(TAG, "Device: " + selectedDevice.getFriendlyName());
            Log.d(TAG, "Route ID: " + routeId);

            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(selectedDevice, new CastClientListener());

            apiClient = new GoogleApiClient.Builder(CastActivity.this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(CastActivity.this)
                    .addOnConnectionFailedListener(CastActivity.this)
                    .build();
            apiClient.connect();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            // teardown();
            Log.d(TAG, "Route unselected, stopped casting");
            selectedDevice = null;
            casting = false;
        }
    };

    private class CastClientListener extends Cast.Listener {

        @Override
        public void onApplicationStatusChanged() {
            super.onApplicationStatusChanged();

            Log.d(TAG, "App status changed");
        }

        @Override
        public void onApplicationDisconnected(int statusCode) {
            super.onApplicationDisconnected(statusCode);

            Log.d(TAG, "App disconnect");
        }

        @Override
        public void onVolumeChanged() {
            super.onVolumeChanged();

            Log.d(TAG, "Volume changed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create media router and selector
        this.mediaRouter = MediaRouter.getInstance(getApplicationContext());
        this.mediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CAST_CATEGORY).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Connect route selector and menu item
        final MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        final MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        castPlayer.setOnStatusUpdatedListener(this);
        castPlayer.setOnMetadataUpdatedListener(this);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "App connected");
        try {
            Cast.CastApi.launchApplication(apiClient, APP_ID, false)
                    .setResultCallback(
                            new ResultCallback<Cast.ApplicationConnectionResult>() {
                                @Override
                                public void onResult(Cast.ApplicationConnectionResult result) {
                                    Status status = result.getStatus();
                                    if (status.isSuccess()) {
                                        ApplicationMetadata applicationMetadata =
                                                result.getApplicationMetadata();
                                        String sessionId = result.getSessionId();
                                        String applicationStatus = result.getApplicationStatus();
                                        boolean wasLaunched = result.getWasLaunched();
                                        casting = true;
                                        Log.d(TAG, "Application launched, casting is on");
                                    } else {
                                        // teardown();
                                        casting = false;
                                    }
                                }
                            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to launch application", e);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Connection failed");
        casting = false;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection suspended");
        casting = false;
    }

    @Override
    public void onStatusUpdated() {
        Log.d(TAG, "Remote media player status updated");
    }

    @Override
    public void onMetadataUpdated() {
        Log.d(TAG, "Remote media player metadata updated");
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mediaRouter.removeCallback(mediaRouterCallback);
            castPlayer.setOnStatusUpdatedListener(null);
            castPlayer.setOnMetadataUpdatedListener(null);
        }

        super.onPause();
    }

    protected void play(Episode episode) {
        try {
            Cast.CastApi.setMessageReceivedCallbacks(apiClient,
                    castPlayer.getNamespace(), castPlayer);
            castPlayer.requestStatus(apiClient)
                    .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(MediaChannelResult result) {
                            if (!result.getStatus().isSuccess()) {
                                Log.e(TAG, "Failed to request status.");
                            }
                        }
                    });

            MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, episode.getName());
            mediaMetadata.putString(MediaMetadata.KEY_ARTIST, episode.getPodcast().getName());
            MediaInfo mediaInfo = new MediaInfo.Builder(episode.getMediaUrl())
                    .setContentType(
                            episode.getMediaType() == null ? "audio/mp3" : episode.getMediaType())
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(mediaMetadata)
                    .build();
            castPlayer.load(apiClient, mediaInfo, true)
                    .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(MediaChannelResult result) {
                            if (result.getStatus().isSuccess()) {
                                Log.d(TAG, "Media loaded successfully");
                                castPlaying = true;
                            } else {
                                Log.d(TAG, "Media playback failed");
                                castPlaying = false;
                            }
                        }
                    });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Problem occurred with media during loading", e);
        } catch (IOException e) {
            Log.e(TAG, "Exception while creating media channel", e);
        } catch (Exception e) {
            Log.e(TAG, "Problem opening media during loading", e);
        }
    }

    protected void togglePlay() {
        if (castPlaying)
            castPlayer.pause(apiClient);
        else
            castPlayer.play(apiClient);

        this.castPlaying = !castPlaying;
    }
}
