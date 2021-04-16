package nl.utwente.sekhmet.api;

import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.ut.UTOAuth2User;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.Date;

@RestController
@RequestMapping(value = "/api")
public class RestErrorApiController {


    @PostMapping(value = "/error")
    public ResponseEntity<String> postError(@RequestBody String json,
                                            @AuthenticationPrincipal SupernaturalAuthUser principal) {

        long uid = principal.getUser().getId();
        //no security required, but requiring login is nice
        try {
            JSONObject jo = new JSONObject(json);
            Long time = jo.getLong("timestamp");
            String error = jo.getString("error");
            String extra = jo.getString("extra");
            logError(time,error,extra,uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok("error logged");
    }

    // internal error endpoint
    public static void logError(long time,String error,String extra) {
        logError(time, error, extra, 0);
    }
    public static void logError(long time,String error,String extra,long uid) {
        try {
            File file = new File("log/errorLogs.log");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            String res = "ERROR";
            res += "\n\tTimestamp\t: " + time;
            res += "\n\tperson_id\t: " + uid;
            res += "\n\tError\t\t: " + error;
            res += "\n\tExtra\t\t: " + extra;
            res += "\n";

            writer.write(res);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @GetMapping("/error")
    public ResponseEntity<String> getErrors(@AuthenticationPrincipal SupernaturalAuthUser principal) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("../logs/errorLogs.log"));
            String[] res = new String[10000];
            String temp = "";
            int i = 0;
            while ( (temp = br.readLine()) != null) {
                res[i] = temp;
                i = (i + 1) % 10000;
            }
            StringBuilder sb = new StringBuilder();
            int b = i;
            while (b != (i-1)%10000) {
                sb.append(res[b]).append("\n");
            }
            return new ResponseEntity<>(sb.toString(), HttpStatus.OK);
        } catch (IOException e) {
            return  new ResponseEntity<>("error while getting errors: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
