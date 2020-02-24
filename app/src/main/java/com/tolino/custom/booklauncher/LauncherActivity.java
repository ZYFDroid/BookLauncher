package com.tolino.custom.booklauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tolino.custom.booklauncher.utils.AndroidUtils;
import com.tolino.custom.booklauncher.utils.DBUtils;
import com.zy.myapplication.epub.BookModel;
import com.zy.myapplication.epub.ReadEpubHeadInfo;


import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class LauncherActivity extends Activity {
    public static String bookRoot = "/mnt/sdcard/Books";

    void showHide(int id,boolean visible){
        findViewById(id).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    boolean canOperate(){

        try {
            if(!new File(bookRoot).exists()){
                new File(bookRoot).mkdirs();
            }
            return new File(bookRoot).exists();
        }catch (Exception ex){
            return false;
        }
    }
    public void openToolBox(View view) {
        startActivity(new Intent(this,ToolsActivity.class));
    }


    void showWaringMessage(){
        new AlertDialog.Builder(LauncherActivity.this).setTitle("电纸书已连接到电脑").setMessage("当前处于文件传输模式下，不能进行其它操作。建议在电脑上安全弹出。")
                .setPositiveButton("好", null).create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookRoot = new File(Environment.getExternalStorageDirectory(),"Books").getAbsolutePath();
        cacheCoverPath = getFilesDir().getAbsolutePath();
        DBUtils.init(getApplicationContext());
        setContentView(R.layout.activity_launcher);
        ReadEpubHeadInfo.setCachePath(this);
        initMainPage();
        postInitBookPage();
        postInitAppPage();
    }

    Handler hWnd = new Handler();

    Runnable postInitBookPageWaiter = new Runnable() {
        @Override
        public void run() {
            if(findViewById(R.id.gridBook).getWidth()!=0){
                initBookPage();
                return;
            }
            hWnd.postDelayed(postInitBookPageWaiter,200);
        }
    };
    void postInitBookPage(){
        hWnd.postDelayed(postInitBookPageWaiter,200);
    }

    Runnable postInitAppPageWaiter = new Runnable() {
        @Override
        public void run() {
            if(findViewById(R.id.gridView1).getWidth()!=0){
                initAppPage();
                return;
            }
            hWnd.postDelayed(postInitAppPageWaiter,200);
        }
    };
    void postInitAppPage(){
        hWnd.postDelayed(postInitAppPageWaiter,200);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentBook();
    }

    //region MainPageController

    private static final String TAG = "Main";

    public static final int brighness_offset=20;

    void initMainPage(){
        SeekBar brightness = ((SeekBar)findViewById(R.id.seekBarBrightness));
        try {
            int max = 190-brighness_offset;
            int current = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            brightness.setMax(max);
            brightness.setProgress(current-brighness_offset);
            Log.d(TAG, "initMainPage: Brightness="+current);
            brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    setBrightness(seekBar.getProgress()+brighness_offset);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    setBrightness(seekBar.getProgress()+brighness_offset);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    setSysBrightness(seekBar.getProgress()+brighness_offset);
                    setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
                    int i = (seekBar.getProgress()+brighness_offset);
                    Log.d(TAG, "onStopTrackingTouch: System brightness = "+i);
                    Log.d(TAG, "onStopTrackingTouch: Window brightness = "+(((float)i) / 190f));
                }

                void setBrightness(int i){
                    setBrightness(((float)i) / 190f);
                }
                void setBrightness(float f){
                    Window window = LauncherActivity.this.getWindow();
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.screenBrightness = f;
                    window.setAttributes(lp);
                }
                void setSysBrightness(int i){
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,i);
                }

            });
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        loadRecentBook();
    }

    int[] viewpages = {R.id.page1,R.id.page2,R.id.page3};
    public void onTabClick(View view) {
        int index = Integer.parseInt(view.getTag().toString());
        for(int i = 0; i< viewpages.length; i++){
            showHide(viewpages[i],index==i);
        }
    }


    @Override
    public void onBackPressed() {
        showHide(R.id.page1,true);
        showHide(R.id.page2,false);
        showHide(R.id.page3,false);
    }

    void loadRecentBook(){

        List<DBUtils.BookEntry> recents = DBUtils.queryBooks("type=? order by lastopen desc limit 3 offset 0", ""+DBUtils.BookEntry.TYPE_BOOK);
        if(recents.size()>0){
            ((ImageButton)findViewById(R.id.recentBook1)).setImageBitmap(getCover(recents.get(0)));
            ((ImageButton)findViewById(R.id.recentBook1)).setTag(recents.get(0));
            ((TextView)findViewById(R.id.recentTitle1)).setText(recents.get(0).getDisplayName());
        }
        else{
            ((ImageButton)findViewById(R.id.recentBook1)).setImageBitmap(makeCoverImage("图书馆是空的"));
            ((ImageButton)findViewById(R.id.recentBook1)).setTag(null);
            ((TextView)findViewById(R.id.recentTitle1)).setText("  ");
        }
        if(recents.size()>1){
            ((ImageButton)findViewById(R.id.recentBook2)).setImageBitmap(getCover(recents.get(1)));
            ((ImageButton)findViewById(R.id.recentBook2)).setTag(recents.get(1));
            ((TextView)findViewById(R.id.recentTitle2)).setText(recents.get(1).getDisplayName());
        }else{
            ((ImageButton)findViewById(R.id.recentBook2)).setImageBitmap(makeCoverImage("图书馆是空的"));
            ((ImageButton)findViewById(R.id.recentBook2)).setTag(null);
            ((TextView)findViewById(R.id.recentTitle2)).setText("  ");
        }
        if(recents.size()>2){
            ((ImageButton)findViewById(R.id.recentBook3)).setImageBitmap(getCover(recents.get(2)));
            ((ImageButton)findViewById(R.id.recentBook3)).setTag(recents.get(2));
            ((TextView)findViewById(R.id.recentTitle3)).setText(recents.get(2).getDisplayName());

        }else{
            ((ImageButton)findViewById(R.id.recentBook3)).setImageBitmap(makeCoverImage("图书馆是空的"));
            ((ImageButton)findViewById(R.id.recentBook3)).setTag(null);
            ((TextView)findViewById(R.id.recentTitle3)).setText("  ");
        }
    }

    public void onRecentBookClick(View view) {
        if(!canOperate()){
            showWaringMessage();return;
        }
        if(null!=view.getTag() && view.getTag() instanceof DBUtils.BookEntry){
            openBook((DBUtils.BookEntry)view.getTag());
        }
    }



    //endregion

    //region library

    String cacheCoverPath="";

    public DBUtils.BookEntry currentDictionary;

    void initBookPage(){

        cd(DBUtils.BookEntry.ROOT_UUID);
    }


    int currentBookPage = 0;
    int totalBookPage = 1;
    public List<DBUtils.BookEntry> lsResult = null;

    void cd(String uuid){
        currentDictionary = DBUtils.queryBooks("uuid=?", uuid).get(0);
        Log.d(TAG, "cd: "+currentDictionary.getDisplayName()+" - "+currentDictionary.getUUID());
        ls();
    }

    void cdup(){
        if(!TextUtils.isEmpty(currentDictionary.getParentUUID())){
            cd(currentDictionary.getParentUUID());
        }
    }

    private Bitmap bookshelf;
    Bitmap getCover(DBUtils.BookEntry entry){
        if(entry.getType()==entry.TYPE_FOLDER){
            if(null==bookshelf){
                bookshelf=BitmapFactory.decodeResource(getResources(),R.drawable.img_bookshelf);
            }
            return bookshelf;
        }
        else{
            File cover =new File(cacheCoverPath+"/"+entry.getUUID()+".png");
            try {
                return BitmapFactory.decodeFile(cover.getAbsolutePath());
            }catch (Exception ex){
                ex.printStackTrace();
            }
            return makeCoverImage(entry.getDisplayName());
        }
    }

    void ls(){
        lsResult = DBUtils.queryBooks("parent_uuid=? order by display_name",currentDictionary.getUUID());
        Log.d(TAG, "ls: Got "+lsResult.size()+" entries.");
        totalBookPage = lsResult.size() / BOOK_PER_PAGE;
        if(lsResult.size() % BOOK_PER_PAGE != 0){
            totalBookPage++;
        }
        if(totalBookPage ==0){
            totalBookPage++;}
        currentBookPage =0;
        showBookPage(currentBookPage);
    }

    void showBookPage(int pageId){
        ((TextView)findViewById(R.id.txtBookshelfName)).setText(currentDictionary.getDisplayName());
        GridView gridView = (GridView) findViewById(R.id.gridBook);
        BookAdapter adapter = new BookAdapter(lsResult, currentBookPage *BOOK_PER_PAGE,BOOK_PER_PAGE);
        gridView.setAdapter(adapter);
        gridView.setNumColumns(BOOK_COL);
        ((TextView)findViewById(R.id.txtBookPageInfo)).setText("第 "+(currentBookPage +1)+"/"+ totalBookPage +" 页");
    }

    private static int BOOK_PER_PAGE = 6;
    private static int BOOK_ROW = 2;
    private static int BOOK_COL = 3;

    public void onBookPageUp(View view) {
        if(currentBookPage >0){
            currentBookPage--;}
        showBookPage(currentBookPage);
    }

    public void onBookPageDn(View view) {
        if(currentBookPage < totalBookPage -1){
            currentBookPage++;}
        showBookPage(currentBookPage);
    }



    class BookAdapter extends BaseAdapter{
        List<DBUtils.BookEntry>  adapterList;
        int offset,len;

        public BookAdapter(List<DBUtils.BookEntry> adapterList, int offset, int len) {
            this.adapterList = adapterList;
            this.offset = offset;
            this.len = len;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LinearLayout layout;
            BookListViewHolder holder = new BookListViewHolder();
            if(convertView == null)
            {
                LayoutInflater inflater = getLayoutInflater();
                layout = (LinearLayout) inflater.inflate(R.layout.adapter_books, null);
                holder.viewImg = (ImageView) layout.findViewById(R.id.imageView1);
                holder.viewName = (TextView) layout.findViewById(R.id.textView1);
                layout.setTag(holder);
            }
            else
            {
                layout = (LinearLayout) convertView;
                holder = (BookListViewHolder) layout.getTag();
            }
            DBUtils.BookEntry bookEntry = (DBUtils.BookEntry) getItem(position);
            holder.viewName.setText(bookEntry.getDisplayName());
            holder.viewImg.setImageBitmap(getCover(bookEntry));

            int measureWidth = findViewById(R.id.gridBook).getWidth() / BOOK_COL;
            int measureHeight = findViewById(R.id.gridBook).getHeight() / BOOK_ROW-10;

            int[] textSize = AndroidUtils.measureView(holder.viewName);
            int textHeight = textSize[1];

            int imgHeight = measureHeight - textHeight -10;
            int imgWidth = imgHeight * 2 / 3;

            if(imgWidth > measureWidth){
                imgHeight = imgHeight * measureWidth / imgWidth;
                imgWidth = imgWidth / imgWidth * measureWidth;
            }

            holder.viewImg.getLayoutParams().width = imgWidth;
            holder.viewImg.getLayoutParams().height = imgHeight;


            if(layout.getLayoutParams()==null) {
                AbsListView.LayoutParams param = new AbsListView.LayoutParams(measureWidth, measureHeight);
                layout.setLayoutParams(param);
            }else{
                layout.getLayoutParams().height = measureHeight;
                layout.getLayoutParams().width = measureWidth;
            }
            layout.setOnClickListener(new BookClicker(bookEntry));

            return layout;
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public Object getItem(int position)
        {
            return adapterList.get(position+offset);
        }

        @Override
        public int getCount()
        {
            int leftSize = (lsResult.size() - offset);
            return leftSize > len ? len : leftSize;
        }
    }

    public void onBooksGoBack(View view) {
        if(!canOperate()){
            showWaringMessage();return;
        }
        cdup();
    }

    public void onReloadBooks(View view) {
        if(!canOperate()){
            showWaringMessage();return;
        }
        new AlertDialog.Builder(LauncherActivity.this).setTitle("扫描方式").setItems(new String[]{"扫描/sdcard/Books","扫描自定义路径"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==0){
                    scanDefaultPath();
                }
                else{
                    scanCustomPath();
                }
            }
        }).create().show();
    }

    public void scanDefaultPath(){
        new AlertDialog.Builder(LauncherActivity.this).setTitle("扫描书籍").setMessage("是否开始扫描书籍?扫描时间取决于书籍的数量。")
                .setPositiveButton("是的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scanBooks(bookRoot);
                    }
                }).setNegativeButton("不是",null).create().show();
    }

    public void scanCustomPath(){
        inputPath();
    }

    public void inputPath(){
        final EditText edt = new EditText(this);
        edt.setText(bookRoot);
        edt.setHint("输入路径");
        new AlertDialog.Builder(LauncherActivity.this).setTitle("输入要扫描的路径").setView(edt)
                .setPositiveButton("开始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scanBooks(edt.getText().toString());
                    }
                }).setNegativeButton("取消",null).create().show();
    }






    class BookListViewHolder
    {
        ImageView viewImg;
        TextView viewName;
    }

    class BookClicker implements View.OnClickListener{

        DBUtils.BookEntry entry;

        public BookClicker(DBUtils.BookEntry entry) {
            this.entry = entry;

        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "BookClicked: "+entry.getDisplayName());
            if(entry.getType()== DBUtils.BookEntry.TYPE_FOLDER){
                cd(entry.getUUID());
            }
            else{
                openBook(entry);
            }
        }
    }


    void openBook(DBUtils.BookEntry entry){
        DBUtils.execSql("update library set lastopen=? where uuid=?",System.currentTimeMillis(),entry.getUUID());
        Intent intent = new Intent();
        File file = new File(entry.getPath());
        if(!file.exists()){
            new AlertDialog.Builder(LauncherActivity.this).setTitle("打开失败").setMessage("书籍不存在，建议重新扫描。")
                    .setPositiveButton("好", null).create().show();
            return;
        }
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//设置标记
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setAction(Intent.ACTION_VIEW);//动作，查看
        intent.setDataAndType(Uri.fromFile(file), "*/*");//设置类型
        startActivity(intent);
    }

    Bitmap makeCoverImage(String title){
        Bitmap bmp = Bitmap.createBitmap(150,200, Bitmap.Config.RGB_565);
        Canvas g = new Canvas(bmp);
        Paint white = new Paint();
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        g.drawRect(0,0,bmp.getWidth(),bmp.getHeight(),white);
        TextPaint tp = new TextPaint();
        tp.setTextSize(20);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setColor(Color.BLACK);
        tp.setAntiAlias(true);
        g.translate(bmp.getWidth()/2,bmp.getHeight()/2.5f);
        StaticLayout staticLayout = new StaticLayout(title,tp,bmp.getWidth()-10, Layout.Alignment.ALIGN_NORMAL,1.0f,0.0f,true);
        g.translate(0,-staticLayout.getHeight()/2);
        staticLayout.draw(g);
        return bmp;
    }
    Bitmap getCoverImage(File file){
        String name="";
        try {
            BookModel model = ReadEpubHeadInfo.getePubBook(file.getAbsolutePath());
            name=model.getName();
            Bitmap bmp = BitmapFactory.decodeFile(model.getCover());
            Bitmap bmp2 = Bitmap.createBitmap(240,360, Bitmap.Config.ARGB_8888);
            Canvas g = new Canvas(bmp2);
            Paint white = new Paint();
            white.setAntiAlias(true);
            g.drawBitmap(bmp,new Rect(0,0,bmp.getWidth(),bmp.getHeight()),new Rect(0,0,bmp2.getWidth(),bmp2.getHeight()),white);
            bmp.recycle();
            return bmp2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(name.length()>0){return makeCoverImage(name);}
        return makeCoverImage(file.getName());
    }

    TempBookInfo readBookInfo(File file){
        String name="";
        String title="";
        try {
            BookModel model = ReadEpubHeadInfo.getePubBook(file.getAbsolutePath());
            name=model.getName();
            title=name+" - "+model.getAuthor();
            Bitmap bmp = BitmapFactory.decodeFile(model.getCover());
            Bitmap bmp2 = Bitmap.createBitmap(150,200, Bitmap.Config.RGB_565);
            Canvas g = new Canvas(bmp2);
            Paint white = new Paint();
            white.setAntiAlias(true);
            g.drawBitmap(bmp,new Rect(0,0,bmp.getWidth(),bmp.getHeight()),new Rect(0,0,bmp2.getWidth(),bmp2.getHeight()),white);
            bmp.recycle();
            return new TempBookInfo(title,bmp2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(name.length()>0){return new TempBookInfo(title,makeCoverImage(name));}
        return new TempBookInfo(file.getName(),makeCoverImage(file.getName()));
    }

    class TempBookInfo{
        String title;
        Bitmap cover;

        public TempBookInfo(String title, Bitmap cover) {
            this.title = title;
            this.cover = cover;
        }
    }

    void scanBooks(String scanPath){
        AsyncTask<String,String,String> scanTask = new AsyncTask<String, String, String>() {

            HashMap<String,String> pathToUUID = new HashMap<String, String>();
            List<DBUtils.BookEntry> bookEntries = new ArrayList<DBUtils.BookEntry>();
            List<DBUtils.BookEntry> folderEntries = new ArrayList<DBUtils.BookEntry>();

            List<String> path = new ArrayList<String>();
            List<String> bookPath = new ArrayList<String>();
            void rescurePath(String root){
                File f = new File(root);
                if(f==null || f.getName().startsWith(".")){
                    return;
                }
                for(File subf : f.listFiles()){
                    if(subf.isDirectory()){
                        rescurePath(subf.getAbsolutePath());
                        path.add(subf.getAbsolutePath());
                        Log.d(TAG, "rescurePath: "+subf.getAbsolutePath());
                    }else{
                        if(subf.getName().toLowerCase().endsWith(".epub")){
                            Log.d(TAG, "rescureFile: "+subf.getAbsolutePath());
                            bookPath.add(subf.getAbsolutePath());
                        }
                    }
                }


            }

            void delay(int mill){
                try {
                    Thread.sleep(mill);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    publishProgress("正在扫描目录...");
                    delay(300);
                    rescurePath(params[0]);
                    publishProgress("扫描到了" + bookPath.size() + "本书，" + path.size() + "个目录");
                    delay(500);
                    publishProgress("正在读取数据库...");

                    List<DBUtils.BookEntry> pathInDb = DBUtils.queryBooks("type=?", String.valueOf(DBUtils.BookEntry.TYPE_FOLDER));
                    for (DBUtils.BookEntry pid : pathInDb) {
                        pathToUUID.put(pid.getPath(), pid.getUUID());
                    }
                    List<DBUtils.BookEntry> newPaths = new ArrayList<DBUtils.BookEntry>();
                    for (String p : path) {
                        if (!pathToUUID.containsKey(p)) {
                            newPaths.add(DBUtils.BookEntry.createFolder("", p));
                        }
                    }
                    for (DBUtils.BookEntry pid : newPaths) {
                        pathToUUID.put(pid.getPath(), pid.getUUID());
                    }
                    for (DBUtils.BookEntry pid : newPaths) {
                        if (TextUtils.isEmpty(pid.getParentUUID())) {
                            String parentPath = new File(pid.getPath()).getParentFile().getAbsolutePath();
                            String path2uuid = pathToUUID.get(parentPath);
                            pid.setParentUUID(path2uuid != null ? path2uuid : DBUtils.BookEntry.ROOT_UUID);
                        }
                    }
                    folderEntries.addAll(newPaths);

                    List<DBUtils.BookEntry> bookInDb = DBUtils.queryBooks("type=?", String.valueOf(DBUtils.BookEntry.TYPE_BOOK));

                    List<String> bookPathInDb = new ArrayList<String>();
                    for (DBUtils.BookEntry bk :
                            bookInDb) {
                        bookPathInDb.add(bk.getPath());
                    }

                    List<String> newBookPathList = new ArrayList<String>();
                    for (String newBookPath : bookPath) {
                        if (!bookPathInDb.contains(newBookPath)) {
                            newBookPathList.add(newBookPath);
                        }
                    }
                    publishProgress("找到" + newBookPathList.size() + "本新书。");
                    delay(500);
                    publishProgress("开始添加...");
                    delay(200);

                    int success = 0, deleted = 0;

                    for (int i = 0; i < newBookPathList.size(); i++) {
                        publishProgress("正在添加第" + (i + 1) + "/" + newBookPathList.size() + "本书");
                        try {
                            File bf = new File(newBookPathList.get(i));
                            String parentPath = bf.getParentFile().getAbsolutePath();
                            TempBookInfo readinfo = readBookInfo(bf);
                            String path2uuid = pathToUUID.get(parentPath);
                            DBUtils.BookEntry tmpEntry = DBUtils.BookEntry.createBook(path2uuid != null ? path2uuid : DBUtils.BookEntry.ROOT_UUID, readinfo.title, bf.getAbsolutePath());
                            if (!new File(cacheCoverPath).exists()) {
                                new File(cacheCoverPath).mkdirs();
                            }
                            String coverPathName = cacheCoverPath + "/" + tmpEntry.getUUID() + ".png";
                            readinfo.cover.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(coverPathName));
                            bookEntries.add(tmpEntry);
                            readinfo.cover.recycle();
                            success++;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    publishProgress("添加完成, 正在保存到数据库...");
                    delay(300);
                    DBUtils.InsertBooks(folderEntries);
                    DBUtils.InsertBooks(bookEntries);
                    publishProgress("正在查找已删除或移动的书籍...");
                    delay(300);
                    deleted = cleanDB();
                    int removeEmptyPath = cleanEmptyDir();
                    while (removeEmptyPath>0){
                        removeEmptyPath = cleanEmptyDir();
                    }
                    return "添加了" + success + "本书，移除了" + deleted + "本书";
                }catch (Exception ex){
                    ex.printStackTrace();
                    return "扫描失败："+ ex.toString();

                }
            }

            int cleanDB(){
                int deleted = 0;
                List<String> deletionUUIDs = new ArrayList<String>();
                List<DBUtils.BookEntry> bookInDb = DBUtils.queryBooks("1=1");
                for (DBUtils.BookEntry bk : bookInDb) {
                    if(bk.getType()== DBUtils.BookEntry.TYPE_BOOK) {
                        if (!new File(bk.getPath()).exists()){
                            deletionUUIDs.add(bk.getUUID());
                            deleted++;
                        }
                    }
                }
                for (String delete : deletionUUIDs) {
                    DBUtils.execSql("delete from library where uuid=?",delete);
                    File imgCover = new File(cacheCoverPath+"/"+delete+".png");
                    if(imgCover.exists()){
                        imgCover.delete();
                    }
                }
                return deleted;
            }

            int cleanEmptyDir(){
                int deleted = 0;
                List<String> deletionUUIDs = new ArrayList<String>();
                List<DBUtils.BookEntry> bookInDb = DBUtils.queryBooks("type=?", String.valueOf(DBUtils.BookEntry.TYPE_FOLDER));
                for (DBUtils.BookEntry bk : bookInDb) {
                    if(bk.getUUID().equals(DBUtils.BookEntry.ROOT_UUID)){continue;}
                    if(DBUtils.getCount("parent_uuid=?",bk.getUUID())==0){
                        deletionUUIDs.add(bk.getUUID());
                    }
                }
                for (String delete : deletionUUIDs) {
                    DBUtils.execSql("delete from library where uuid=?",delete);
                    deleted++;
                }
                return deleted;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                if(null!=pdd){
                    pdd.setTitle(values[0]);
                }
            }
            ProgressDialog pdd;
            @Override
            protected void onPreExecute() {
                pdd = new ProgressDialog(LauncherActivity.this,ProgressDialog.STYLE_SPINNER);
                pdd.setTitle("扫描书籍");
                pdd.setCancelable(false);
                pdd.show();
            }

            @Override
            protected void onPostExecute(String s) {
                pdd.dismiss();
                cd(DBUtils.BookEntry.ROOT_UUID);
                loadRecentBook();
                new AlertDialog.Builder(LauncherActivity.this).setTitle("扫描完成").setMessage(s)
                        .setPositiveButton("好", null).create().show();
            }
        }.execute(scanPath);
    }


    //endregion

    //region applications

    private List<ResolveInfo> mApps = null;
    private ArrayList<HashMap<String, Object>> data = null;
    private HashMap<String, Object> hashMap = null;

    public void onReloadApp(View view) {
        loadapp();
        loadPage();
    }

    class AppListViewHolder
    {
        ImageView viewImg;
        TextView viewName;
    }

    class IntentClicker implements View.OnClickListener{
        private Intent launchIntent;
        private String title;

        public IntentClicker(Intent launchIntent, String title) {
            this.launchIntent = launchIntent;
            this.title = title;
        }

        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(LauncherActivity.this).setTitle("打开外部应用").setMessage("是否打开 "+title+"?")
                    .setPositiveButton("是的", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try{
                                startActivity(launchIntent);
                            }catch (Exception ex){
                                ex.printStackTrace();
                                AndroidUtils.Toast(LauncherActivity.this, "无法启动这个应用");
                            }
                        }
                    }).setNegativeButton("不是",null).create().show();
        }
    }

    class InfoPresser implements View.OnLongClickListener{

        String appName,pkgName;

        public InfoPresser(String appName, String pkgName) {
            this.appName = appName;
            this.pkgName = pkgName;
        }

        @Override
        public boolean onLongClick(View v) {

            new AlertDialog.Builder(LauncherActivity.this).setTitle(appName).setItems(new String[]{"应用信息"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==0){
                        Intent mIntent = new Intent();
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (Build.VERSION.SDK_INT >= 9) {
                            mIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                            mIntent.setData(Uri.fromParts("package", pkgName, null));
                        } else if (Build.VERSION.SDK_INT <= 8) {
                            mIntent.setAction(Intent.ACTION_VIEW);
                            mIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                            mIntent.putExtra("com.android.settings.ApplicationPkgName", pkgName);
                        }
                        startActivity(mIntent);
                    }
                }
            }).create().show();
            return true;
        }
    }

    void loadPage(){
        ((TextView)findViewById(R.id.txtAppPageInfo)).setText("第 "+(currentAppPage +1)+"/"+ totalAppPage +" 页");
        GridView gridView = (GridView) findViewById(R.id.gridView1);
        BaseAdapter adapter = new AppAdapter(currentAppPage * APP_PER_PAGE,APP_PER_PAGE);
        gridView.setAdapter(adapter);
        gridView.setNumColumns(APP_COL);
    }

    private static int APP_PER_PAGE = 16;
    private static int APP_ROW = 4;
    private static int APP_COL = 4;

    private void loadapp()
    {
        Intent intent = new Intent(Intent.ACTION_MAIN,null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = getPackageManager().queryIntentActivities(intent, 0);
        currentAppPage = 0;

        totalAppPage = mApps.size() / APP_PER_PAGE;
        if(mApps.size() % APP_PER_PAGE != 0){
            totalAppPage++;
        }
        if(totalAppPage ==0){
            totalAppPage++;}
        currentAppPage =0;
    }
    void initAppPage(){
        loadapp();
        loadPage();
    }

    public void onAppPageUp(View view) {
        if(currentAppPage >0){
            currentAppPage--;}
        loadPage();
    }

    public void onAppPageDn(View view) {
        if(currentAppPage < totalAppPage -1){
            currentAppPage++;}
        loadPage();
    }

    int currentAppPage = 0;
    int totalAppPage = 1;

    class AppAdapter extends BaseAdapter{
        int offset,len;
        public AppAdapter(int offset, int len) {
            this.offset = offset;
            this.len = len;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LinearLayout layout;
            AppListViewHolder holder = new AppListViewHolder();
            if(convertView == null)
            {
                LayoutInflater inflater = getLayoutInflater();
                layout = (LinearLayout) inflater.inflate(R.layout.adapter_app, null);

                holder.viewImg = (ImageView) layout.findViewById(R.id.imageView1);
                holder.viewName = (TextView) layout.findViewById(R.id.textView1);
                layout.setTag(holder);
            }
            else
            {
                layout = (LinearLayout) convertView;
                holder = (AppListViewHolder) layout.getTag();
            }

            ResolveInfo info = (ResolveInfo) getItem(position);
            holder.viewImg.setImageDrawable(info.activityInfo.loadIcon(getPackageManager()));
            holder.viewName.setText(info.activityInfo.loadLabel(getPackageManager()).toString());

            Intent launchIntent = null;
            try{
                Intent intent = new Intent();
                ComponentName comp = new ComponentName( info.activityInfo.packageName, info.activityInfo.name);
                intent.setComponent(comp);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launchIntent = intent;
            }catch (Exception ex){
                ex.printStackTrace();
            }

            layout.setClickable(true);
            layout.setOnClickListener(new IntentClicker(launchIntent,holder.viewName.getText().toString()));
            layout.setOnLongClickListener(new InfoPresser(holder.viewName.getText().toString(),info.activityInfo.packageName));
            int measureWidth = findViewById(R.id.gridView1).getWidth() / APP_COL;
            int measureHeight = findViewById(R.id.gridView1).getHeight() / APP_ROW-10;
            if(layout.getLayoutParams()==null) {
                AbsListView.LayoutParams param = new AbsListView.LayoutParams(measureWidth, measureHeight);
                layout.setLayoutParams(param);
            }else{
                layout.getLayoutParams().height = measureHeight;
                layout.getLayoutParams().width = measureWidth;
            }
            return layout;
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public Object getItem(int position)
        {
            return mApps.get(position + offset);
        }

        @Override
        public int getCount()
        {
            int leftSize = (mApps.size() - offset);
            return leftSize > len ? len : leftSize;
        }
    }

    //endregion
}
