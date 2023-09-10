import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Year;
import java.util.List;

import com.google.auth.oauth2.GoogleCredentials;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class Main {
    static Firestore db;
    public static void main(String[] args) {
        //TODO: Implement logging
        //TODO: Improve exception handling
        //TODO: Add GUIs

        //Initialize instance of Cloud Firestore
        // Use a service account
        try {
            //Open stream to Firebase
            InputStream serviceAccount = new FileInputStream("/Users/coltenglover/Desktop/NCAA-Fantasy-Football" +
                    "/Database/ncaa-fantasy-football-391902-32591822272e.json");
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error reading service account file while connecting to Firestore",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error creating stream connecting to Firebase",
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }

        //Options of years to update from
        Integer[] years = new Integer[4];
        for (int year = Year.now().getValue() - 2; year < Year.now().getValue() + 2; year++) {
            years[year - Year.now().getValue() + 2] = year;
        }

        //Set the year to update
        int yearIndex = -1;
        while (yearIndex <= 0 || yearIndex >= 3) {
            yearIndex = JOptionPane.showOptionDialog(null, "Choose what to update", "Update Options",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, years, 0);
        }

        //TODO: Split updating teams or players
        String[] updateChoices = {"Teams", "Players"};
        int choice = JOptionPane.showOptionDialog(null, "Choose entity to update", "Update Entity",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, updateChoices, 0);
        System.out.println(choice);
        if (choice == 0) {
            getBigTenTeams(years[yearIndex]);
        } else if (choice == 1) { //Players
            //Get all offensive and special teams players in each B1G team
            //Iterate over each B1G in Firestore DB
            // asynchronously retrieve multiple documents
            ApiFuture<QuerySnapshot> future = db.collection("B1G-Teams").get();
            // future.get() blocks on response
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                for (DocumentSnapshot document : documents) {
                    //Each document is a team... request the roster from CFDB for each team
                    BufferedReader br = getStreamReader(getApiConnection("https://api.collegefootballdata.com" +
                            "/roster?team=" + document.get("school") + "&year=" + years[yearIndex]));
                    JSONArray roster = (JSONArray) new JSONParser().parse(br.readLine());
                    for (int i = 0; i < roster.size(); i++) {
                        JSONObject player = (JSONObject) roster.get(i);
                        if (player.get("position").equals("DB") || player.get("position").equals("DL") || player.get(
                                "position").equals("LB") || player.get("position").equals("PK") || player.get(
                                        "position").equals("LS") || player.get("position").equals("P")) {
                            continue;
                        }
                        //Remove unnecessary fields
                        player.remove("home_city");
                        player.remove("home_state");
                        player.remove("home_country");
                        player.remove("home_latitude");
                        player.remove("home_longitude");
                        player.remove("home_county_fips");
                        player.remove("recruit_ids");

                        System.out.printf("Writing %s %s to DB\n", player.get("first_name").toString(), player.get(
                                "last_name").toString());
                        //write to Firestore DB
                        DocumentReference docRef = db.collection("B1G-Offensive-Players")
                                .document(player.get("id").toString());
                        //asynchronously write data
                        docRef.set(player);
                    }
                }
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

        }

        JOptionPane.showMessageDialog(null, "Firebase writing complete");
    }

    static void updateBigTenTeams(int year) {
        //Get JSONArray containing all B1G teams
        JSONArray bigTenTeams = getBigTenTeams(year);
        if (bigTenTeams == null) {
            JOptionPane.showMessageDialog(null, "Error retrieving teams from CFDB", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        //Modify each team's JSON to only contain the necessary data
        bigTenTeams = modifyTeamsData(bigTenTeams);

        System.out.println("Writing all teams to Firestore...");
        //Write each team to the database
        for (int i = 0; i < bigTenTeams.size(); i++) {
            DocumentReference docRef = db.collection("B1G-Teams")
                    .document(((JSONObject) bigTenTeams.get(i)).get("id").toString());
            //asynchronously write data
            docRef.set((JSONObject) bigTenTeams.get(i));
        }
    }

    /**
     * Uses CFDB API to get all B1G teams in JSON
     * @param year Roster year
     * @return JSONArray of B1G teams
     */
    public static JSONArray getBigTenTeams(int year) {
        //URL to pull from
        String httpsURL = "https://api.collegefootballdata.com/teams/fbs?year=" + year;
        //connect to CFDB
        HttpsURLConnection connection = getApiConnection(httpsURL);
        if (connection == null) {
            JOptionPane.showMessageDialog(null, "Error creating connection to CFDB", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
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
            JOptionPane.showMessageDialog(null, "API URL is malformed", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error creating connection to CFDB", "Error", JOptionPane.ERROR_MESSAGE);
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
        for (Object team : teams) {
            JSONObject modifiedTeam = new JSONObject();
            //Retrieve only necessary data
            modifiedTeam.put("conference", ((JSONObject) team).get("conference").toString());
            modifiedTeam.put("color", ((JSONObject) team).get("color").toString());
            modifiedTeam.put("alt_color", ((JSONObject) team).get("alt_color").toString());
            modifiedTeam.put("abbreviation", ((JSONObject) team).get("abbreviation").toString());
            modifiedTeam.put("logos", ((JSONObject) team).get("logos"));
            modifiedTeam.put("color", ((JSONObject) team).get("color").toString());
            modifiedTeam.put("division", ((JSONObject) team).get("division").toString());
            modifiedTeam.put("school", ((JSONObject) team).get("school").toString());
            modifiedTeam.put("id", ((JSONObject) team).get("id").toString());
            modifiedTeams.add(modifiedTeam);
        }
        return modifiedTeams;
    }
}
