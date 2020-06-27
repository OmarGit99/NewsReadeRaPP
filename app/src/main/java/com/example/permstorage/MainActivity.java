package com.example.permstorage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    ListView listView;
    ArrayList<String> titlesArray;
    ArrayAdapter<String> arrayAdapter;
    ArrayList<String> urlsarray;
    ArrayList<String> contentarray;
    SQLiteDatabase sqLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StuffDownloader stuffDownloader = new StuffDownloader();
        JSONArray jsonArray =new JSONArray();
        ArrayList<String> urlsdownloaded = new ArrayList<String>();
        sqLiteDatabase = this.openOrCreateDatabase("HackerNews", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS hackernews(webid INTEGER PRIMARY KEY, title VARCHAR, url VARCHAR, content VARCHAR)");



        try {
            urlsdownloaded = stuffDownloader.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = sqLiteDatabase.rawQuery("SELECT * FROM hackernews", null);
                int idindex = c.getColumnIndex("webid");
                int titleindex = c.getColumnIndex("title");
                c.moveToFirst();
                while(c!=null){
                    Log.i("indexesofdb", c.getString(idindex));
                    Log.i("titlesofdb", c.getString(titleindex));
                    c.moveToNext();
                }



            }
        });




    }


    public class StuffDownloader extends AsyncTask<String, Void, ArrayList<String>>{

        @Override
        protected ArrayList<String> doInBackground(String... strings) {
            URL url;
            HttpURLConnection httpURLConnection;
            InputStreamReader inputStreamReader;
            InputStream inputStream;
            int data;
            String ids = "" ;
            try {
                url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection)url.openConnection();
                inputStream= httpURLConnection.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                data = inputStreamReader.read();
                while (data != -1){
                    char dat = (char) data;
                    ids +=dat;
                    data = inputStreamReader.read();
                }
                JSONArray jsonArray = new JSONArray(ids);
                JSONArray newjsonArray = new JSONArray();
                int limiter = 20;
                if(jsonArray.length() > 20){
                    for(int i = 0; i<= limiter; i++){
                        newjsonArray.put(jsonArray.get(i));
                    }
                }
                else {
                    for (int i = 0; i< jsonArray.length(); i++){
                        newjsonArray.put(jsonArray.get(i));

                    }
                }
                JSONArray webdetails = new JSONArray();

                for(int i = 0; i< newjsonArray.length(); i++) {
                    String webdata = "";
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+newjsonArray.getString(i)+".json?print=pretty");
                    httpURLConnection = (HttpURLConnection)url.openConnection();
                    inputStream = httpURLConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    data = inputStream.read();
                    while (data != -1) {
                        char x = (char) data;
                        webdata += x;
                        data = inputStream.read();
                    }
                    webdetails.put(webdata);
                }
                titlesArray = new ArrayList<String>();
                urlsarray = new ArrayList<String>();
                contentarray = new ArrayList<String>();

                arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, titlesArray);
                try {

                    for(int i =0; i< webdetails.length(); i++){
                        JSONObject jsonObject =  new JSONObject(webdetails.getString(i));
                        titlesArray.add(jsonObject.getString("title"));
                        arrayAdapter.notifyDataSetChanged();
                    }
                    for(int i =0; i< webdetails.length(); i++){
                        JSONObject jsonObject =  new JSONObject(webdetails.getString(i));
                        urlsarray.add(jsonObject.getString("url"));
                    }

                    sqLiteDatabase.execSQL("DELETE FROM hackernews");

                    for(int i =0; i< webdetails.length(); i++){
                        String result = "";
                        url = new URL(urlsarray.get(i));
                        httpURLConnection = (HttpURLConnection)url.openConnection();
                        inputStream = httpURLConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        while (data!= -1){
                            char dat = (char) data;
                            result += dat;
                            data = inputStream.read();
                        }
                        String sql = "INSERT INTO hackernews (title, url, content) VALUES( ? , ? , ? )";
                        SQLiteStatement statement = sqLiteDatabase.compileStatement(sql);
                        statement.bindString(1, titlesArray.get(i));
                        statement.bindString(2, urlsarray.get(i));
                        statement.bindString(3, result);
                        statement.execute();






                    }


                    Log.i("contentno", Integer.toString(contentarray.size()));

                } catch (JSONException e) {
                    e.printStackTrace();
                }



                return urlsarray;



            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;


        }

    }
}
