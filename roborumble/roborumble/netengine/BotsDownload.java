package roborumble.netengine;


import java.net.*;
import java.util.*;
import java.util.Vector;
import java.util.jar.*;
import java.util.zip.*;
import java.io.*;
import robocode.util.*;
import roborumble.battlesengine.*;


/**
 * BotsDownload - a class by Albert Perez
 * Manages the download operations (participants and JAR files)
 * Controlled by properties files
 */

public class BotsDownload {

	private String internetrepository;
	private String botsrepository;
	private String participantsfile;
	private String participantsurl;
	private String tempdir;
	private String tag;
	private String isteams;
	private String sizesfile;
	private CompetitionsSelector size;
	private String ratingsurl;
	private String generalbots;
	private String minibots;
	private String microbots;
	private String nanobots;
	private String generalbotsfile;
	private String minibotsfile;
	private String microbotsfile;
	private String nanobotsfile;
	private String removeboturl;
	
	public BotsDownload(String propertiesfile) {
		// Read parameters
		Properties parameters = null;

		try {
			parameters = new Properties();
			parameters.load(new FileInputStream(propertiesfile));
		} catch (Exception e) {
			System.out.println("Parameters File not found !!!");
		}
		internetrepository = parameters.getProperty("BOTSURL", "");
		botsrepository = parameters.getProperty("BOTSREP", "");
		isteams = parameters.getProperty("TEAMS", "NOT");
		participantsurl = parameters.getProperty("PARTICIPANTSURL", "");
		participantsfile = parameters.getProperty("PARTICIPANTSFILE", "");
		tag = parameters.getProperty("STARTAG", "pre");
		tempdir = parameters.getProperty("TEMP", "");
		// Code size
		sizesfile = parameters.getProperty("CODESIZEFILE", "");
		size = new CompetitionsSelector(sizesfile, botsrepository);
		// Ratings files
		ratingsurl = parameters.getProperty("RATINGS.URL", "");
		generalbots = propertiesfile;
		while (generalbots.indexOf("/") != -1) {
			generalbots = generalbots.substring(generalbots.indexOf("/") + 1);
		}
		generalbots = generalbots.substring(0, generalbots.indexOf("."));
		minibots = parameters.getProperty("MINIBOTS", "");
		microbots = parameters.getProperty("MICROBOTS", "");
		nanobots = parameters.getProperty("NANOBOTS", "");
		generalbotsfile = parameters.getProperty("RATINGS.GENERAL", "");
		minibotsfile = parameters.getProperty("RATINGS.MINIBOTS", "");
		microbotsfile = parameters.getProperty("RATINGS.MICROBOTS", "");
		nanobotsfile = parameters.getProperty("RATINGS.NANOBOTS", "");
		// remove old bots
		removeboturl = parameters.getProperty("UPDATEBOTSURL", "");
	}

	public boolean downloadRatings() {
		// delete previous files
		if (!generalbotsfile.equals("")) {
			(new File(generalbotsfile)).delete();
		}
		if (!minibotsfile.equals("")) {
			(new File(minibotsfile)).delete();
		}
		if (!microbotsfile.equals("")) {
			(new File(microbotsfile)).delete();
		}
		if (!nanobotsfile.equals("")) {
			(new File(nanobotsfile)).delete();
		}
		// download new ones
		if (ratingsurl.equals("")) {
			return false;
		}
		boolean downloaded = true;

		if (!generalbots.equals("") && !generalbotsfile.equals("")) {
			downloaded = downloadRatingsFile(generalbots, generalbotsfile) & downloaded;
		}
		if (!minibots.equals("") && !minibotsfile.equals("")) {
			downloaded = downloadRatingsFile(minibots, minibotsfile) & downloaded;
		}
		if (!microbots.equals("") && !microbotsfile.equals("")) {
			downloaded = downloadRatingsFile(microbots, microbotsfile) & downloaded;
		}
		if (!nanobots.equals("") && !nanobotsfile.equals("")) {
			downloaded = downloadRatingsFile(nanobots, nanobotsfile) & downloaded;
		}
		return downloaded;
	}

	public boolean downloadParticipantsList() {
		String begin = "<" + tag + ">";
		String end = "</" + tag + ">";
		Vector bots = new Vector();

		try {
			URL url = new URL(participantsurl);

			HttpURLConnection urlc = (HttpURLConnection) url.openConnection();

			urlc.setRequestMethod("GET");
			urlc.setDoInput(true);
			urlc.connect();

			boolean arebots = false;
			// BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
			String str;

			while ((str = in.readLine()) != null) {
				if (str.indexOf(begin) != -1) {
					arebots = true;
				} else if (str.indexOf(end) != -1) {
					arebots = false;
				} else if (arebots) {
					bots.add(str);
				}
			}
			in.close();
			urlc.disconnect();

			PrintStream outtxt = new PrintStream(new BufferedOutputStream(new FileOutputStream(participantsfile)), false);  

			for (int i = 0; i < bots.size(); i++) {
				outtxt.println((String) bots.get(i));
			}
			outtxt.close();			

		} catch (Exception e) { 
			System.out.println("Unable to retrieve participants list"); 
			System.out.println(e);
			return false; 
		}
		return true;	
	}
	
