import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class Main {
    public static void main(String[] args) {
        //TODO: change to non-constant number
        //Get JSONArray containing all B1G teams
        JSONArray bigTenTeams = getBigTenTeams(2022);
        JSONObject team = (JSONObject) bigTenTeams.get(0);
        //Modify each team's JSON to only contain the necessary data
        bigTenTeams = modifyTeamsData(bigTenTeams);
    }

    /**
     * Uses CFDB API to get all B1G teams in JSON
     * @param year Roster year
     * @return JSONArray of B1G teams
     */
    public static JSONArray getBigTenTeams(int year) {
        String httpsURL = "https://api.collegefootballdata.com/teams/fbs?year=" + year;
        //connect to CFDB
        HttpsURLConnection connection = getApiConnection(httpsURL);
        //Create streams to read from CFDB
        BufferedReader br = getStreamReader(connection);
        //Get all B1G teams from CFDB
        return extractBigTenTeams(br);
    }

    /**
     * Connects to CFDB
     * @param httpsURL The URL to connect to
     * @return HttpsURLConnection that allows connection to CFDB
     */
    private static HttpsURLConnection getApiConnection(String httpsURL) {
        try {
            URL url = new URL(httpsURL);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", API_KEYS.CFDB_API);
            return connection;
        } catch (MalformedURLException e) {
            System.err.println("URL is malformed");
        } catch (IOException e) {
            System.err.println("Error creating connection to CFDB");
        }
        return null;
    }

    /**
     * Opens input stream to CFDB
     * @param connection Connection to read from
     * @return BufferedReader that can read from CFDB
     */
    private static BufferedReader getStreamReader(HttpsURLConnection connection) {
        try {
            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            return new BufferedReader(isr);
        } catch (IOException ex) {
            System.err.println("Error creating streams to read from CFDB");
        }
        return null;
    }

    /**
     * Gets all NCAA teams filters all out except B1G
     * @param br Database reader
     * @return JSONArray of B1G teams
     */
    private static JSONArray extractBigTenTeams(BufferedReader br) {
        JSONArray bigTenTeams = new JSONArray();
        try {
            //Gets all NCAA teams
            JSONArray ncaaTeams = (JSONArray) new JSONParser().parse(br.readLine());
            //Filter out all but B1G teams and put into own JSONArray
            for (Object team : ncaaTeams) {
                JSONObject jsonTeam = (JSONObject) team;
                if (jsonTeam.get("conference").toString().equals("Big Ten")) {
                    bigTenTeams.add(jsonTeam);
                }
            }
        } catch (IOException ex) {
            System.err.println("Error reading from CFDB while fetching all NCAA teams");
        } catch (ParseException e) {
            System.err.println("Error while parsing all NCAA teams JSON");
        }
        return bigTenTeams;
    }

    /**
     * CFDB has too much data about each team, so the function filters much of it out
     * @param teams JSON array of teams
     * @return Modified JSON array of teams with only necessary data
     */
    public static JSONArray modifyTeamsData(JSONArray teams) {
        JSONArray modifiedTeams = new JSONArray();
        for (int i = 0; i < teams.size(); i++) {
            JSONObject modifiedTeam = new JSONObject();
            //Retrieve only necessary data
            modifiedTeam.put("conference", ((JSONObject) teams.get(i)).get("conference").toString());
            modifiedTeam.put("color", ((JSONObject) teams.get(i)).get("color").toString());
            modifiedTeam.put("alt_color", ((JSONObject) teams.get(i)).get("alt_color").toString());
            modifiedTeam.put("abbreviation", ((JSONObject) teams.get(i)).get("abbreviation").toString());
            modifiedTeam.put("logos", ((JSONObject) teams.get(i)).get("logos"));
            modifiedTeam.put("color", ((JSONObject) teams.get(i)).get("color").toString());
            modifiedTeam.put("division", ((JSONObject) teams.get(i)).get("division").toString());
            modifiedTeam.put("school", ((JSONObject) teams.get(i)).get("school").toString());
            modifiedTeam.put("id", ((JSONObject) teams.get(i)).get("id").toString());
            modifiedTeams.add(modifiedTeam);
        }
        return modifiedTeams;
    }
}
