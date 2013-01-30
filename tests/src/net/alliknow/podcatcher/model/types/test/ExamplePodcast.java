package net.alliknow.podcatcher.model.types.test;

import java.net.MalformedURLException;
import java.net.URL;

public enum ExamplePodcast {
	SCAMSCHOOL("http://revision3.com/scamschool/feed/MP4-Large"),
	RADIOLAB("http://feeds.wnyc.org/radiolab"),
	THISAMERICANLIFE("http://feeds.thisamericanlife.org/talpodcast"),
	LINUXOUTLAWS("http://feeds.feedburner.com/linuxoutlaws"),
	NASAEDGE("http://www.nasa.gov/rss/NASAcast_vodcast.rss"),
	MAUS("http://podcast.wdr.de/maus.xml"),
	DAILYBACON("http://downloads.bbc.co.uk/podcasts/fivelive/dailybacon/rss.xml"),
	GREENCAST("http://www.greenpeace-berlin.de/fileadmin/podcast/greencast.xml"),
	NERDIST("http://nerdist.libsyn.com/rss"),
	INTUNE("http://downloads.bbc.co.uk/podcasts/radio3/r3intune/rss.xml"),
	RICHEISEN("http://richeisen.libsyn.com/rss"),
	GEO("http://www.geo.de/GEOaudio/index.xml"),
	UHHYEAHDUDE("http://feeds.feedburner.com/UhhYeahDude/podcast"),
	NEO("http://www.zdf.de/ZDFmediathek/podcast/1446344?view=podcast"),
	ANSTALT("http://www.zdf.de/ZDFmediathek/podcast/222630?view=podcast"),
	BAUERFEIND("http://www.3sat.de/bauerfeind/podcast/bauerfeind_feed.xml");
		
	private String url;
	
	private ExamplePodcast(String url) {
		this.url = url;
	}
	
	public URL getURL() {
		try {
			return new URL(this.url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getFunnyName() {
		return this + "\"" + " " + "\r\n\t" + "\'" + "'";
	}
}