	public boolean downloadMissingBots() {
		Vector jars = new Vector();
		Vector ids = new Vector();
		Vector names = new Vector();

		// Read participants
		try {
			FileReader fr = new FileReader(participantsfile); 
			BufferedReader br = new BufferedReader(fr);
			String record = new String();

			while ((record = br.readLine()) != null) { 
				if (record.indexOf(",") >= 0) {
					String id = record.substring(record.indexOf(",") + 1);
					String name = record.substring(0, record.indexOf(","));
					String jar = name.replace(' ', '_') + ".jar";

					jars.add(jar);
					ids.add(id);
					names.add(name);
				}
			}
			br.close();
		} catch (Exception e) { 
			System.out.println("Participants file not found ... Aborting"); 
			System.out.println(e);
			return false; 
		}
		// check if the file exists in the repository and download if not present
		for (int i = 0; i < jars.size(); i++) {
			String botjar = (String) jars.get(i);
			String botid = (String) ids.get(i);
			String botname = (String) names.get(i);
			String botpath = botsrepository + botjar;
			boolean exists = (new File(botpath)).exists();

			if (!exists) { 
				// System.out.println("Going to download ..."+botname);
				boolean downloaded = downloadBot(botname, botjar, botid, botsrepository, tempdir);

				if (!downloaded) {
					System.out.println("Could not download bot " + botjar);
				}
			}
		}
		return true;
	}

	public void updateCodeSize() {
		if (!sizesfile.equals("")) {
			try {
				FileReader fr = new FileReader(participantsfile); 
				BufferedReader br = new BufferedReader(fr);
				String record = new String();

				while ((record = br.readLine()) != null) { 
					String id = record.substring(record.indexOf(",") + 1);
					String name = record.substring(0, record.indexOf(","));

					name = name.replace(' ', '_');
					size.CheckCompetitorsForSize(name, name, 1500);
				}
				br.close();
			} catch (Exception e) { 
				System.out.println("Battles input file not found ... Aborting"); 
				System.out.println(e);
				return; 
			}
			
		}
		
	}
	
	private boolean downloadBot(String botname, String file, String id, String destination, String tempdir) {
		String filed = tempdir + file;
		String finald = destination + file;

		// check if the bot exists in the repository

		boolean exists = (new File(finald)).exists();

		if (exists) { 
			System.out.println("The bot already exists in the repository.");
			return false;
		}

		// Download the bot

		FileTransfer filetransfer = new FileTransfer(1000);
		
		URL url = null;

		try { 
			if (id.indexOf("://") == -1) {
				url = new URL("http://www.robocoderepository.com/Controller.jsp?submitAction=downloadClass&id=" + id);
			} else {
				url = new URL(id);
			}
		} catch (Exception e) {
			System.out.println("Wrong URL");
			return false;
		}

		System.out.println("Downloading ..." + botname);	
		boolean downloaded = filetransfer.download(url, new File(filed));

		if (!downloaded) {
			System.out.println("Unable to download " + botname + " from site.");
			return false;
		}

		// Check the bot and save it into the repository
			
		if (checkJarFile(filed, botname)) {
			try {
				Utils.copy(new File(filed), new File(finald));
			} catch (Exception e) {
				System.out.println("Unable to copy " + filed + " into the repository");
				return false;
			}
			System.out.println("Downloaded " + botname + " into " + finald);
		} else {
			System.out.println("Downloaded file is wrong or corrupted:" + file);
			return false;
		} 
	
		return true;
	}

	private boolean checkJarFile(String file, String botname) {
		if (botname.indexOf(" ") == -1) {
			System.out.println("Are you sure " + botname + " is a bot/team? Can't download it."); 
			return false;
		}		

		String bot = botname.substring(0, botname.indexOf(" ")); 

		bot = bot.replace('.', '/'); 
		if (!isteams.equals("YES")) {
			bot = bot + ".properties";
		} else {
			bot = bot + ".team";
		}

		try {
			JarFile jarf = new JarFile(file);
			ZipEntry zipe = jarf.getJarEntry(bot);

			if (zipe == null) {
				System.out.println("Not able to read properties");
				return false;
			}
			InputStream properties = jarf.getInputStream(zipe);

			Properties parameters = null;

			parameters = new Properties();
			parameters.load(properties); 
			
			if (!isteams.equals("YES")) {
				String classname = parameters.getProperty("robot.classname", "");
				String version = parameters.getProperty("robot.version", "");

				if (botname.equals(classname + " " + version)) {
					return true;
				} else {
					return false;
				}
			} else {
				String version = parameters.getProperty("team.version", "");

				if (botname.equals(botname.substring(0, botname.indexOf(" ")) + " " + version)) {
					return true;
				} else {
					return false;
				}				
			}
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}
	}

