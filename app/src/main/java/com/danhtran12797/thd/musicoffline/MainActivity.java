package com.danhtran12797.thd.musicoffline;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static int REQUEST_CODE = 1997;

    private Button btnFav, btnPlaylist, btnSong;

    private Dialog dialog_load_data;

    private long backPressedTime;
    private Toast backToast;

    // biến mà khai báo public static:
    // biến này dùng chung
    // chỉ bị hủy khi thoát app
    // chi tiết có thể tìm google

    public static String KEY = "music";
    public static String KEY_SONGS = "songs";
    public static final String KEY_PLAYLISTS = "playlists";
    public static final String KEY_FAVORITES = "favorites";

    // list chứa tất cả ds bài hát
    // arrSong sẽ thay đổi trong suốt quá trình app hoạt động
    // thay đổi khi có thêm 1 bài hát yêu thích, hủy bài hát yêu thích
    // thêm 1 bài hát vào playlist, hủy bài hát khỏi playlist,
    public static ArrayList<Music> arrSong;

    // ds bài hát yêu thích
    private ArrayList<Music> arrFav;
    // ds bài hát trong playlist
    private ArrayList<Music> arrPlaylist;

    // ds các id dc yêu thích
    public ArrayList<String> arrID_Fav;
    // ds các id playlist
    public ArrayList<String> arrID_Playlist;

    public static final String SHARED_PREFERENCES_NAME = "MUSIC2019";
    private SharedPreferences sharedPreferences = null;

    private Toolbar toolbar;
    private Intent intent;

    public static final int ITEM_DELAY = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDialogLoadData();
        CheckPermission();

    }

    private void initDialogLoadData() {
        dialog_load_data = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog_load_data.setContentView(R.layout.layout_dialog_load_data);
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar_home);
        setSupportActionBar(toolbar);

        btnFav = findViewById(R.id.btnFav);
        btnPlaylist = findViewById(R.id.btnPlaylists);
        btnSong = findViewById(R.id.btnSongs);

        btnFav.setOnClickListener(this);
        btnSong.setOnClickListener(this);
        btnPlaylist.setOnClickListener(this);
    }

    // Kiểm tra xem user có xác nhận quyền đọc bộ nhớ hay k.
    // nếu có thì load data
    // nếu k thì sẽ xuất hộp thoại xác nhận quyền đọc, việc có xác nhận hay k của user sẽ xử lý trong hàm onRequestPermissionsResult
    public void CheckPermission(){
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // mở hộp thoại xác nhận thông báo với requestCode=1(bạn cho giá trị bao nhiêu cũng dc)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            // thực thi Asyntask LoadSong
            new LoadSong().execute();
        }
    }

    // hàm này trên mạng nhiều lắm
    // hàm này sẽ lấy ra các file audio trong máy của bạn, dùng Cursor để duyệt
    // và lấy ra thông tin từ cursor: id, name, singer, path.
    // new 1 đối tượng Music với các thông tin, rồi add vào mp3Files, rồi return mp3Files.
    // đừng quên kiểm tra id nó có thuộc trong ds id arrID_Fav và arrID_Playlist
    // nếu có thì set lại cho check_fav hoặc check_playlist
    // Note: đọc file mp3 theo cách này thì...k thể đọc dc thư mục Zing Mp3(k hiểu tại sao lun, ai cũng gặp trường hợp z hết)
    // nên viết thêm hàm đọc trực tiếp file từ thư mục mình chọn
    // hàm này là: getZingMP3(String rootPath), đọc tiếp sẽ thấy
    public ArrayList<Music> scanDeviceForMp3Files() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                //MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media._ID
        };
        //final String sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC";
        ArrayList<Music> mp3Files = new ArrayList<>();

        Cursor cursor = null;
        try {
            Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            cursor = getContentResolver().query(uri, projection, selection, null, "");
            if (cursor != null) {
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    String path = cursor.getString(2);
                    String nameSinger = cursor.getString(1);
                    String nameSong = cursor.getString(0);
                    String id = cursor.getString(3);

                    cursor.moveToNext();
                    if (path != null && path.endsWith(".mp3")) {
                        boolean check_fav = false;
                        boolean check_playlist = false;

                        if (arrID_Fav.contains(id))
                            check_fav = true;
                        if (arrID_Playlist.contains(id))
                            check_playlist = true;

                        mp3Files.add(new Music(nameSong, nameSinger, true, path, id, check_fav, check_playlist));
                    }
                }
            }


        } catch (Exception e) {
            Log.e("TAG", e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return mp3Files;
    }

    // hàm này trên mạng
    // hàm này là cần truyền 1 đường dẫn thư mục
    // duyệt trong 1 thư mục đó, lấy ra thông tin file mp3
    // trả về ds các bài hát
    // đồng thời kiểm tra xem id đó có thuộc ds id arrID_Playlist hoặc ds arrID_Fav
    // nếu có thì set lại cho check_fav hoặc check_playlist
    // note: hàm này mình sẽ truyền vào đường dẫn thư mục Zing MP3
    public ArrayList<Music> getZingMP3(String rootPath) {
        ArrayList<Music> fileList = new ArrayList<>();

        try {
            File rootFolder = new File(rootPath);
            File[] files = rootFolder.listFiles(); //here you will get NPE if directory doesn't contains  any file,handle it like this.
            for (File file : files) {
                if (file.isDirectory()) {
                    if (getZingMP3(file.getAbsolutePath()) != null) {
                        fileList.addAll(getZingMP3(file.getAbsolutePath()));
                    } else {
                        break;
                    }
                    // trong đây là tìm thấy file .mp3
                } else if (file.getName().endsWith(".mp3")) {
                    //String parent = file.getParentFile().getName();

                    // name file(.mp3) trong thư mục Zing MP3 sẽ là:
                    // Em Gì Ơi_Jack, K-ICM_-1079353759.mp3
                    // nhìn kỹ sẽ thấy: tên bài hát, ca sĩ và id bài hát.
                    // giờ mình tách name file ra, để lấy thông tin
                    // new 1 đối tượng Music với 3 thông tin đó
                    // ở đây mình mặc định là bài hát trong Zing MP3 sẽ là bài hát download xuống local=false

                    String arr[] = file.getName().split("_");
                    String id = arr[2].substring(1, arr[2].indexOf("."));
                    boolean check_fav = false;
                    boolean check_playlist = false;
                    if (arrID_Fav.contains(id))
                        check_fav = true;
                    if (arrID_Playlist.contains(id))
                        check_playlist = true;

                    fileList.add(new Music(arr[0], arr[1], false, file.getPath(), id, check_fav, check_playlist));
                }
            }
            return fileList;
        } catch (Exception e) {
            return fileList;
        }

    }

    // lấy chuỗi các id fav(or playlist) từ sharedPreferences rồi tách nó ra gán vào arrayList
    public ArrayList<String> get_arr_id(String id_song) {
        String arr[] = id_song.split("-");
        return new ArrayList<>(Arrays.asList(arr));
    }

    // so sánh 2 arrayList
    // 1 arrayList chứa id của Fav hoặc playlist
    // 1 chứa ds music của tất cả các bài hát
    // nếu truyền vào là 1 arrID_Playlist thì sẽ return ds các bài hát playlist
    public ArrayList<Music> create_fav_playlist_music(ArrayList<String> arrayList) {
        ArrayList<Music> arrTemp = new ArrayList<>();
        for (String id : arrayList) {
            for (Music music : arrSong) {
                if (id.equals(music.getId())) {
                    arrTemp.add(music);
                    break;
                }
            }
        }
        return arrTemp;
    }

    public void addMusic() {
        // xem youtube để hiểu chi tiết hơn về SharedPreferences(nó thường dùng để lưu trạng thái)
        // ở đây mình dùng nó để lưu 1 chuỗi id playlist bởi dấu '-', khi lấy ra thì tách các id đó ra cũng bởi dấu '-' gán vào ArayList.
        // tương tự favorites
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        // lấy chuỗi id favorites lưu từ bộ nhớ lên
        // nếu k tồn tại KEY_FAVORITES thì sẽ gán mặc định là ""
        String fav = sharedPreferences.getString(KEY_FAVORITES, "");
        String playlist = sharedPreferences.getString(KEY_PLAYLISTS, "");

        arrID_Fav = get_arr_id(fav); // hàm tách chuỗi thành các id rối gán cho ArrlayList
        arrID_Playlist = get_arr_id(playlist); // hàm tách chuỗi thành các id rối gán cho ArrlayList

        arrSong = new ArrayList<>();

        // sắp xếp các file theo thời gian dc tải vào máy
        Collections.sort(arrSong, new Comparator<Music>() {
            @Override
            public int compare(Music o1, Music o2) {
                return compare(new File(o2.getPath()).lastModified(), new File(o1.getPath()).lastModified());
            }

            private int compare(long lastModified, long lastModified1) {
                return (int) (lastModified - lastModified1);
            }
        });

        // đọc trực tiếp từ thư mục Zing MP3
        arrSong.addAll(getZingMP3(Environment.getExternalStorageDirectory() + "/Zing MP3"));
        // đọc tất cả các bài hát trong máy, kể cả thẻ nhớ, nhưng nó k đọc dc trong thư mục /Zing MP3(k hiểu tại sao)
        arrSong.addAll(scanDeviceForMp3Files());

        // chuỗi id fav
        if (fav.equals(""))
            arrFav = new ArrayList<>();
        else {
            // vào chi tiết hàm sẽ rõ
            // click chuột vào hàm gọi rồi ctrl + b
            arrFav = create_fav_playlist_music(arrID_Fav);
        }
        // chuỗi id playlist
        if (playlist.equals(""))
            arrPlaylist = new ArrayList<>();
        else {
            arrPlaylist = create_fav_playlist_music(arrID_Playlist);
        }
    }

    // cáy này là xử lý đa tiến trình, có nhiếu cách, nhưng Android có hộ trợ thằng AsyncTask, thì dùng nó thôi
    // ở đây nó hoạt động: onPreExecute -> doInBackground -> onPostExecute
    // chi tiết hơn thì youtube: AsyncTask trong Android, xem sẽ hiểu chi tiết hơn.
    public class LoadSong extends AsyncTask<Void, Void, String> {

        // khi đã thực thi: new LoadSong().execute(), thì  onPreExecute sẽ chạy vào đầu tiên
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // cho show ra cái dialog full màn hình, để tránh thời giàn chờ load data
            dialog_load_data.show();
        }

        // khi đã extends AsyncTask, thì bắt buộc phải Override lại thằng doInBackground,
        // những khác k có cũng k sao,k thì sẽ lỗi
        // xử lý load bài hát tại đây(hoặc load dữ liêu internet, cái nào mà tốn time nhiều,...)
        @Override
        protected String doInBackground(Void... voids) {
            // load data
            addMusic();
            // ở đây mình có cho nó return về String, nó k có ý nghĩa gì hết
            // k trả về cũng dc, nhưng phải khai báo lại phía trên: LoadSong extends AsyncTask<Void, Void, Void>
            return "Good";
        }

        // xử lý xong doInBackground sẽ chạy vào  thằng này.
        @Override
        protected void onPostExecute(String aVoid) {
            super.onPostExecute(aVoid);

            // hiệu ứng 3 nút button
            animate();
            // tắt dialog
            dialog_load_data.dismiss();
            btnFav.setText("Yêu thích(" + arrFav.size() + ")");
            btnPlaylist.setText("Playlist(" + arrPlaylist.size() + ")");
            btnSong.setText("Bài hát(" + arrSong.size() + ")");
            Log.d("CCC", "LoadActivity: onPostExecute");
        }
    }

    // anim của 3 button(Song, Playlists, Favorites) khi mở  app lên
    private void animate() {
        ViewGroup container = findViewById(R.id.layout_off);

        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            ViewPropertyAnimatorCompat viewAnimator;

            if (v instanceof Button) {
                viewAnimator = ViewCompat.animate(v)
                        .scaleY(1).scaleX(1)
                        .setStartDelay((ITEM_DELAY * i) + 500)
                        .setDuration(500);
                viewAnimator.setInterpolator(new DecelerateInterpolator()).start();
            }
        }
    }

    // xử lý user có chấp nhận quyền đọc bộ nhớ hoặc k chấp nhận
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // resquestCode==1 tương ứng với phía trên
        if (requestCode == 1) {
            // User đã chấp nhận quyền đọc bộ nhớ
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã được cho phép", Toast.LENGTH_SHORT).show();
                new LoadSong().execute();
            } else { // user k chấp nhận quyền đọc bộ nhớ -> k load dc bài hát nào hết.
                arrSong = new ArrayList<>();
                arrFav = new ArrayList<>();
                arrPlaylist = new ArrayList<>();

                // k load dc, thì gán text cho button là 0 thôi.
                btnSong.append("(0)"); // Song(0)
                btnPlaylist.append("(0)"); // Playlist(0)
                btnFav.append("(0)"); // Favorites(0)

                Toast.makeText(this, "Từ chối xác nhận quyền", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "Bạn không thể nghe nhạc vì bạn chưa xác nhận quyền!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // code nhấn back lần 2 sẽ thoát app.
    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(getBaseContext(), "Nhấn lại lần nữa để thoát", Toast.LENGTH_SHORT);
            backToast.show();
        }

        backPressedTime = System.currentTimeMillis();
    }

    // khi từ OfflineActivity back về MainActivity thì sẽ chạy vào thằng onActivityResult đầu tiên
    // hàm này sẽ kiểm tra trạng thái có thay đổi k
    // nếu có thì clear ds arrFav, và arrPlaylist
    // và tạo arrFav, arrPlaylist mới từ arrSong
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Boolean check_change_share = data.getBooleanExtra("check_change_share", false);
            btnSong.setText("Bài hát(" + arrSong.size() + ")");
            if (check_change_share) {
                arrFav.clear();
                arrPlaylist.clear();
                for (Music music : arrSong) {
                    if (music.isCheck_fav()) {
                        arrFav.add(music);
                    }
                    if (music.isCheck_playlist()) {
                        arrPlaylist.add(music);
                    }
                }
                btnPlaylist.setText("Playlist(" + arrPlaylist.size() + ")");
                btnFav.setText("Yêu thích(" + arrFav.size() + ")");
            }
        }
    }

    // xử lý sự kiện click button
    // 3 button này sẽ mở sang 1 màn hình OfflineActivity
    // nhưng dữ liệu truyền vào sẽ khác nhau ở mỗi sự kiện click button
    // phân biệt nó bằng cách truyền đi 1 cái KEY_SONGS, để bên màn hình kia biết dc bạn đang nhấn button nào
    // đồng thời cũng truyền đi 1 arrayList bài hát, tương ứng với bạn nhấn button nào.
    // vd: bạn nhấn button Favorites, bạn sẽ truyền qua màn hình kia gồm KEY_SONGS="favorites"
    // và 1 arrFav là ds bài hát bạn thích, ds này lúc này bạn đã add vào khi xử lý load data.
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSongs:
                // intent này trong phần: truyền và nhận dữ liệu giữa 2 màn hình
                // (truyền dữ liệu đi sau đó nhận lại dữ liệu trong hàm onActivityResult)
                intent = new Intent(this, OfflineActivity.class);
                KEY_SONGS = "songs";
                intent.putExtra(KEY, arrSong);
                intent.putExtra(KEY_SONGS, KEY_SONGS);
                startActivityForResult(intent, REQUEST_CODE);
                break;
            case R.id.btnPlaylists:
                intent = new Intent(this, OfflineActivity.class);
                KEY_SONGS = "playlists";
                intent.putExtra(KEY, arrPlaylist);
                intent.putExtra(KEY_SONGS, KEY_PLAYLISTS);
                startActivityForResult(intent, REQUEST_CODE);
                break;
            case R.id.btnFav:
                intent = new Intent(this, OfflineActivity.class);
                KEY_SONGS = "favorites";
                intent.putExtra(KEY, arrFav);
                intent.putExtra(KEY_SONGS, KEY_FAVORITES);
                startActivityForResult(intent, REQUEST_CODE);
                break;
        }
    }
}
