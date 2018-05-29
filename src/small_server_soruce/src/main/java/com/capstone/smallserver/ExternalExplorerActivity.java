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

public class ExternalExplorerActivity extends AppCompatActivity {
    private TextView textView;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> items;
    private String rootPath = "";
    private String nextPath = "";
    private String prevPath = "";
    private String currentPath = "";
    private TextView tempView;
    private String uploadTxt;


    public void makeDirectory(File targetLocation)
            throws IOException {

        if (!targetLocation.exists() && !targetLocation.mkdirs()) {
            throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external);

        textView = (TextView) findViewById(R.id.textView);
        listView = (ListView) findViewById(R.id.listView);
        final Button uploadButton=(Button)findViewById(R.id.AWSuploadBtn);
        tempView=(TextView)findViewById(R.id.tempView);

        items = new ArrayList<>();
        listAdapter = new ArrayAdapter<String>(ExternalExplorerActivity.this,
                android.R.layout.simple_list_item_1, items);


        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:4d7fa6d0-b38a-4156-b02d-62aa17339c92", // 자격 증명 풀 ID
                Regions.US_EAST_1 // 리전
        );

        AmazonS3 s3=new AmazonS3Client(credentialsProvider);
        TransferUtility transferUtility=new TransferUtility(s3,getApplicationContext());

        s3.setRegion(Region.getRegion(Regions.AP_NORTHEAST_2));
        s3.setEndpoint("s3.ap-northeast-2.amazonaws.com");


        // 루트 경로 가져오기

        rootPath =  Environment.getExternalStorageDirectory().getAbsolutePath()+"/smallServerUploadFolder";

        try {
            makeDirectory(new File(rootPath));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        boolean result = Init(rootPath);

        if (result == false)
            return;

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                currentPath = textView.getText().toString();
                String path = items.get(position).toString();

                nextPath(path);
                uploadTxt=path;
            }
        });

        uploadButton.setOnClickListener(

                new Button.OnClickListener() {
                    public void onClick(View v) {

                        new Thread() {
                            public  void run() {

                                TransferManager manager = new TransferManager(s3);

                                try
                                {
                                    MultipleFileUpload upload =  manager.uploadDirectory("s3abcd",uploadTxt,new File(rootPath+"/"+uploadTxt), true);
//                                    download.addProgressListener(new ProgressListener() {
//                                        @Override
//                                        public void progressChanged(ProgressEvent progressEvent) {
//                                            double pct= progressEvent.getBytesTransferred() * 100.0/ progressEvent.getBytesTransferred();
//                                            //Log.d("percent",String.valueOf(pct));
//                                            Log.d("percent",download.getProgress().getPercentTransferred()+"%");
//                                        }
//                                    });
                                    upload.waitForCompletion();
//                                    Transfer.TransferState xfer_state=download.getState();
//                                    Log.d("state",xfer_state.toString());

                                }
                                catch (InterruptedException e)
                                {
                                }
                                manager.shutdownNow();

                                // copyDirectory(sourceFile, s3, null);
                            }
                        }.start();
                    }

                }
        );



    }

    public boolean Init(String rootPath) {
        // 파일 객체 생성
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


    public void nextPath(String str) {
        prevPath = currentPath;

        // 현재 경로에서 / 와 다음 경로 붙이기
        nextPath = currentPath + "/" + str;

        tempView.setText(str);
        //File[] files = new File(nextPath).listFiles();

        //String[] fileList = file.list();
        //items.clear();


        //for (int i = 0; i < fileList.length; i++) {
        //    items.add(fileList[i]);
        //}

        //textView.setText(nextPath);
        //listAdapter.notifyDataSetChanged();

    }
//
//    public void prevPath(String str){
//        nextPath=currentPath;
//        prevPath=currentPath;
//
//
//        // 마지막 / 의 위치 찾기
//        int lastSlashPosition=prevPath.lastIndexOf("/");
//
//        // 처음부터 마지막 / 까지의 문자열 가져오기
//        prevPath=prevPath.substring(0,lastSlashPosition);
//        File file=new File(prevPath);
//
//        if(file.isDirectory()==false){
//        Toast.makeText(ExplorerActivity.this,"Not Directory",Toast.LENGTH_SHORT).show();
//        return;
//        }
//
//        String[]fileList=file.list();
//        items.clear();
//        items.add("..");
//
//        for(int i=0;i<fileList.length;i++){
//        items.add(fileList[i]);
//        }
//
//        textView.setText(prevPath);
//        listAdapter.notifyDataSetChanged();
//        }


}


