package com.danhtran12797.thd.musicoffline;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static com.danhtran12797.thd.musicoffline.OfflineActivity.KEY_LIST_SONG;
import static com.danhtran12797.thd.musicoffline.OfflineActivity.arrMusic;
import static com.danhtran12797.thd.musicoffline.OfflineActivity.delete_arrMusic_local;

public class ListSongFragment extends Fragment {

    // fragment ListSongFragment chứa 1 recyclerView
    // google recyclerView in fragment - sẽ hiểu hơn về recyclerView

    private static final String ARG_ARR_MUSIC = "MUSICS";

    View view;
    View bottomSheetLayout;
    public static RecyclerView recyclerView;
    private ArrayList<Music> arrMusics;
    public static MusicAdapter musicAdapter;
    private RelativeLayout relativeLayout;

    // để truyễn dữ liệu giữa fragment và activity thì dùng listener
    // chi tiết hơn thì search google
    public static FragmentContactListener listener;
    public static AddFavPlaylistListener listener1;
    public static DeleteSongListener listener2;
    // khi nhấn vào menu của 1 bài hát trong ds thì sẽ show ra 1 thông báo lựa chọn từ phía dưới lên là BottomSheetDialog
    private BottomSheetDialog bottomSheetDialog;

    long date_download = 0; // format nó sẽ dc ngày download xuống
    long duration; // thời gian phát
    Bitmap bitmap = null; // ảnh file mp3
    String genre = ""; // thể loại
    String parent = ""; // thư mục chưa file mp3 đó

    // khởi tạo interface
    // OfflineActivity sẽ implement interface
    // khi đó sẽ phải overite lại onInpuSent(position),
    // khi đó OfflineActivity sẽ nhận dc position của bài hát hiện tại rồi xử lý...
    public interface FragmentContactListener {
        void onInpuSent(int position);
    }

    public interface DeleteSongListener {
        void onInpuSent2(int position);
    }

    public interface AddFavPlaylistListener {
        void onInpuSent1(int position, boolean fav_play);
    }

    public static ListSongFragment newInstance(ArrayList<Music> list) {
        ListSongFragment fragment = new ListSongFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_ARR_MUSIC, list);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        arrMusics = new ArrayList<>();

        if (getArguments() != null) {
            arrMusics = (ArrayList<Music>) getArguments().getSerializable(ARG_ARR_MUSIC);
        }

