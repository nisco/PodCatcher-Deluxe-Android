/** Copyright 2012 Kevin Hausmann
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
package net.alliknow.podcatcher.services;

import java.io.IOException;

import net.alliknow.podcatcher.types.Episode;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Play an episode service
 * 
 * @author Kevin Hausmann
 */
public class PlayEpisodeService extends Service implements OnPreparedListener {

	/** Current episode */
	private Episode currentEpisode;
	/** Our MediaPlayer handle */
	private MediaPlayer player;
	/** Is the player preparing */
	private boolean preparing = false;
	
	/** Binder given to clients */
    private final IBinder binder = new PlayEpisodeBinder();
    
	public class PlayEpisodeBinder extends Binder {
		public PlayEpisodeService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayEpisodeService.this;
        }
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	/**
	 * @return Whether the player is currently playing
	 */
	public boolean isPlaying() {
		return this.player != null && this.player.isPlaying();
	}
	
	/**
	 * @return Whether the player is currently preparing
	 */
	public boolean isPreparing() {
		return this.preparing;
	}
	
	public void pause() {
		this.player.pause();
	}
	
	public void resume() {
		this.player.start();
	}
	
	public void playEpisode(Episode episode) {
		Log.d("Play Service", "Play called for " + episode + " (" + episode.getMediaUrl() + ")");
		this.currentEpisode = episode;
		
		if (isPlaying()) {
			this.player.stop();
			this.releasePlayer();
		}
		 
		try {
			this.initPlayer();
			this.player.setDataSource(episode.getMediaUrl().toExternalForm());
			this.player.prepareAsync(); // might take long! (for buffering, etc)
			this.preparing = true;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		this.preparing = false;
		this.player.start();
	}
	
	public Episode getCurrentEpisode() {
		return this.currentEpisode;
	}
	
	private void initPlayer() {
		this.player = new MediaPlayer();
		this.player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		this.player.setOnPreparedListener(this);
	}
	
	private void releasePlayer() {
		if (this.player != null) {
			this.player.release();
			this.player = null;
		}
	}
}