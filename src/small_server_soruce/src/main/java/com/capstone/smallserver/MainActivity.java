package com.capstone.smallserver;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.policy.Resource;
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
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/* 액티비티 설명: 현재 저장되어있는 서버 listView로 출력, 다른 메뉴로 넘어갈 수 있는 액티비티.
                                                                                        */


public class MainActivity extends AppCompatActivity {


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }

    //We just want one instance of node running in the background.
    public static boolean _startedNodeAlready = false;

    final Context context = this;

    private NetworkInfo mNetworkState;
    private String nodeName;
    private String serverName;
    private String iconImgName;
    private DrawerLayout mDrawerLayout;

    private ListView listView;
    private ArrayList<String> items;
    private ArrayList<String> desc;
    private ArrayList<Uri> imgid;
    private CustomListView customListView;

    private String rootPath = "";
    private String nextPath = "";
    private String currentPath = "";

    private Button btnClosePopup;
    private Button btnServerStart;
    private ImageView doorImg;
    private Uri uri;

    private PopupWindow pwindo;
    private int mWidthPixels, mHeightPixels;
    private Thread cib;


    //삭제후 listView 갱신하기 위한 UI제어 핸들러
    private Handler refreshHandler = new Handler() {
        public void handleMessage(Message msg) {


            boolean result = Init(getApplicationContext().getFilesDir().getAbsolutePath()+"/nodejs-project");

            if (result == false)
                return;

            listView.setAdapter(customListView);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final Button offButton = (Button) findViewById(R.id.button);

        //////////////listView를 위한 변수할당/////////////////////////////////
        items = new ArrayList<>();
        desc = new ArrayList<>();
        imgid = new ArrayList<>();
        listView = (ListView) findViewById(R.id.serverList);
        customListView = new CustomListView(this, items, desc, imgid);

        //////////////팝업창을 위한 변수할당/////////////////////////////////
        WindowManager w = getWindowManager();
        Display d = w.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        // since SDK_INT = 1;
        mWidthPixels = metrics.widthPixels;
        mHeightPixels = metrics.heightPixels;

        //좌측 메뉴를 누르면 메뉴가 나타나도록 하기 위한 변수 할당////
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);



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


        //좌측 메뉴를 누르면 발생하는 이벤트 처리/////////////////////////////////////////////////////////////////////////////
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                mDrawerLayout.closeDrawers();
                int id = menuItem.getItemId();
                switch (id) {
                    case R.id.hotspot:

                        k();
                        break;

                    case R.id.server_market:
                        //인터넷 연결이 되어있는 경우에만 마켓으로 연결
                        mNetworkState = getNetworkInfo();
                        if (mNetworkState != null && mNetworkState.isConnected()) {
                            Intent AWSIntent = new Intent(MainActivity.this, AWSExplorerActivity.class);
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


        //팝업창 설정 초기화/////////////////////////////////////////////////////////////////////////////
        // 상태바와 메뉴바의 크기를 포함해서 재계산
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
            try {
                mWidthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                mHeightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
        // 상태바와 메뉴바의 크기를 포함
        if (Build.VERSION.SDK_INT >= 17)
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                mWidthPixels = realSize.x;
                mHeightPixels = realSize.y;
            } catch (Exception ignored) {
            }


        /////////////////////ㅣistView 에 서버목록 적용 부분////////////////////////////////////////////////////////////////////////////
        rootPath = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";
        boolean result = Init(rootPath);

        if (result == false)
            return;

        listView.setAdapter(customListView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                currentPath = rootPath;
                String path = items.get(position).toString();

                PathForOpen(path);
            }
        });

        //오래 눌렀을 경우 삭제하는 기능 출력 구현부/////////////////////////////////////////////////////////////////
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           final int position, long id) {
                AlertDialog diaBox = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("삭제")
                        .setMessage("해당 서버를 삭제하시겠습니까?")
                        .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //The path where we expect the node project to be at runtime.

                                        String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project/" + items.get(position);
                                        //Recursively delete any existing nodejs-project.
                                        File nodeDirReference = new File(nodeDir);
                                        if (nodeDirReference.exists()) {
                                            deleteFolderRecursively(new File(nodeDir));

                                        }
                                        Message message = refreshHandler.obtainMessage();
                                        refreshHandler.sendMessage(message);

                                    }
                                }).start();
                                listView.clearChoices();


                            }
                        })
                        .setNegativeButton("돌아가기", null)
                        .create();
                diaBox.show();
                return true;
            }
        });


        //서버 끄기 버튼 눌렀을 경우 이벤트 처리를 위한 리스너. 프로세스를 종료하고 재시작 한다.
        offButton.setOnClickListener(

                new Button.OnClickListener() {
                    public void onClick(View v) {

                        Intent i = getBaseContext().getPackageManager()
                                .getLaunchIntentForPackage(getBaseContext().getPackageName());
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        System.exit(0);

                        _startedNodeAlready = false;

                    }
                }
        );


        ///////////////////////////////////////////////////////////////////////////
        //node.js 엔진에서 서버를 여는 스레드 러닝 코드
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                   if (nodeName == null) {
                    return;
                }

                startNodeWithArguments(new String[]{"node",
                        nodeName
                });
            }
        };

        //cib 스레드에 서버를 여는 런 코드 스레드를 할당
        cib = new Thread(runnable);

    }



    ///////이하 메소드 선언부///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //노드를 실행하는 라이브러리 메소드
    public native Integer startNodeWithArguments(String[] arguments);


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

    //폴더 삭제하는 메소드
    private static boolean deleteFolderRecursively(File file) {
        try {
            boolean res=true;
            for (File childFile : file.listFiles()) {
                if (childFile.isDirectory()) {
                    res &= deleteFolderRecursively(childFile);
                } else {
                    res &= childFile.delete();
                }
            }
            res &= file.delete();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    //listView에 서버목록 넣기 위해 initialize하는 메소드
    public boolean Init(String rootPath)
    {
        // 파일 객체 생성
        File fileRoot = new File(rootPath);
        if (!fileRoot.isDirectory()) {
            return false;
        }

        // 파일 리스트 가져오기
        String[] fileList = fileRoot.list();
        if (fileList == null) {
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
        customListView.notifyDataSetChanged();
        return true;
    }


    public void PathForOpen(String str) {

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

                nodeName = nextPath+text.toString();
                serverName=str;
                iconImgName = "icon.png";
                uri = Uri.parse(getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project/" + serverName + "/" + iconImgName);

                //팝업 띄우기
                initiatePopupWindow();

            }
        }
    }


    private void initiatePopupWindow() {
        try {
            //  LayoutInflater 객체와 시킴
            LayoutInflater inflater = (LayoutInflater) MainActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View layout = inflater.inflate(R.layout.screen_popup,
                    (ViewGroup) findViewById(R.id.popup_element));

            pwindo = new PopupWindow(layout, mWidthPixels-200, mHeightPixels-700, true);
            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);

            btnClosePopup = (Button) layout.findViewById(R.id.btn_close_popup);
            btnServerStart=(Button)layout.findViewById(R.id.startBtn);
            doorImg = (ImageView) layout.findViewById(R.id.DoorImage);

            try {
                doorImg.setImageURI(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }

            btnClosePopup.setOnClickListener(cancel_button_click_listener);
            btnServerStart.setOnClickListener(start_button_click_listener);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private View.OnClickListener cancel_button_click_listener =
            new View.OnClickListener() {

                public void onClick(View v) {

                    pwindo.dismiss();
                }
            };

    private View.OnClickListener start_button_click_listener =
            new View.OnClickListener() {

                public void onClick(View v) {
                    _startedNodeAlready = true;
                    cib.start();
                    pwindo.dismiss();

                }
            };

}