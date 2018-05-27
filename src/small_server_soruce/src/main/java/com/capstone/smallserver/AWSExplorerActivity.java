package com.capstone.smallserver;

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

public class AWSExplorerActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> items;
    private ArrayList<String> desc;
    private ArrayList<Uri> imgid;

    private List<String> a;

    CustomListView customListView;

    private String rootPath = "";
    private String nextPath = "";
    private String currentPath = "";

    AmazonS3 s3;

    private static boolean result = false;


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
        // textView = (TextView) findViewById(R.id.textView);

        items = new ArrayList<>();
        desc = new ArrayList<>();
        imgid = new ArrayList<>();
        listView = (ListView) findViewById(R.id.listView);
        customListView = new CustomListView(this, items, desc, imgid);


        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:4d7fa6d0-b38a-4156-b02d-62aa17339c92", // 자격 증명 풀 ID
                Regions.US_EAST_1 // 리전
        );

        s3 = new AmazonS3Client(credentialsProvider);

        s3.setRegion(Region.getRegion(Regions.AP_NORTHEAST_2));
        s3.setEndpoint("s3.ap-northeast-2.amazonaws.com");


        final Handler handler3=new Handler()
        {
            public void handleMessage(Message msg){

                customListView.notifyDataSetChanged();
            }
        };


        Init(s3,handler3);

//        if (result == false)
//            return;


//        for (int i = 0; i < a.size(); i++) {
//            items.add(a.[i]);

//            try {
//                //Uri uri = Uri.parse(getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project/" + fileList[i] + "/icon.png");
//
//                //imgid.add(uri);
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//            desc.add("서버");
        //}



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
                    desc.add("서버");
                }
                Message premessage=handler3.obtainMessage();
                handler3.sendMessage(premessage);

                // 아이템 리스트 전부 삭제
//                items.clear();
//
//                Iterator iterator = a.iterator();
//                while (iterator.hasNext()){
//                    items.add("hi");
//                }
////        for (int i = 0; i < a.size(); i++) {
////            items.add(a.[i]);
//
////            try {
////                //Uri uri = Uri.parse(getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project/" + fileList[i] + "/icon.png");
////
////                //imgid.add(uri);
////            }
////            catch (Exception e)
////            {
////                e.printStackTrace();
////            }
////            desc.add("서버");
//                //}
//
//
//                // 리스트 뷰에 적용
//                //listAdapter.notifyDataSetChanged();

//
//                result=true;
            }

        }.start();


    }





    public void downPath(String str, AmazonS3 s3) {

        //The path where we expect the node project to be at runtime.
        String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";

        Log.d("log",str);
        TransferManager manager = new TransferManager(s3);
        MultipleFileDownload download =  manager.downloadDirectory("s3abcd", str, new File(nodeDir));
        try
        {

            download.addProgressListener(new ProgressListener() {
                @Override
                public void progressChanged(ProgressEvent progressEvent) {
                    double pct= progressEvent.getBytesTransferred() * 100.0/ progressEvent.getBytesTransferred();
                    //Log.d("percent",String.valueOf(pct));
                    Log.d("percent",download.getProgress().getPercentTransferred()+"%");
                }
            });
            download.waitForCompletion();
            Transfer.TransferState xfer_state=download.getState();
            Log.d("state",xfer_state.toString());

        }
        catch (InterruptedException e)
        {
        }
        manager.shutdownNow();

        //prevPath = currentPath;

        // 현재 경로에서 / 와 다음 경로 붙이기
        //nextPath = currentPath + "/" + str;


                //mainActivity에 result값 전달.

//                Intent intent2=new Intent();
//                intent2.putExtra("result", nextPath+text.toString());
//                intent2.putExtra("serverName",str);
//                intent2.putExtra("doorImg","icon.png");
//                setResult(1234,intent2);
//                finish();


            }





}



