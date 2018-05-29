package com.capstone.smallserver;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private List<String> awsFolderName;
    private Uri uri;
    private String awsdescPath="";
    private String svrname="";
    private NetworkInfo mNetworkState;

    //다운로드 상태 표시를 위한 핸들러 선언
    private Handler handler = new Handler();
    private Handler handler2 =new Handler();
    private ProgressDialog progressDialog;

    CustomListView customListView;
    AmazonS3 s3;
    private DrawerLayout mDrawerLayout;
    Context context=this;


    // 다운로드 완료 후 UI에 Toast 메시지 출력하는 핸들러
    private Handler downAlarmHandler = new Handler() {

        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), "다운로드 완료!", Toast.LENGTH_SHORT).show();
            super.handleMessage(msg);
        }
    };

    //옵션 메뉴 생성 코드//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
                {
                    mDrawerLayout.closeDrawers();
                }
                else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
                return true;

        }

        return super.onOptionsItemSelected(item);
    }
    //옵션 메뉴 생성 코드 끝//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);

        items = new ArrayList<>();
        desc = new ArrayList<>();
        imgid = new ArrayList<>();
        listView = (ListView) findViewById(R.id.serverList1);
        customListView = new CustomListView(this, items, desc, imgid);
        progressDialog = new ProgressDialog(AWSExplorerActivity.this);

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Overview");
        alert.setCancelable(false);
        final WebView wv = new WebView(this);
        wv.getSettings().setLoadWithOverviewMode(true); //
        wv.getSettings().setUseWideViewPort(true); // fit to webview


        //좌측 메뉴를 누르면 메뉴가 나타나도록 하기 위한 변수 할당////
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        actionBar.setDisplayHomeAsUpEnabled(true);


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout1);
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view1);

        //좌측 메뉴를 누르면 발생하는 이벤트 처리/////////////////////////////////////////////////////////////////////////////
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                mDrawerLayout.closeDrawers();
                int id = menuItem.getItemId();
                switch (id) {
                    case R.id.homehome:
                        Intent Intent1 = new Intent(AWSExplorerActivity.this, MainActivity.class);
                        startActivityForResult(Intent1, 1111);
                        break;


                    case R.id.hotspot:

                        k();
                        break;

                    case R.id.server_market:
                        //인터넷 연결이 되어있는 경우에만 마켓으로 연결
                        mNetworkState = getNetworkInfo();
                        if (mNetworkState != null && mNetworkState.isConnected()) {
                            Intent AWSIntent = new Intent(AWSExplorerActivity.this, AWSExplorerActivity.class);
                            startActivityForResult(AWSIntent, 1111);
                        } else {
                            Toast.makeText(context, "인터넷 연결을 해주세요", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case R.id.server_make:

                        break;

                    case R.id.server_upload:
                        Intent intent2 = new Intent(getApplicationContext(),
                                ExternalExplorerActivity.class);
                        startActivity(intent2);
                        break;


                }
                return true;
            }
        });

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);

                //view.setBackgroundColor(Color.TRANSPARENT);
                //view.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                return true;
            }
        });

        alert.setView(wv);
        alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                ((ViewGroup)wv.getParent()).removeView(wv);
            }
        });
        alert.setPositiveButton("다운 받기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

                new Thread()
                {public void run()
                {
                    mNetworkState = getNetworkInfo();
                    if (mNetworkState != null && mNetworkState.isConnected()) {

                    } else {
                        return;
                    }
                    downloadEvent(svrname, s3 );
                }
                }.start();
                dialog.dismiss();
                ((ViewGroup)wv.getParent()).removeView(wv);
            }
        });



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


        //ListView initialize
        Init(s3,uihandler);
        listView.setAdapter(customListView);


        //터치했을 때 이벤트 제어 ( 눌렀을 경우 해당 서버 다운로드)
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                svrname = items.get(position).toString();


                handler2.post(new Runnable() {
                    public void run() {
                        awsdescPath="https://s3.ap-northeast-2.amazonaws.com/s3abcd/"+svrname+"/description.png";
                        wv.loadUrl(awsdescPath);

                        alert.show();

                    }
                });

            }
        });
    }

    ///////이하 메소드 선언부///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void Init(AmazonS3 s3, Handler uihandler) {

        new Thread()
        {
            public void run()
            {

                awsFolderName=listKeysInDirectory("s3abcd",s3);
                if (awsFolderName == null) {
                    Toast.makeText(AWSExplorerActivity.this, "Could not find List", Toast.LENGTH_SHORT).show();
                }

                items.clear();

                //s3abcd 버킷 내의 모든 폴더 이름을 불러와 listView로 넣는 코드 붑ㄴ

                Iterator iterator = awsFolderName.iterator();
                while (iterator.hasNext()) {
                    String name=(String) iterator.next();
                    items.add(name.replace("/",""));
                    imgid.add(uri);
                    desc.add("서버");
                }

                Message premessage=uihandler.obtainMessage();
                uihandler.sendMessage(premessage);

            }

        }.start();


    }


    //Download 처리 메소드
    public void downloadEvent(String str, AmazonS3 s3)
    {

        String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";

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

                }
            });

            download.waitForCompletion();

            //다운로드 완료 후
            Transfer.TransferState xfer_state=download.getState();
            progressDialog.cancel();
            if(xfer_state.toString().equals("Completed"))
                downAlarmHandler.sendEmptyMessage(0);

        }
        catch (InterruptedException e)
        {
        }
        manager.shutdownNow();
    }


    //AWS S3 버켓에서 폴더 이름을 받아오는 메소드
    public List<String> listKeysInDirectory(String bucketName, AmazonS3 s3) {
        String delimiter = "/";

        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucketName)
                .withDelimiter(delimiter);
        ObjectListing objects = s3.listObjects(listObjectsRequest);
        return objects.getCommonPrefixes();
    }


    private NetworkInfo getNetworkInfo()
    {
        ConnectivityManager connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo= connectivityManager.getActiveNetworkInfo();
        return  networkInfo;
    }

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

    //뒤로가기 버튼 눌릴 경우 mainActivity 재실행
    @Override
    public void onBackPressed() {
        Intent mainIntent=new Intent(AWSExplorerActivity.this, MainActivity.class);
                       startActivity(mainIntent);
    }

}



