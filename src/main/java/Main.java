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


        } catch (Exception e) { //TODO: Specify exception

        }
    }
}
