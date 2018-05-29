package com.capstone.smallserver;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.MultipleFileDownload;
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/* 액티비티 설명: 서버마켓 메뉴 클릭 시 나타나는 액티비티.
    AWS에서 목록을 받아와서 listView로 출력.          */


public class AWSExplorerActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<String> items;
    private ArrayList<String> desc;
    private ArrayList<Uri> imgid;
    private List<String> a;
    private Uri uri;


    //다운로드 상태 표시를 위한 핸들러
    private Handler handler = new Handler();
    private ProgressDialog progressDialog;

    CustomListView customListView;
    AmazonS3 s3;


    // 다운로드 완료 후 UI에 Toast 메시지 출력하는 핸들러
    private Handler downAlarmHandler = new Handler() {

        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), "다운로드 완료!", Toast.LENGTH_SHORT).show();
            super.handleMessage(msg);
        }
    };


    //AWS S3 버켓에서 폴더 이름을 받아오는 메소드
    public List<String> listKeysInDirectory(String bucketName, AmazonS3 s3) {
        String delimiter = "/";

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucketName)
                .withDelimiter(delimiter);
        ObjectListing objects = s3.listObjects(listObjectsRequest);
        return objects.getCommonPrefixes();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);

        items = new ArrayList<>();
        desc = new ArrayList<>();
        imgid = new ArrayList<>();
        listView = (ListView) findViewById(R.id.listView);
        customListView = new CustomListView(this, items, desc, imgid);
        progressDialog = new ProgressDialog(AWSExplorerActivity.this);


        ///AWS 자격증명 얻는 코드////////////////////////////////////////////////////////////////////////
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:4d7fa6d0-b38a-4156-b02d-62aa17339c92", // 자격 증명 풀 ID
                Regions.US_EAST_1 // 리전
        );

        s3 = new AmazonS3Client(credentialsProvider);
        s3.setRegion(Region.getRegion(Regions.AP_NORTHEAST_2));
        s3.setEndpoint("s3.ap-northeast-2.amazonaws.com");
        ///AWS 자격증명 얻는 코드 끝////////////////////////////////////////////////////////////////////////


        /// AWS S3 버켓리스트를 listView로 셋팅하기 위해 UI 제어하는 핸들러
        
        final Handler uihandler=new Handler()
        {
            public void handleMessage(Message msg){

                customListView.notifyDataSetChanged();
            }
        };


        Init(s3,uihandler);


        listView.setAdapter(customListView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String svrname = items.get(position).toString();

                new Thread()
                {
                    public void run()
                    {

                        downPath(svrname, s3 );
                    }
                }.start();

            }
        });
    }

    public void Init(AmazonS3 s3, Handler handler3) {

        // 파일 리스트 가져오기
        new Thread()
        {
            public void run()
            {

                a=listKeysInDirectory("s3abcd",s3);
                // Log.d("list",a.toString());

               // Log.d("list",a.toString());
                if (a == null) {
                    Toast.makeText(AWSExplorerActivity.this, "Could not find List", Toast.LENGTH_SHORT).show();
                }

                items.clear();
                Iterator iterator = a.iterator();
                while (iterator.hasNext()) {
                    String name=(String) iterator.next();
                    items.add(name.replace("/",""));
                    imgid.add(uri);
                    desc.add("서버");
                }
                Message premessage=handler3.obtainMessage();
                 handler3.sendMessage(premessage);

            }

        }.start();


    }





    public void downPath(String str, AmazonS3 s3) {


        //The path where we expect the node project to be at runtime.
        String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";

        Log.d("log",str);
        handler.post(new Runnable() {
            public void run() {
                progressDialog.setTitle("Downloading");
                progressDialog.setMessage("Please Wait...");
                progressDialog.setCancelable(false);
                progressDialog.show();

            }
        });
        TransferManager manager = new TransferManager(s3);
        MultipleFileDownload download =  manager.downloadDirectory("s3abcd", str, new File(nodeDir));
        try
        {


            download.addProgressListener(new ProgressListener() {
                @Override
                public void progressChanged(ProgressEvent progressEvent) {
                    double pct= progressEvent.getBytesTransferred() * 100.0/ progressEvent.getBytesTransferred();

                    Log.d("percent",String.valueOf(pct));
                    Log.d("percent",download.getProgress().getPercentTransferred()+"%");

                }
            });
            download.waitForCompletion();
            Transfer.TransferState xfer_state=download.getState();
            Log.d("state",xfer_state.toString());
            progressDialog.cancel();
            if(xfer_state.toString().equals("Completed"))
            downAlarmHandler.sendEmptyMessage(0);



        }
        catch (InterruptedException e)
        {
        }
        manager.shutdownNow();

            }

    @Override
    public void onBackPressed() {

        Intent mainIntent=new Intent(AWSExplorerActivity.this, MainActivity.class);
                       startActivity(mainIntent);
    }

}



