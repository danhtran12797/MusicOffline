package com.danhtran12797.thd.musicoffline;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.thekhaeng.pushdownanim.PushDownAnim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;

import me.relex.circleindicator.CircleIndicator;

import static com.danhtran12797.thd.musicoffline.DiscFragment.objectAnimator;
import static com.danhtran12797.thd.musicoffline.ListSongFragment.musicAdapter;
import static com.danhtran12797.thd.musicoffline.ListSongFragment.recyclerView;
import static com.danhtran12797.thd.musicoffline.MainActivity.KEY;
import static com.danhtran12797.thd.musicoffline.MainActivity.KEY_FAVORITES;
import static com.danhtran12797.thd.musicoffline.MainActivity.KEY_PLAYLISTS;
import static com.danhtran12797.thd.musicoffline.MainActivity.KEY_SONGS;
import static com.danhtran12797.thd.musicoffline.MainActivity.SHARED_PREFERENCES_NAME;
import static com.danhtran12797.thd.musicoffline.MainActivity.arrSong;
import static com.thekhaeng.pushdownanim.PushDownAnim.MODE_SCALE;

public class OfflineActivity extends AppCompatActivity implements View.OnClickListener
        , ListSongFragment.FragmentContactListener, ListSongFragment.AddFavPlaylistListener,ListSongFragment.DeleteSongListener  {

    private ViewPager viewPager;
    public ViewPagerAdapter viewPagerAdapter;
    private Toolbar toolbar;

    private final String path_lyric_MP3 = Environment.getExternalStorageDirectory().getPath() + "/Zing MP3/Lyrics/";

    private ImageButton btnPre, btnPlay, btnLoop, btnNext, btnRandom, btnFav, btnPlaylist;
    private SeekBar seekBar;
    private TextView txtProgress;
    String timeTotal = "0:00";
    View thumbView;
    SimpleDateFormat format;
    Window window;

    private MediaPlayer mediaPlayer = null;
    public static ArrayList<Music> arrMusic;
    private Intent intent;

    // vị trí của bài hát đang phát(arrMusic)
    private int position = 0;

    private boolean check_onBackPressed = false;
    public static String KEY_LIST_SONG = "";

    // check lần đầu tiên phát 1 bài hát
    // nếu check_frist_play_music=false thì là chưa phát nhạc lần nào
    // check_frist_play_music=true là đã phát nhạc rồi
    private boolean check_frist_play_music = false;

    // chuỗi id fav, để lưu lại trong SharedPreferences, xử lý trong save_preferent
    // khi người dùng thoát
    private String id_fav = "";
    private String id_playlist = "";

    // kiểm tra người dùng có thay đổi gì k
    // vd: thêm 1 bài hát vào playlist, xóa 1 bài hát khỏi playlist,...
    // để khi vào onStop()/ kiểm tra có thay đổi
    // thì sẽ save_preferent()
    private boolean check_change_share = false;

    int cout_click = 0;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        format = new SimpleDateFormat("m:ss");

        initView();
        initToolbar();
        get_intent();

        // hàm khởi tạo thư viện cho button, và hiệu ứng khi click nó
        // sử dụng thư viện pushdown
        eventButton();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // khi người dùng tương tác với seekBar thì mediaPlayer sẽ chạy đến đoạn hát đó mediaPlayer.seekTo(...)
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!check_onBackPressed)
                    mediaPlayer.seekTo(seekBar.getProgress());
            }
        });

        ListSongFragment listSongFragment = ListSongFragment.newInstance(arrMusic);
        DiscFragment discFragment = new DiscFragment();
        LyricFragment lyricFragment = new LyricFragment();

        viewPagerAdapter.addFragment(listSongFragment);
        viewPagerAdapter.addFragment(discFragment);
        viewPagerAdapter.addFragment(lyricFragment);

        // vd: đang ở fragment 0 thì fragment 2 sẽ dc khởi tạo lại
        // nên không muốn cho nó khởi tạo lại
        viewPager.setOffscreenPageLimit(2); // k cho tạo mới khi lướt tới

        viewPager.setAdapter(viewPagerAdapter);

        // thư viện indicator
        CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);

        viewPagerAdapter.registerDataSetObserver(indicator.getDataSetObserver());
        mediaPlayer = new MediaPlayer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                musicAdapter.getFilter().filter(s);
                return false;
            }
        });

        return true;
    }

    private void eventButton() {
        PushDownAnim.setPushDownAnimTo(btnRandom, btnPlay, btnNext, btnLoop, btnPre, btnFav, btnPlaylist)
                .setScale(MODE_SCALE, 0.8f)
                .setDurationPush(PushDownAnim.DEFAULT_PUSH_DURATION)
                .setDurationRelease(PushDownAnim.DEFAULT_RELEASE_DURATION)
                .setInterpolatorPush(PushDownAnim.DEFAULT_INTERPOLATOR)
                .setInterpolatorRelease(PushDownAnim.DEFAULT_INTERPOLATOR)
                .setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void get_intent() {
        intent = getIntent();
        if (intent != null) {

            arrMusic = (ArrayList<Music>) intent.getSerializableExtra(KEY);
            KEY_LIST_SONG = intent.getStringExtra(KEY_SONGS);

            if (KEY_LIST_SONG.equals("songs")) {
                toolbar.setTitle("Songs(" + arrMusic.size() + ")");
            } else if (KEY_LIST_SONG.equals("playlists")) {
                setBackgroundListMusic(R.color.color_playlist);
                toolbar.setTitle("Playlists(" + arrMusic.size() + ")");
                toolbar.setBackgroundResource(R.color.color_playlist);
            } else {
                setBackgroundListMusic(R.color.color_favorites);
                toolbar.setTitle("Favorites(" + arrMusic.size() + ")");
                toolbar.setBackgroundResource(R.color.color_favorites);
                Log.d("DDD", "Favorites: " + arrMusic.size());
            }

            check_fav_playlist(this.position);
        }
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Music Offline");
        setSupportActionBar(toolbar);

        // show nút mũi tên thoát trên thanh toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // sự kiện thoát, trả dữ liệu về cho MainActivity
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // intent.putExtra("arrMusic",arrMusic);
                intent.putExtra("check_change_share", check_change_share);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void initView() {
        // khởi tạo thumbView
        thumbView = LayoutInflater.from(this).inflate(R.layout.layout_seekbar_thumb, null, false);

        viewPager = findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        seekBar = findViewById(R.id.seekBar);

        btnRandom = findViewById(R.id.btnRandom);
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnPre = findViewById(R.id.btnPre);
        btnLoop = findViewById(R.id.btnLoop);
        btnPlaylist = findViewById(R.id.imgPlaylist);
        btnFav = findViewById(R.id.imgFav);

        // thiết lập giá trị ban đầu cho btnFav, btnPlaylist, btnLoop, btnRandom
        // false: người dùng chưa nhấn vào nút đó.
        btnFav.setTag(false);
        btnPlaylist.setTag(false);

        // button lặp lại bài hát
        btnLoop.setTag(false);
        // button phát ngẫu nhiên bài hát
        btnRandom.setTag(false);

        txtProgress = thumbView.findViewById(R.id.tvProgress);

        // thiết lập hình dạng mới cho seekBar, với progress=0
        seekBar.setThumb(getThumb(0));
    }

    //custom seekbar với giá trị progress truyền vào.
    public Drawable getThumb(int progress) {
        txtProgress.setText(format.format(progress) + "/" + timeTotal);

        thumbView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Bitmap bitmap = Bitmap.createBitmap(thumbView.getMeasuredWidth(), thumbView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        thumbView.layout(0, 0, thumbView.getMeasuredWidth(), thumbView.getMeasuredHeight());
        thumbView.draw(canvas);

        return new BitmapDrawable(getResources(), bitmap);
    }

    //check is fav, playlist.Then set image.
    public void check_fav_playlist(int position) {
        if (arrMusic.size() != 0) {
            Music music = arrMusic.get(position);

            if (music.isCheck_fav()) {
                btnFav.setTag(true);
                btnFav.setImageResource(R.drawable.ic_favorite_red);
            } else {
                btnFav.setTag(false);
                btnFav.setImageResource(R.drawable.ic_favorite);
            }
            if (music.isCheck_playlist()) {
                btnPlaylist.setTag(true);
                btnPlaylist.setImageResource(R.drawable.ic_playlist_blu);
            } else {
                btnPlaylist.setTag(false);
                btnPlaylist.setImageResource(R.drawable.ic_playlist);
            }
        }
    }

    //set color statusBar
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setBackgroundListMusic(int color) {
        window = this.getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, color));
    }

    // tạo chuỗi id favorites, playlist để SharedPreferences lưu xuống.
    public void get_string_id(ArrayList<Music> arr) {
        //Log.d("III","arr: "+arr.size());
        if (arr.size() != 0) {
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i).isCheck_playlist()) {
                    if (i == 0) {
                        id_playlist += arr.get(i).getId();
                    } else {
                        id_playlist += "-" + arr.get(i).getId();
                    }
                }
                if (arr.get(i).isCheck_fav()) {
                    if (i == 0) {
                        id_fav += arr.get(i).getId();
                    } else {
                        id_fav += "-" + arr.get(i).getId();
                    }
                }
            }
        }
    }

    // lưu chuỗi id favorites, playlists vào sharedPreference
    public void save_preferent() {
        get_string_id(arrSong);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(KEY_FAVORITES, id_fav);
        editor.putString(KEY_PLAYLISTS, id_playlist);

        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (check_change_share)
            save_preferent();
    }

    @Override
    public void onBackPressed() {
        //intent.putExtra("arrMusic",arrMusic);
        intent.putExtra("check_change_share", check_change_share);
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // người dùng quay lại màn hình MainActivity
        check_onBackPressed = true;
        stopPlayer();
    }

    // mỗi 0.5s update lại seekbar
    // và kiểm tra người dùng có back(check_onBackPressed=true)
    // thì dừng handler
    // dừng phát nhạc
    public void updateTimeSong() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (check_onBackPressed) {
                    handler.removeCallbacks(this);//dừng handler
                    stopPlayer();
                } else {
                    seekBar.setThumb(getThumb(mediaPlayer.getCurrentPosition())); //thiết lập Thumb cho seekbar
                    //txtTimeSong.setText(format.format(mediaPlayer.getCurrentPosition()));
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            }
        }, 100);
    }

    public void setTimeTotal() {
        // time tổng 1 bài hát. Vd: 4:24
        timeTotal = format.format(mediaPlayer.getDuration());
        // set Max cho seerBar
        seekBar.setMax(mediaPlayer.getDuration());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        if (arrMusic.size() == 0) {
            Toast.makeText(this, "NO SONGS!!!", Toast.LENGTH_SHORT).show();
            btnFav.setImageResource(R.drawable.ic_favorite);
            btnPlaylist.setImageResource(R.drawable.ic_playlist);
            toolbar.setTitle("NO SONGS!!!");
            return;
        }

        switch (v.getId()) {
            case R.id.btnPlay:
                // kiểm tra lần đầu phát nhạc
                // nếu check_frist_play_music=false thì là chưa phát nhạc lần nào
                // check_frist_play_music=true là đã cho phát nhạc rồi(đã chạy mediaPlayer.start())
                if (!check_frist_play_music) { // cho phát random 1 bài hát
                    position = new Random().nextInt(arrMusic.size());
                    check_frist_play_music = true;
                    btnPlay.setImageResource(R.drawable.ic_pause);
                    Log.d("FFF", "check_frist_play_music");
                    startPlayer();
                } else {
                    // nếu nhạc đang phát thì cho dừng nhạc, dừng xoay đĩa, dừng VuMeterView
                    // VuMeterView(thằng này là cái thanh nhạc lên xuống ở mỗi item recylerView khi phát nhạc)
                    if (mediaPlayer.isPlaying()) {
                        Log.d("FFF", "isPlaying");
                        objectAnimator.pause();
                        musicAdapter.setCheckPause(true); // pause VuMeterView
                        btnPlay.setImageResource(R.drawable.ic_play);
                        mediaPlayer.pause();
                    } else { // ngược lại
                        Log.d("FFF", "else");
                        objectAnimator.resume();
                        musicAdapter.setCheckPause(false); // pause VuMeterView (heart)
                        btnPlay.setImageResource(R.drawable.ic_pause);
                        mediaPlayer.start();
                    }
                }
                break;
            case R.id.btnPre:
                btnPlay.setImageResource(R.drawable.ic_pause);
                if (!check_frist_play_music) {
                    position = new Random().nextInt(arrMusic.size());
                    check_frist_play_music = true;
                    btnPlay.setImageResource(R.drawable.ic_pause);
                } else {
                    if (btnLoop.getTag().equals(false) && btnRandom.getTag().equals(false)) {
                        if (position == 0) {
                            position = arrMusic.size() - 2;
                        } else if (position == 1) {
                            position = -1;
                        } else
                            position -= 2;
                    }
                    if (btnLoop.getTag().equals(true)) {
                        position--;
                        if (position <= -1) {
                            position = arrMusic.size() - 1;
                        }
                    }
                }
                startPlayer();

                break;
            case R.id.btnLoop:
                if (btnLoop.getTag().equals(false)) {
                    btnLoop.setImageResource(R.drawable.ic_sync_blu);
                    btnLoop.setTag(true);
                } else {
                    btnLoop.setImageResource(R.drawable.ic_sync);
                    btnLoop.setTag(false);
                }
                btnRandom.setImageResource(R.drawable.ic_random);
                btnRandom.setTag(false);

                break;
            case R.id.btnNext:
                btnPlay.setImageResource(R.drawable.ic_pause);
                if (!check_frist_play_music) {
                    position = new Random().nextInt(arrMusic.size());
                    check_frist_play_music = true;
                    btnPlay.setImageResource(R.drawable.ic_pause);
                } else {
                    if (btnLoop.getTag().equals(true)) {
                        position++;
                        if (position >= arrMusic.size()) {
                            position = 0;
                        }
                    }
                }
                startPlayer();

                break;
            case R.id.btnRandom:
                if (btnRandom.getTag().equals(false)) {
                    btnRandom.setImageResource(R.drawable.ic_random_blu);
                    btnRandom.setTag(true);
                } else {
                    btnRandom.setImageResource(R.drawable.ic_random);
                    btnRandom.setTag(false);
                }
                btnLoop.setImageResource(R.drawable.ic_sync);
                btnLoop.setTag(false);

                break;
                // khi có thay đổi gì về 1 bài hát yêu thích hoặc playlist
            // thì sẽ thay đổi lại ở arrMusic: là ds những bài hát hiện tại, dc MainActivity gửi qua khi người dùng nhấn 1 trong 3 button
            // và thay đổi ở arrSong: chứa tổng ds các bài hát-dc khởi tạo ở MainActivity
            case R.id.imgFav:
                check_change_share = true;
                if (btnFav.getTag().equals(false)) {
                    btnFav.setTag(true);
                    btnFav.setImageResource(R.drawable.ic_favorite_red);
                    arrMusic.get(position).setCheck_fav(true);
                    arrSong.get(position).setCheck_fav(true);
                    Toast.makeText(this, "Đã thêm '" + arrMusic.get(position).getNameSong() + "' vào Yêu Thích", Toast.LENGTH_SHORT).show();
                } else {
                    btnFav.setTag(false);
                    btnFav.setImageResource(R.drawable.ic_favorite);
                    arrMusic.get(position).setCheck_fav(false);
                    arrSong.get(getIdMusic(this.position, arrSong)).setCheck_fav(false);
                    Toast.makeText(this, "Đã xóa '" + arrMusic.get(position).getNameSong() + "' từ Yêu Thích", Toast.LENGTH_SHORT).show();
                }
                check_fav_playlist(this.position);
                break;
            case R.id.imgPlaylist:
                // trạng thái có thay đổi
                check_change_share = true;
                // bài hát hiện tại chưa đưa vào playlist
                // khi nhấn vào thì set tag = true
                // set image mới cho button
                // set check_playlist = true

                if (btnPlaylist.getTag().equals(false)) {
                    btnPlaylist.setTag(true);
                    btnPlaylist.setImageResource(R.drawable.ic_playlist_blu);
                    // arrMusic là ds những bài hát hiện tại, dc MainActivity gửi qua khi người dùng nhấn 1 trong 3 button
                    arrMusic.get(position).setCheck_playlist(true);
                    arrSong.get(getIdMusic(this.position, arrSong)).setCheck_playlist(true); // thiết lập lại ds nhạc gốc(arrSong-khởi tạo ở MainActivity)
                    Toast.makeText(this, "Đã thêm '" + arrMusic.get(position).getNameSong() + "' vào Playlist", Toast.LENGTH_SHORT).show();
                } else {
                    btnPlaylist.setTag(false);
                    btnPlaylist.setImageResource(R.drawable.ic_playlist);
                    arrMusic.get(position).setCheck_playlist(false);
                    arrSong.get(getIdMusic(this.position, arrSong)).setCheck_playlist(false);
                    Toast.makeText(this, "Đã xóa '" + arrMusic.get(position).getNameSong() + "' từ Playlist", Toast.LENGTH_SHORT).show();
                }
                check_fav_playlist(this.position);
                break;
        }
    }

    // lấy ra vị trí của arrSong(bên MainActivity) tương ứng vói music.id của trong arrMusic bằng với music.id arrSong
    public int getIdMusic(int position, ArrayList<Music> arr) {
        String idMusic = arrMusic.get(position).getId();
        for (int i = 0; i < arr.size(); i++) {
            if (idMusic.equals(arr.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    // xử lý phát nhạc tại hàm này
    // nó sẽ tự động gọi lại hàm này khi nó phát hết 1 bài hát
    // đọc tiếp sẽ biết
    public void startPlayer() {
        // reset mediaPlayer về null
        stopPlayer();
        check_onBackPressed = false;
        // ở đây ta có 2 button tính năng: btnLoop(phát lại bài hát), btnRandom(phát ngẫu nhiên 1 bài hát trong list)
        // nếu btnLoop bật thì sẽ tắt btnRandom(ngược lại)
        // nếu k có vấn đề gì( k có nhấn 1 trong 2 button tính năng)
        // thì tắng position++ để phát đến bài tiếp theo, nếu position> arrMusic.size() thì set position = 0
        if (btnLoop.getTag().equals(false) && btnRandom.getTag().equals(false)) {
            position++;
            if (position >= arrMusic.size()) {
                position = 0;
            }
        }

        // nếu có nhấn nút random - thì position sẽ radom từ 0 -> size-1
        if (btnRandom.getTag().equals(true)) {
            position = new Random().nextInt(arrMusic.size());
        }

        // nếu có nhấn nút lặp lại thì giữ nguyên position
        // nhưng nếu đồng người dùng nhấn next thì positon++
        // hoặc người dùng nhấn Pre thì positon--
        // nên phải ktra xem nó có vượt quá size hay position bị âm
        if (btnLoop.getTag().equals(true)) {
            if (position >= arrMusic.size())
                position = 0;
            if (position < 0)
                position = 0;
        }

        // kiểm tra ds bài hát truyền từ MainActivity vào
        // nếu ==0 thì thông báo và thoát khỏi startPlayer()
        if (arrMusic.size() == 0) {
            toolbar.setTitle("NO SONGS!!!");
            objectAnimator.cancel(); // dừng quay view
            objectAnimator.clone();
            check_onBackPressed = true;
            Toast.makeText(this, "NO SONGS!!!", Toast.LENGTH_SHORT).show();
            return;
        }

        Music music = arrMusic.get(position);
        // kiểm tra file đó có tồn tại hay k
        // mục đích là để xử lý trường hợp: user ra ngoài xóa 1 file mp3 từ ứng dụng khác
        if (!new File(music.getPath()).exists()) {
            Toast.makeText(this, "Bài hát '" + music.getNameSong() + "' đã bị xóa khỏi bộ nhớ!", Toast.LENGTH_SHORT).show();
            Log.d("PPP", arrMusic.get(position).getNameSong());
            delete_arrMusic_local(position); // xóa arrSong(arrSong, arrFav, arrPlaylist) gốc
            arrMusic.remove(position);


            Fragment fragment = viewPagerAdapter.getItem(0);
            if (fragment instanceof ListSongFragment) {
                ((ListSongFragment) fragment).setArrMusic(position);
                position--;
                startPlayer();
            }
            return;
        }

        //String lyric_song= path_lyric_MP3+music.getId(); //đường dẫn lời nhạc zingmp3 bài hát hiện tại

        Fragment fragment = viewPagerAdapter.getItem(2);
        if (fragment instanceof LyricFragment) {
            ((LyricFragment) fragment).add_lyric_song(getArrLyric(music.getId(), music));
        }

        mediaPlayer = new MediaPlayer();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(music.getPath()));
        } catch (IOException e) {
            Log.d("AAAA", e.getMessage() + "danh_1");
        }

        try {
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
        } catch (IOException e) {
            //Toast.makeText(this, "prepare ERROR", Toast.LENGTH_SHORT).show();
            Log.d("AAAA", e.getMessage() + "danh_2");
        }

        // lắng nghe xem bài hát phát hết chưa
        // nếu hết rồi thì xử lý trong hàm này
        // là gọi lại startPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startPlayer();
            }
        });

        musicAdapter.setPosition(position);
        musicAdapter.setCheckPause(false);
        recyclerView.scrollToPosition(position); // recyclerView sẽ scroll đến position hiện tại

        mediaPlayer.start();

        check_fav_playlist(this.position);
        toolbar.setTitle(music.getNameSong());
        DiscFragment.CreateMusic(position, KEY_LIST_SONG);

        setTimeTotal();
        updateTimeSong();
    }


    // dừng phát nhạc, hủy mediaPlayer
    public void stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // xóa 1 music từ arrSong với file k tồn tại
    public static void delete_arrMusic_local(int position) {
        //Music song_delete = arrSong.get(position);
        String idMusic = arrMusic.get(position).getId();
        for (int i = 0; i < arrSong.size(); i++) {
            if (idMusic.equals(arrSong.get(i).getId())) {
                arrSong.remove(i);
                break;
            }
        }
    }

    // hàm này trả về arrayList các dòng của file lyric
    // ở đây mình chỉ hổ trợ đọc file lyric từ Zing Mp3
    // Environment.getExternalStorageDirectory().getPath() + "/Zing MP3/Lyrics/
    // tên của file lyric sẽ là cái id của bài hát thuộc Zing MP3
    public ArrayList<String> getArrLyric(String id, Music music) {
        ArrayList<String> arrLyric = new ArrayList<>();
        File file_lyric_zing = new File(path_lyric_MP3 + id);

        // vd: 1 dòng trong file lyric của MP3 sẽ là:
        // [00:01.47]Đừng khóc như thế
        // ở đây mình sẽ đọc từ vị trí 10 đến hết dòng đó.
        // là chỉ lấy ra 'Đừng khóc như thế'
        if (file_lyric_zing.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file_lyric_zing));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.length() == 0 || line.charAt(1) != '0')
                        continue;
                    arrLyric.add(line.substring(10));
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            arrLyric.add("Song: " + music.getNameSong());
            arrLyric.add("Artist: " + music.getNameSinger());
            arrLyric.add("No Lyric");
            arrLyric.add(getResources().getString(R.string.simle));
        }

        return arrLyric;
    }

    // even click item recyclerView
    // lick vào 1 item sẽ phát nhạc bài hát đó
    @Override
    public void onInpuSent(int position) {
        this.position = position;

        // this.position--; là để khi qua startPlayer();
        // trong hàm startPlayer() nó sẽ ++ khi k có vấn đề gì(k nhấn btnLoop hoặc btnRandom)
        // nên ở đây cho this.position--;
        this.position--;
        btnPlay.setImageResource(R.drawable.ic_pause);
        if (check_frist_play_music == false) {
            check_frist_play_music = true;
        }

        if (btnLoop.getTag().equals(true))
            this.position++;
        startPlayer();
    }

    @Override
    public void onInpuSent1(int position, boolean fav_play) {
        check_change_share = true;

        if (fav_play) {
            arrMusic.get(position).setCheck_playlist(true);
            arrSong.get(getIdMusic(position, arrSong)).setCheck_playlist(true);
            Toast.makeText(this, "Đã thêm '" + arrMusic.get(position).getNameSong() + "' vào Playlist", Toast.LENGTH_SHORT).show();
        } else {
            arrMusic.get(position).setCheck_fav(true);
            arrSong.get(getIdMusic(position, arrSong)).setCheck_fav(true);
            Toast.makeText(this, "Đã thêm '" + arrMusic.get(position).getNameSong() + "' vào Yêu Thích", Toast.LENGTH_SHORT).show();
        }
        if (this.position == position) {
            check_fav_playlist(position);
        }
    }

    @Override
    public void onInpuSent2(int position) {
        check_change_share = true;
        delete_arrMusic_local(position);
        arrMusic.remove(position);
        musicAdapter.notifyItemRemoved(position);

        if (this.position == position) {
            //check_fav_playlist(this.position);
            this.position--;
            startPlayer();
        }
    }
}
