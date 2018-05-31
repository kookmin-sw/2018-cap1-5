package com.capstone.smallserver;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.MultipleFileUpload;
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
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
    private CustomListView customListView;
    private DrawerLayout mDrawerLayout;
    private NetworkInfo mNetworkState;
    Context context=this;
    private Handler handler = new Handler();
    private ProgressDialog progressDialog;

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
        setContentView(R.layout.activity_external);

        textView = (TextView) findViewById(R.id.textView);
        listView = (ListView) findViewById(R.id.uploadList2);
        final Button uploadButton=(Button)findViewById(R.id.AwsUpBtn);
        tempView=(TextView)findViewById(R.id.tempView);
        progressDialog = new ProgressDialog(ExternalExplorerActivity.this);

        items = new ArrayList<>();
        listAdapter = new ArrayAdapter<String>(ExternalExplorerActivity.this,
                android.R.layout.simple_list_item_1, items);

        //좌측 메뉴를 누르면 메뉴가 나타나도록 하기 위한 변수 할당////
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout2);
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view2);

        //좌측 메뉴를 누르면 발생하는 이벤트 처리/////////////////////////////////////////////////////////////////////////////
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                mDrawerLayout.closeDrawers();
                int id = menuItem.getItemId();
                switch (id) {

                    case R.id.homehome:
                        Intent Intent1 = new Intent(ExternalExplorerActivity.this, MainActivity.class);
                        startActivityForResult(Intent1, 1111);
                        break;



                    case R.id.hotspot:

                        k();
                        break;

                    case R.id.server_market:
                        //인터넷 연결이 되어있는 경우에만 마켓으로 연결
                        mNetworkState = getNetworkInfo();
                        if (mNetworkState != null && mNetworkState.isConnected()) {
                            Intent AWSIntent = new Intent(ExternalExplorerActivity.this, AWSExplorerActivity.class);
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

                        handler.post(new Runnable() {
                            public void run() {
                                progressDialog.setTitle("Uploading");
                                progressDialog.setMessage("Please Wait...");
                                progressDialog.setCancelable(false);
                                progressDialog.show();

                            }
                        });
                      new Thread() {
                            public  void run() {

                              TransferManager manager = new TransferManager(s3);
                              MultipleFileUpload upload =  manager.uploadDirectory("s3abcd",uploadTxt,new File(rootPath+"/"+uploadTxt), true);
                                try
                                {
                                    upload.addProgressListener(new ProgressListener() {
                                        @Override
                                        public void progressChanged(ProgressEvent progressEvent) {

                                        }
                                    });

                                    upload.waitForCompletion();
                                    Transfer.TransferState xfer_state=upload.getState();
                                    progressDialog.cancel();
                                    if(xfer_state.toString().equals("Completed"))
                                        manager.shutdownNow();
                                }
                                catch (InterruptedException e)
                                {
                                }

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

    //인터넷 연결 되어있는지 확인하는 메소드
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




