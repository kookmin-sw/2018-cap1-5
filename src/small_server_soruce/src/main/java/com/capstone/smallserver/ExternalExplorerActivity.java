package com.capstone.smallserver;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transfermanager.MultipleFileUpload;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/* 액티비티 설명: 서버 업로드 메뉴 클릭 시 나타나는 액티비티.
    핸드폰 external stroage로 접근하여 업로드 디렉토리 안의 파일들을 listView로 출력.     */


public class ExternalExplorerActivity extends AppCompatActivity {
    private TextView textView;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> items;
    private String rootPath = "";
    private String nextPath = "";
    private String currentPath = "";
    private TextView tempView;
    private String uploadTxt;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external);

        textView = (TextView) findViewById(R.id.textView);
        listView = (ListView) findViewById(R.id.listView);
        final Button uploadButton=(Button)findViewById(R.id.AWSuploadBtn);
        tempView=(TextView)findViewById(R.id.tempView);

        items = new ArrayList<>();
        listAdapter = new ArrayAdapter<String>(ExternalExplorerActivity.this,
                android.R.layout.simple_list_item_1, items);


        ///AWS 자격증명 얻는 코드////////////////////////////////////////////////////////////////////////
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:4d7fa6d0-b38a-4156-b02d-62aa17339c92", // 자격 증명 풀 ID
                Regions.US_EAST_1 // 리전
        );

        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
        s3.setRegion(Region.getRegion(Regions.AP_NORTHEAST_2));
        s3.setEndpoint("s3.ap-northeast-2.amazonaws.com");
        ///AWS 자격증명 얻는 코드 끝////////////////////////////////////////////////////////////////////////


        // 업로드를 위한 폴더 'smallServerUploadFolder'를 루트경로로 한다.
        // 만약에 사용자의 핸드폰에 해당 폴더가 없으면 생성한다.
        rootPath =  Environment.getExternalStorageDirectory().getAbsolutePath()+"/smallServerUploadFolder";

        try {
            makeDirectory(new File(rootPath));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }



        //listView initialize. 만약에 디렉토리 내에 파일이 하나도 없으면 false 리턴
        boolean result = Init(rootPath);

        if (result == false)
            return;

        listView.setAdapter(listAdapter);

        //터치했을 때 이벤트 제어 ( 눌렀을 경우 해당 파일의 디렉토리명을 uploadTxt를 에 할당)
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                currentPath = textView.getText().toString();
                String path = items.get(position).toString();

                uploadPath(path);
                uploadTxt=path;
            }
        });

        //업로드 버튼 눌렀을 경우 upload 시작하는 버튼 리스너
        uploadButton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {

                      new Thread() {
                            public  void run() {

                              TransferManager manager = new TransferManager(s3);
                              try
                              {
                                  MultipleFileUpload upload =  manager.uploadDirectory("s3abcd",uploadTxt,new File(rootPath+"/"+uploadTxt), true);
                                  upload.waitForCompletion();

                                }
                                catch (InterruptedException e)
                                {
                                }
                                manager.shutdownNow();

                            }
                        }.start();
                    }

                }
        );

    }


    ///////이하 메소드 선언부///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void makeDirectory(File targetLocation)
            throws IOException {

        if (!targetLocation.exists() && !targetLocation.mkdirs()) {
            throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
        }
    }

    public boolean Init(String rootPath) {

        File fileRoot = new File(rootPath);
        if (!fileRoot.isDirectory()) {
            Toast.makeText(ExternalExplorerActivity.this, "저장된 서버가 없습니다", Toast.LENGTH_SHORT).show();
            return false;
        }
        textView.setText(rootPath);

        // 파일 리스트 가져오기
        String[] fileList = fileRoot.list();
        if (fileList == null) {
            Toast.makeText(ExternalExplorerActivity.this, "Could not find List", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 아이템 리스트 전부 삭제
        items.clear();

        for (int i = 0; i < fileList.length; i++) {
            items.add(fileList[i]);
        }

        // 리스트 뷰에 적용
        listAdapter.notifyDataSetChanged();
        return true;
    }

    public void uploadPath(String str) {

        // 현재 경로에서 / 와 다음 경로 붙이기
        nextPath = currentPath + "/" + str;
        tempView.setText(str);

    }


}




