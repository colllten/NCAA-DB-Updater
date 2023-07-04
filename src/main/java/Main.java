import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class Main {
    public static void main(String[] args) {
        //TODO: change to non-constant number
        String httpsURL = "https://api.collegefootballdata.com/teams/fbs?year=" + 2022;

        try {
            URL myURL = new URL(httpsURL);
            //Open connection to the URL
            System.out.println("Opening connection...");
            HttpsURLConnection conn = (HttpsURLConnection) myURL.openConnection();
            conn.setRequestProperty("Authorization", API_KEYS.CFDB_API);

            System.out.println("Opening streams...");
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            //Holds all teams in JSON Array
            String input = br.readLine();

            JSONParser parser = new JSONParser();
            //JSON array of all teams from CFDB
            JSONArray array = (JSONArray) parser.parse(input);

            //Iterate through every JSON team and store all B1G teams
            JSONArray bigTenTeams = new JSONArray();
            for (Object team : array) {
                JSONObject teamInfo = (JSONObject) team;
                if (teamInfo.get("conference").toString().equals("Big Ten")) {
                    System.out.printf("Adding %s%n", teamInfo.get("school").toString());
                    bigTenTeams.add(team);
                }
            }



        } catch (Exception e) { //TODO: Specify exception

        }
    }
}