	// ----------------------------------------------------------------------------------
	// download ratings file
	// ----------------------------------------------------------------------------------
	private boolean downloadRatingsFile(String competition, String file) {

		try {
			URL url = new URL(ratingsurl + "?version=1&game=" + competition);

			HttpURLConnection urlc = (HttpURLConnection) url.openConnection();

			urlc.setRequestMethod("GET");
			urlc.setDoInput(true);
			urlc.connect();

			PrintStream outtxt = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)), false);  

			boolean arebots = false;
			BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
			String str;

			while ((str = in.readLine()) != null) {
				outtxt.println(str);
			}
			in.close();
			urlc.disconnect();

			outtxt.close();			

		} catch (Exception e) { 
			System.out.println("Unable to ratings for " + competition); 
			System.out.println(e);
			return false; 
		}
		return true;
	}

	// ----------------------------------------------------------------------------------
	// download ratings file
	// ----------------------------------------------------------------------------------

	public boolean notifyServerForOldParticipants() {
		// Load participants names
		Hashtable namesall = new Hashtable();

		try {
			FileReader fr = new FileReader(participantsfile); 
			BufferedReader br = new BufferedReader(fr);
			String record = new String();

			while ((record = br.readLine()) != null) { 
				if (record.indexOf(",") != -1) {
					String name = record.substring(0, record.indexOf(","));

					name = name.replace(' ', '_');
					namesall.put(name, name);
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Participants file not found when removing old participants ... Aborting");
			System.out.println(e);
			return false;
		}
		
		// Load ratings files
		Properties generalratings = new Properties();
		Properties miniratings = new Properties();
		Properties microratings = new Properties();
		Properties nanoratings = new Properties();

		try {
			generalratings.load(new FileInputStream(generalbotsfile));
		} catch (Exception e) {
			generalratings = null;
		}
		try {
			miniratings.load(new FileInputStream(minibotsfile));
		} catch (Exception e) {
			miniratings = null;
		}
		try {
			microratings.load(new FileInputStream(microbotsfile));
		} catch (Exception e) {
			microratings = null;
		}
		try {
			nanoratings.load(new FileInputStream(nanobotsfile));
		} catch (Exception e) {
			nanoratings = null;
		}
		
		// Check general ratings	
		if (generalratings == null) {
			return false;
		}
		for (Enumeration e = generalratings.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement(); 

			if (!namesall.containsKey(key)) {
				// remove the key from the ratings file
				System.out.println("Removing entry ... " + key + " from " + generalbots);
				removebot(generalbots, key);
			}
		}
		// Check mini ratings	
		if (miniratings == null) {
			return true;
		}
		for (Enumeration e = miniratings.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement(); 

			if (!namesall.containsKey(key)) {
				// remove the key from the ratings file
				System.out.println("Removing entry ... " + key + " from " + minibots);
				removebot(minibots, key);
			}
		}
		
		// Check micro ratings
		if (microratings == null) {
			return true;
		}
		for (Enumeration e = microratings.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement(); 

			if (!namesall.containsKey(key)) {
				// remove the key from the ratings file
				System.out.println("Removing entry ... " + key + " from " + microbots);
				removebot(microbots, key);
			}
		}
		
		// Check nano ratings
		if (nanoratings == null) {
			return true;
		}
		for (Enumeration e = nanoratings.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement(); 

			if (!namesall.containsKey(key)) {
				// remove the key from the ratings file
				System.out.println("Removing entry ... " + key + " from " + nanobots);
				removebot(nanobots, key);
			}
		}
	
		return true;
	}
	
	private void removebot(String game, String bot) {
		if (removeboturl.equals("")) {
			System.out.println("UPDATEBOTS URL not defined!");
			return;
		}
		
		String data = "version=1&game=" + game + "&name=" + bot.trim() + "&dummy=NA";
		
		// System.out.println("Sending to "+removeboturl+" ... "+data+".");
		
		try {
			// Send data
			URL url = new URL(removeboturl);
			URLConnection conn = url.openConnection();

			conn.setDoOutput(true);
			PrintWriter wr = new PrintWriter(new OutputStreamWriter(conn.getOutputStream()));

			wr.println(data);
			wr.flush();
			
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;

			while ((line = rd.readLine()) != null) {
				System.out.println(line);
			}
				
			wr.close();
			rd.close();					
			
		} catch (Exception e) {
			System.out.println(e);
		}
				
	}
	
}
