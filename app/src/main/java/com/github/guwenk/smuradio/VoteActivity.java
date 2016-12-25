package com.github.guwenk.smuradio;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;



public class VoteActivity extends AppCompatActivity {

    NodeList trackNodeList;
    ProgressDialog pDialog;
    List<Tracks> trackList = new ArrayList<>();
    ArrayList<String> names = new ArrayList<>();
    String filename;
    String choose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if (!new InternetChecker().hasConnection(getApplicationContext())){
            Toast.makeText(getApplicationContext(), "Проверьте подключение к интернету", Toast.LENGTH_LONG).show();
            finish();
        }
        new ParseXML().execute(); //для сети нужно добавить в execute параметр urlXML
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, names);
        listView.setAdapter(adapter);

        //<Поисковик>
        final EditText etFilter = (EditText)findViewById(R.id.filter);
        final Button btnFind = (Button)findViewById(R.id.btnApply);
        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                names.clear();
                String filter = etFilter.getText().toString();
                String trackName1, trackName2;
                for (int i = 0; i < trackList.size(); i++){
                    trackName1 = trackList.get(i).getArtist() + " " + trackList.get(i).getTitle();
                    trackName2 = trackList.get(i).getArtist() + " - " + trackList.get(i).getTitle();
                    if (trackName1.toUpperCase().contains(filter.toUpperCase()) || trackName2.toUpperCase().contains(filter.toUpperCase())){
                        names.add((i+1)+ ". " + trackList.get(i).getArtist() + " - " + trackList.get(i).getTitle());
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
        //<Поисковик/>



        //<Кнопка голосования>
        Button btnVote = (Button)findViewById(R.id.buttonVote);
        btnVote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    for (int i = 0; i < trackList.size(); i++) {
                        if (names.get(listView.getCheckedItemPosition()).toUpperCase().contains(trackList.get(i).getArtist().toUpperCase())&& names.get(listView.getCheckedItemPosition()).toUpperCase().contains(trackList.get(i).getTitle().toUpperCase())) {
                            filename = trackList.get(i).getFilename();
                            choose = trackList.get(i).getTitle();
                            //Log.d("***************************", listView.getCheckedItemPosition()+"; "+ names.get(listView.getCheckedItemPosition())+ "; "+ trackList.get(i).getArtist()+ " - "+ trackList.get(i).getTitle() + "; "+ trackList.get(i).getFilename());
                            break;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e){
                    filename = "";
                    choose = "";
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (!Objects.equals(filename, "") && filename != null) {
                        new VoteRequest().execute();
                        Toast.makeText(getApplicationContext(), "Выполнено: " + choose, Toast.LENGTH_SHORT).show();
                        finish();
                    } else{
                        Toast.makeText(getApplicationContext(), "Ничего не выбрано", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
        //<Кнопка голосования/>
    }


    //<Запрос на добавление трека>
    protected class VoteRequest extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... strings) {
            try {
                //Log.d("FilePath", filename);
                new URL("http://192.168.1.69:9001/?pass=yHZDVtGwCC&action=songrequest&filename=" + filename).openConnection().getInputStream(); // Отправка запроса
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    //Парсинг XML из сети
    protected class ParseXML extends AsyncTask<String, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(VoteActivity.this);
            pDialog.setTitle("Загрузка плейлиста");
            pDialog.setMessage("Загрузка....");
            pDialog.setIndeterminate(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(String... strings) { //это для сети - protected Void doInBackground(String... Url)
            try {
                InputStream stream = new URL("http://192.168.1.69:9001/?pass=yHZDVtGwCC&action=library&filename=Base").openConnection().getInputStream();
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document doc = documentBuilder.parse(new InputSource(stream));
                trackNodeList = doc.getElementsByTagName("Track");
            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
            }
            if (trackNodeList != null)
            for (int i = 0; i < trackNodeList.getLength(); i++){
                Element trackElement = (Element) trackNodeList.item(i);
                Tracks tracks = new Tracks();
                tracks.setNum(i+1);
                tracks.setArtist(trackElement.getAttribute("artist"));
                tracks.setTitle(trackElement.getAttribute("title"));
                tracks.setDuration(trackElement.getAttribute("duration"));
                tracks.setFilename(trackElement.getAttribute("filename"));
                names.add((i+1)+ ". " + trackElement.getAttribute("artist") + " - " + trackElement.getAttribute("title"));
                trackList.add(tracks);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pDialog.dismiss();
        }
    }
}
