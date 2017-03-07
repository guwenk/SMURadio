package com.github.guwenk.smuradio;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;



public class OrderActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private NodeList trackNodeList;
    private List<Tracks> trackList = new ArrayList<>();
    private ArrayList<String> names = new ArrayList<>();
    private String filename;
    private String choose;
    private ArrayAdapter<String> adapter;
    private ImageView backgroundImage;
    SharedPreferences sp;
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mOrderRef = mRootRef.child("Requests").child("order");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        backgroundImage = (ImageView)findViewById(R.id.va_backgroundImage);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        final ListView listView = (ListView) findViewById(R.id.musicBaseView);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        new ParseXML().execute();
        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_single_choice, names);
        listView.setAdapter(adapter);

        //<Кнопка голосования>
        Button btnVote = (Button)findViewById(R.id.buttonOrder);
        btnVote.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                try {
                    StringBuilder sb = new StringBuilder(names.get(listView.getCheckedItemPosition()));
                    while (true){
                        if (sb.charAt(0) != '.'){
                            sb.deleteCharAt(0);
                        }else {
                            sb.deleteCharAt(0);
                            sb.deleteCharAt(0);
                            //Log.d("MusicName", sb.toString());
                            break;
                        }
                    }
                    for (int i = 0; i < trackList.size(); i++) {
                        if (Objects.equals(sb.toString(), trackList.get(i).getArtist() + " - " + trackList.get(i).getTitle())) {
                            filename = trackList.get(i).getFilename();
                            choose = trackList.get(i).getTitle();
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e){
                    filename = "";
                    choose = "";
                }

                if (!Objects.equals(filename, "") && filename != null) {
                    mOrderRef.setValue(filename);
                    Toast.makeText(getApplicationContext(), getString(R.string.done) + choose, Toast.LENGTH_SHORT).show();
                    finish();
                } else{
                    Toast.makeText(getApplicationContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
                }
            }
        });
        //<Кнопка голосования/>
    }

    //Парсинг XML из сети
    private class ParseXML extends AsyncTask<String, Void, Void>{
        boolean caughtException = false;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                URL url = new URL("https://firebasestorage.googleapis.com/v0/b/someradio-4bfa5.appspot.com/o/Base.xml?alt=media&token=c43976d4-7528-4120-a8ee-d5a945d7267f");
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                InputStream stream = connection.getInputStream();
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document doc = documentBuilder.parse(new InputSource(stream));
                trackNodeList = doc.getElementsByTagName("Track");
            } catch (SocketTimeoutException e){
                caughtException = true;
            } catch (SAXException | ParserConfigurationException | IOException e) {
                e.printStackTrace();
            }
            if (trackNodeList != null) {
                ArrayList<String> names_mem = new ArrayList<>();
                for (int i = 0; i < trackNodeList.getLength(); i++) {
                    Element trackElement = (Element) trackNodeList.item(i);
                    Tracks tracks = new Tracks();
                    tracks.setArtist(trackElement.getAttribute("artist"));
                    tracks.setTitle(trackElement.getAttribute("title"));
                    tracks.setFilename(trackElement.getAttribute("filename"));
                    names_mem.add(trackElement.getAttribute("artist") + " - " + trackElement.getAttribute("title"));
                    trackList.add(tracks);
                }
                Collections.sort(names_mem, new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s1.compareToIgnoreCase(s2);
                    }
                });
                for (int i = 0; i < names_mem.size(); i++){
                    names.add((i + 1) + ". " + names_mem.get(i));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!caughtException && names.size()>0) {
                ProgressBar pb = (ProgressBar)findViewById(R.id.progressBar_orderMenu);
                pb.setVisibility(View.INVISIBLE);
                findViewById(R.id.musicBaseView).setVisibility(View.VISIBLE);
                findViewById(R.id.oa_line).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonOrder).setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getApplicationContext(), R.string.unable_to_connect_to_server, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        String path = sp.getString("backgroundPath", "");
        Bitmap backgroundBitmap;
        if (path.equals("")){
            backgroundImage.setImageResource(R.drawable.main_background);
        } else {
            backgroundBitmap = new FileManager(getApplicationContext()).loadBitmap(path, "background");
            backgroundImage.setImageBitmap(backgroundBitmap);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String filter) {
        onQueryTextChange(filter);
        return false;
    }

    @Override
    public boolean onQueryTextChange(final String filter) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<String> stringArrayList = new ArrayList<>();
                stringArrayList.clear();
                String trackName1, trackName2;
                for (int j = 0; j < trackList.size(); j++){
                    trackName1 = trackList.get(j).getArtist() + " " + trackList.get(j).getTitle();
                    trackName2 = trackList.get(j).getArtist() + " - " + trackList.get(j).getTitle();
                    if (trackName1.toUpperCase().contains(filter.toUpperCase()) || trackName2.toUpperCase().contains(filter.toUpperCase())){
                        stringArrayList.add((j+1)+ ". " + trackList.get(j).getArtist() + " - " + trackList.get(j).getTitle());
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        names.clear();
                        names.addAll(stringArrayList);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
        return false;
    }
}
