package com.capstone.smallserver;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.MultipleFileDownload;
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Handler;


public class MainActivity extends AppCompatActivity {

    //핫스팟 설정 키기
    private void k() {
        Intent localIntent = new Intent("android.intent.action.MAIN", null);
        localIntent.addCategory("android.intent.category.LAUNCHER");
        localIntent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
        localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(localIntent);
            return;
        } catch (NullPointerException localNullPointerException) {
            localNullPointerException.printStackTrace();
            return;
        } catch (SecurityException localSecurityException) {
            for (; ; ) {
            }
        } catch (ActivityNotFoundException localActivityNotFoundException) {
            for (; ; ) {
            }
        }
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }

    //We just want one instance of node running in the background.
    public static boolean _startedNodeAlready = false;
    public static boolean _startedCopyThread = false;


    final Context context = this;

    String nodeName;
    String serverName;
    String iconImgName;
    Toolbar myToolbar;

    AmazonS3 s3Client;
    String bucket = "s3abcd";
    String path1= Environment.getExternalStorageDirectory().getAbsolutePath()+"/servers/chat/index.html";
    String path2= Environment.getExternalStorageDirectory().getAbsolutePath()+"/servers/chat/js/index.html";
    File uploadToS3 = new File(path1);
    File downloadFromS3 = new File(path2);
    TransferUtility transferUtility;
    List<String> listing;

    //    //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1111) {
            if (resultCode == 1234) {

                nodeName = data.getStringExtra("result");
                serverName = data.getStringExtra("serverName");
                iconImgName = data.getStringExtra("doorImg");

                final ImageView doorImage = (ImageView) findViewById(R.id.DoorImage);

                try {
                    Uri uri = Uri.parse(getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project/" + serverName + "/" + iconImgName);
                    doorImage.setImageURI(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.d("hi", "hh");
            }
        }
    }


    //옵션 메뉴 생성 코드
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.hotspot, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                k();
                break;
        }
        return true;
    }


//    private Handler handler = new Handler() {
//
//        public void handleMessage(Message msg) {
//
//            Toast.makeText(context, "열고 싶은 서버를 먼저 선택해주세요", Toast.LENGTH_SHORT).show();
//
//            super.handleMessage(msg);
//
//        }
//
//    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //nodeName=getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project"+"/chat/index.js";

        final Button serverButton = (Button) findViewById(R.id.ServerButton);
        final Button rspButton = (Button) findViewById(R.id.rsp);
        final Button uploadButton = (Button) findViewById(R.id.uploadBtn);

        final TextView downTxt = (TextView) findViewById(R.id.downText);
        final TextView openServerTxt = (TextView) findViewById(R.id.svText);
        final TextView listServerTxt = (TextView) findViewById(R.id.svText2);


        myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        // callback method to call credentialsProvider method.
        s3credentialsProvider();

        // callback method to call the setTransferUtility method
        setTransferUtility();

//        final Handler handler3 = new Handler() {
//            public void handleMessage(Message msg) {
//
//                downTxt.setText("서버 다운로드 중..");
//                Toast.makeText(context, "서버 다운로드 중..", Toast.LENGTH_SHORT).show();
//            }
//        };
//
//        final Handler handler2 = new Handler() {
//            public void handleMessage(Message msg) {
//                serverButton.setEnabled(true);
//                _startedCopyThread = false;
//                downTxt.setText("서버 다운로드");
//                Toast.makeText(context, "다운로드 완료!", Toast.LENGTH_SHORT).show();
//            }
//        };

        //외부 저장소에서 파일을 불러오기 위한 권한 획득

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 마시멜로우 버전과 같거나 이상이라면
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "외부 저장소 사용을 위해 읽기/쓰기 필요", Toast.LENGTH_SHORT).show();
                }

                requestPermissions(new String[]
                                {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        2);  //마지막 인자는 체크해야될 권한 갯수
            }
        }


        uploadButton.setOnClickListener(

                new Button.OnClickListener() {
                    public void onClick(View v) {

                        Intent intent = new Intent(getApplicationContext(),
                                ExternalExplorerActivity.class);
                        startActivity(intent);

                    }

                }
        );


        //node.js 엔진에서 서버를 여는 스레드 러닝 코드
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                //ExploerAcitivty에서 전달받은 경로값을 nodeName에다 넣어줌.

                if (nodeName == null) {
                    //handler.sendEmptyMessage(0);
                    return;
                }


                String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";

                Log.d("nodeDir", nodeDir);
                Log.d("nodeName", nodeName);

                startNodeWithArguments(new String[]{"node",
                        nodeName
                });
            }
        };


        //외부저장소에 있는 servers 폴더에 저장된 파일을 갖고 오는 스레드 러닝 코드

        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {

                //The path where we expect the node project to be at runtime.
                String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";
                String path1=Environment.getExternalStorageDirectory().getAbsolutePath()+"/servers";


                    TransferManager manager = new TransferManager(s3Client);
                    MultipleFileDownload download =  manager.downloadDirectory("s3abcd", "chat", new File(nodeDir));
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

                }