        view = inflater.inflate(R.layout.fragment_list_song, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        relativeLayout = view.findViewById(R.id.background_list_music);

        musicAdapter = new MusicAdapter(arrMusics, getContext(), new MusicAdapter.OnClickMenuSongBottomSheet() { // khởi tạo và xử lý các nút trong bottomSheet
            @Override
            public void show_bottom_sheet(final int position) {

                boolean check_show_bs = false; // set trạng thái có show bottomSheet chưa
                final Music music = arrMusics.get(position);

                // khởi tạo view java từ xml
                bottomSheetLayout = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_layout, null);

                bottomSheetDialog = new BottomSheetDialog(getContext());
                bottomSheetDialog.setContentView(bottomSheetLayout);

                // ánh xạ các view trong bottomSheetLayout
                ImageView imgAvatar = bottomSheetLayout.findViewById(R.id.img_avatar_bs);
                TextView txtNameSong = bottomSheetLayout.findViewById(R.id.txt_title_bs);
                TextView txtNameAuthor = bottomSheetLayout.findViewById(R.id.txt_author_bs);
                ImageView imgLocation = bottomSheetLayout.findViewById(R.id.imgLocal_bss);
                LinearLayout playlist = bottomSheetLayout.findViewById(R.id.layout_playlist_bs);
                LinearLayout favorite = bottomSheetLayout.findViewById(R.id.layout_favorite_bs);
                LinearLayout infor = bottomSheetLayout.findViewById(R.id.layout_infor_bs);
                LinearLayout delete = bottomSheetLayout.findViewById(R.id.layout_delete_bs);

                // xử lý sự kiện
                playlist.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (music.isCheck_playlist()) {
                            Toast.makeText(getContext(), "Bài hát này đã có sẵn trong Playlist", Toast.LENGTH_SHORT).show();
                        } else {
                            // hàm sẽ dc overite lại tại OfflineActivity
                            // khi đó OfflineActivity sẽ nhận dc position và trạng playlist của bài hát dc chọn, để xử lý
                            listener1.onInpuSent1(position, true);
                        }
                        bottomSheetDialog.dismiss(); // đóng bottomSheet
                    }
                });
                favorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (music.isCheck_fav()) {
                            Toast.makeText(getContext(), "Bài hát này đã có sẵn trong Yêu Thích", Toast.LENGTH_SHORT).show();
                        } else {
                            // tương tự
                            listener1.onInpuSent1(position, false);
                        }
                        bottomSheetDialog.dismiss();
                    }
                });
                infor.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SimpleDateFormat format_date = new SimpleDateFormat("dd/MM/yyyy");
                        SimpleDateFormat format_duration = new SimpleDateFormat("mm:ss");

                        // show dialog thông tin của bài hát
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Thông Tin Bài Hát");
                        builder.setIcon(new BitmapDrawable(getResources(), bitmap));
                        builder.setMessage(getString(R.string.infor_song, music.getNameSong(), music.getNameSinger(), parent, format_date.format(date_download), format_duration.format(duration), genre));
                        builder.show();
                        bottomSheetDialog.dismiss();
                    }
                });
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File file = new File(music.getPath());
                        if (file.delete()) {
                            Toast.makeText(getContext(), "Đã xóa bài hát '" + music.getNameSong() + "' thành công", Toast.LENGTH_SHORT).show();
                            listener2.onInpuSent2(position);
                        }
                        bottomSheetDialog.dismiss();
                    }
                });

                txtNameSong.setText(music.getNameSong());
                txtNameAuthor.setText(music.getNameSinger());
                if (music.getIdLocal()) {
                    imgLocation.setImageResource(R.drawable.ic_phone_android);
                } else {
                    imgLocation.setImageResource(R.drawable.ic_arrow);
                }

                String path = music.getPath();
                File file = new File(path);

                // khi lấy dc file
                // kiểm tra file đó có tồn tại hay k
                // mục đích là để xử lý trường hợp: user ra ngoài xóa 1 file mp3 từ ứng dụng khác
                if (file.exists()) {
                    date_download = file.lastModified();
                    parent = file.getParentFile().getName();

                    // google: extra file mp3 in android
                    // ở đây dùng thằng MediaMetadataRetriever để lấy duration, genre, bitmap(ảnh file mp3)
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(path);

                    duration = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);

                    byte[] data = mmr.getEmbeddedPicture();
                    // nếu data khác null thì set ảnh qua bitmap lấy dc
                    // ngược lại set ảnh từ resource của ta
                    if (data != null) {
                        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    } else {
                        if (KEY_LIST_SONG.equals("playlists")) {
                            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.disc_song);
                        } else if (KEY_LIST_SONG.equals("playlists")) {
                            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.disc_playlist);
                        } else {
                            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.disc_favorite);
                        }
                    }
                    mmr.release();
                    imgAvatar.setImageBitmap(bitmap);
                } else { // file đã bị xóa nên set lại là k cho mở bottomSheet
                    check_show_bs = true; // set trạng thái show bottomSheet = true, có nghĩa là k cho show
                    Toast.makeText(getContext(), "Bài hát '" + music.getNameSong() + "' đã bị xóa khỏi bộ nhớ!", Toast.LENGTH_SHORT).show();
                    delete_arrMusic_local(position); // xóa phần tử trong a
                    arrMusic.remove(position);
                    setArrMusic(position);
                }
                if (!check_show_bs)
                    bottomSheetDialog.show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(musicAdapter);
        recyclerView.setHasFixedSize(true);

        // custom đường gạch chân dưới item recyclerView
        DividerItemDecoration dividerItemDecorationvider = new DividerItemDecoration(getContext(), new LinearLayoutManager(getContext()).getOrientation());
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.custom_divider);
        dividerItemDecorationvider.setDrawable(drawable);
        recyclerView.addItemDecoration(dividerItemDecorationvider); // gán vào

        // thay đổi màu nền
        if (KEY_LIST_SONG.equals("playlists")) {
            Log.d("EEE", "ListSongFragment playlist");
            relativeLayout.setBackgroundResource(R.drawable.custom_background_playlists);
        }
        if (KEY_LIST_SONG.equals("favorites")) {
            Log.d("EEE", "ListSongFragment favorites");
            relativeLayout.setBackgroundResource(R.drawable.custom_background_favorites);
        }

        // MusicAdapter truyền position cho ListSongFragment
        // ListSongFragment lại truyền position cho OfflineActivity
        musicAdapter.setOnItemClickListener(new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                listener.onInpuSent(position);
            }
        });

        Log.d("TTT", "ListSongFragment");

        return view;
    }

    // arrMusic thay đổi = arrMusics cũng đổi theo
    public void setArrMusic(int position) {
        musicAdapter.notifyItemRemoved(position);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentContactListener) {
            listener = (FragmentContactListener) context;
            listener1 = (AddFavPlaylistListener) context;
            listener2 = (DeleteSongListener) context;
        } else {
            throw new RuntimeException(context.toString() + "must implement FragmentContactListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        listener1 = null;
        listener2 = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
}

