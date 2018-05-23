package com.capstone.smallserver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class ExplorerActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> items;
    private ArrayList<String> desc;
    private ArrayList<Uri> imgid;


    CustomListView customListView;

    private String rootPath = "";
    private String nextPath = "";
    private String currentPath = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
       // textView = (TextView) findViewById(R.id.textView);

        items = new ArrayList<>();
        desc= new ArrayList<>();
        imgid= new ArrayList<>();
        listView = (ListView) findViewById(R.id.listView);
        customListView=new CustomListView(this,items,desc,imgid);


//        listAdapter = new ArrayAdapter<String>(ExplorerActivity.this,
//                android.R.layout.simple_list_item_1, items);

        // 루트 경로 가져오기
        rootPath =  getApplicationContext().getFilesDir().getAbsolutePath()+"/nodejs-project";
        boolean result = Init(rootPath);

        if (result == false)
            return;

        listView.setAdapter(customListView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                currentPath = rootPath;
                String path = items.get(position).toString();

                nextPath(path);
            }
        });
    }

    public boolean Init(String rootPath) {
        // 파일 객체 생성
        File fileRoot = new File(rootPath);
        if (!fileRoot.isDirectory()) {
            Toast.makeText(ExplorerActivity.this, "저장된 서버가 없습니다", Toast.LENGTH_SHORT).show();
            return false;
        }
        //textView.setText(rootPath);

        // 파일 리스트 가져오기
        String[] fileList = fileRoot.list();
        if (fileList == null) {
            Toast.makeText(ExplorerActivity.this, "Could not find List", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 아이템 리스트 전부 삭제
        items.clear();

        for (int i = 0; i < fileList.length; i++) {
            items.add(fileList[i]);

            try {
                Uri uri = Uri.parse(getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project/" + fileList[i] + "/icon.png");

                imgid.add(uri);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            desc.add("서버");
        }


        // 리스트 뷰에 적용
        //listAdapter.notifyDataSetChanged();
        customListView.notifyDataSetChanged();

        return true;
    }


    public void nextPath(String str) {
        //prevPath = currentPath;

        // 현재 경로에서 / 와 다음 경로 붙이기
        nextPath = currentPath + "/" + str;


        File[] files = new File(nextPath).listFiles();

        for(File file:files) {

            //setup.txt 파일을 찾아 안에 입력된 경로를 불러와 mainActivity에 전달.

            if (file.getName().equals("setup.txt")) {
                StringBuilder text=new StringBuilder();

                try {

                    BufferedReader buffer=new BufferedReader(new FileReader(file.getCanonicalFile()));
                    String line="";

                    while ((line=buffer.readLine()) != null) {
                        text.append(line);
                    }
                    Log.d("tag",text.toString());

                    buffer.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                    Log.d("error","에러!");
                }

                //mainActivity에 result값 전달.

                Intent intent2=new Intent();
                intent2.putExtra("result", nextPath+text.toString());
                intent2.putExtra("serverName",str);
                intent2.putExtra("doorImg","icon.png");
                setResult(1234,intent2);
                finish();


            }
        }


    }

}