// try {
//                    Message premessage=handler3.obtainMessage();
//                    handler3.sendMessage(premessage);
//
//                    //copyDirectory(sourceLocation, new File(nodeDir));
//                    Message message=handler2.obtainMessage();
//                    handler2.sendMessage(message);
//
//                }
//                catch (Exception e)
//                {
//
//                }
//

         };

        //cib 스레드에 서버를 여는 런 코드를, copyassetT 스레드에 파일을 갖고오는 스레드를 할당
        Thread cib = new Thread(runnable);
         Thread copyassetT=new Thread(runnable2);


        //serverButton을 눌렀을때, 서버가져오는 스레드를 start
        serverButton.setOnClickListener(

                new Button.OnClickListener() {
                    public void onClick(View v) {

                        // if (!_startedCopyThread) {

                        //_startedCopyThread = true;

                         //copyassetT.start();


                        Intent explorerIntent=new Intent(MainActivity.this, AWSExplorerActivity.class);
                        startActivity(explorerIntent);
                        //serverButton.setEnabled(false);

                    }

                }
        );

        //서버 키기 버튼을 눌렀을 때 서버가동 스레드를 스타트, 한번 더 누를경우 프로세스 종료 후 재시작
        rspButton.setOnClickListener(

                new Button.OnClickListener() {
                    public void onClick(View v) {

                        if (!_startedNodeAlready) {

                            _startedNodeAlready = true;
                            cib.start();
//                            rspButton.setText("서버끄기");
                            openServerTxt.setText("서버 끄기");


                        } else {

                            //cib.interrupt();

                            Intent i = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                            System.exit(0);

                            _startedNodeAlready = false;


                        }
                    }
                }
        );


        //fetch버튼을 누를 경우 가져올 수 있는 서버 목록을 출력하는 acitivity로 전환

        final Button fetchButton = (Button) findViewById(R.id.ServerListButton);

        fetchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                File dir = context.getFileStreamPath("nodejs-project");
                File[] subFiles = dir.listFiles();

//                if (subFiles != null)
//                {
//                    for (File file : subFiles)
//                    {
//                        // Here is each file
//                        Log.d("hi",file.getAbsolutePath());
//                    }
//                }

                //list를 출력하는 ExplorerActivity를 호출

                Intent explorerIntent = new Intent(MainActivity.this, ExplorerActivity.class);
                startActivityForResult(explorerIntent, 1111);
            }
        });


    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //노드를 실행하는 라이브러리 메소드
    public native Integer startNodeWithArguments(String[] arguments);


//    private static boolean deleteFolderRecursively(File file) {
//        try {
//            boolean res=true;
//            for (File childFile : file.listFiles()) {
//                if (childFile.isDirectory()) {
//                    res &= deleteFolderRecursively(childFile);
//                } else {
//                    res &= childFile.delete();
//                }
//            }
//            res &= file.delete();
//            return res;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }


    public void s3credentialsProvider(){

        // Initialize the AWS Credential
        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider =
                new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        "us-east-1:4d7fa6d0-b38a-4156-b02d-62aa17339c92", // Identity Pool ID
                        Regions.US_EAST_1 // Region
                );
        createAmazonS3Client(cognitoCachingCredentialsProvider);
    }

    /**
     *  Create a AmazonS3Client constructor and pass the credentialsProvider.
     * @param credentialsProvider
     */
    public void createAmazonS3Client(CognitoCachingCredentialsProvider
                                             credentialsProvider){

        // Create an S3 client
        s3Client = new AmazonS3Client(credentialsProvider);

        // Set the region of your S3 bucket
        s3Client.setRegion(Region.getRegion(Regions.AP_NORTHEAST_2));
        s3Client.setEndpoint("s3.ap-northeast-2.amazonaws.com");    }

    public void setTransferUtility(){

       // transferUtility = new TransferUtility(s3Client, getApplicationContext());
        transferUtility= TransferUtility.builder().context(getApplicationContext()).s3Client(s3Client).build();

    }


}