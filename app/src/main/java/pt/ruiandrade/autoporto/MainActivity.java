package pt.ruiandrade.autoporto;

import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends WearableActivity {

    private static final String BASE_STCP_URL = "https://www.stcp.pt/pt/itinerarium/soapclient.php?codigo=";

    private TextView txtClock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Wearable_Modal);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtClock = (TextView) findViewById(R.id.txtClock);

        final Handler mHandler = new Handler(getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                txtClock.setText(new SimpleDateFormat("HH:mm").format(new Date()));
                mHandler.postDelayed(this, 1000);
            }
        }, 10);

        final EditText txtParagem = (EditText) findViewById(R.id.txtParagem);
        final TextView txtOutput = (TextView) findViewById(R.id.txtOutput);
        final ProgressBar prgSpinner = (ProgressBar) findViewById(R.id.prgSpinner);
        final ImageButton btnSearch = (ImageButton) findViewById(R.id.btnSearch);
        final ImageView imgFail = (ImageView) findViewById(R.id.imgFail);
        final TextView txtFail = (TextView) findViewById(R.id.txtFail);

        prgSpinner.setVisibility(View.INVISIBLE);
        imgFail.setVisibility(View.INVISIBLE);
        txtFail.setVisibility(View.INVISIBLE);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prgSpinner.setVisibility(View.VISIBLE);
                prgSpinner.setIndeterminate(true);
                imgFail.setVisibility(View.INVISIBLE);
                txtFail.setVisibility(View.INVISIBLE);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            try {
                                URL url = new URL(BASE_STCP_URL + txtParagem.getText().toString());
                                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                                String tmp;
                                String html = "";
                                while ((tmp = in.readLine()) != null) {
                                    html += tmp;
                                }
                                in.close();
                                Document busPage = Jsoup.parseBodyFragment(html);
                                final Elements tableRows = busPage.getElementsByTag("tr");
                                /*
                                <tr class="even">
                                 <td>
                                  <ul class="linhasAssoc">
                                   <li><a target="_self" class="linha_703" title="" href="/pt/viajar/linhas/?linha=703 ">703 </a></li>
                                  </ul> &nbsp;CORDOARIA -</td>
                                 <td><i>19:29</i></td>
                                 <td>3min</td>
                                </tr>
                                 */
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        prgSpinner.setVisibility(View.INVISIBLE);
                                        imgFail.setVisibility(View.INVISIBLE);
                                        txtFail.setVisibility(View.INVISIBLE);
                                        txtOutput.setText("");
                                        for (Element row : tableRows) {
                                            if (row.classNames().contains("even")) {
                                                String   busLine = row.select("td > ul > li > a").first().text();
                                                String   busTime = row.select("td > i").first().text();
                                                String   sanitizedRow = busLine + " - " + busTime;
                                                txtOutput.append(sanitizedRow + "\n");
                                            }
                                        }
                                    }
                                });
                            } catch (MalformedURLException e) {
                                Log.e("AutoPorto", e.toString());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        prgSpinner.setVisibility(View.INVISIBLE);
                                        imgFail.setVisibility(View.VISIBLE);
                                        txtFail.setVisibility(View.VISIBLE);
                                    }
                                });
                            } catch (IOException e) {
                                Log.e("AutoPorto", e.toString());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        prgSpinner.setVisibility(View.INVISIBLE);
                                        imgFail.setVisibility(View.VISIBLE);
                                        txtFail.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    prgSpinner.setVisibility(View.INVISIBLE);
                                    imgFail.setVisibility(View.VISIBLE);
                                    txtFail.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                });
                thread.start();
            }
        });
        // Enables Always-on
        setAmbientEnabled();
    }

    private String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
        return sdf.format(new Date(System.currentTimeMillis()));
    }
}
