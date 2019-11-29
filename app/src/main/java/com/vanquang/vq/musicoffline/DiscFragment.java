package com.vanquang.vq.musicoffline;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.vanquang.vq.musicoffline.OfflineActivity.KEY_LIST_SONG;

public class DiscFragment extends Fragment {
    View view;
    public static CircleImageView imgAvatarSong; // imageView bo tròn ảnh (thư viện)
    private RelativeLayout relativeLayout;
    public static ObjectAnimator objectAnimator; // đối tượng dùng đề xoay 1 view

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_disc, container, false);
        imgAvatarSong = view.findViewById(R.id.imgAvatarSong);
        relativeLayout = view.findViewById(R.id.background_disc);

        // thay đổi màu nền, vài ảnh của cái đĩa xoay(imgAvatarSong)
        if (KEY_LIST_SONG.equals("playlists")) {
            Log.d("EEE", "DiscFragment playlist");
            relativeLayout.setBackgroundResource(R.drawable.custom_background_playlists);
            imgAvatarSong.setImageResource(R.drawable.disc_playlist);
        }
        if (KEY_LIST_SONG.equals("favorites")) {
            Log.d("EEE", "DiscFragment favorites");
            relativeLayout.setBackgroundResource(R.drawable.custom_background_favorites);
            imgAvatarSong.setImageResource(R.drawable.disc_favorite);
        }

        // khởi tạo và set imgAvatarSong cho objectAnimator.
        objectAnimator = ObjectAnimator.ofFloat(imgAvatarSong, "rotation", 0f, 360f);
        objectAnimator.setDuration(13000);
        objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objectAnimator.setRepeatMode(ValueAnimator.RESTART);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.setTarget(imgAvatarSong);

        return view;
    }

    // hàm này mục đích là lấy ra cái ảnh của file mp3 đang phát hiện tại
    // lấy ảnh của  file mp3 bằng cách extra file mp3(có thể search google) - dùng thằng MediaMetadataRetriever
    // trong đó ta lấy dc byte[] data(chuyển nó về bitmap)
    public static void CreateMusic(int position, String disc) {
        Music music = OfflineActivity.arrMusic.get(position);

        String path = music.getPath();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);

        objectAnimator.start(); // bắt đầu cho xoay imgAvatarSong

        byte[] data = mmr.getEmbeddedPicture();
        if (data != null) {
            Bitmap bitmap_disc = BitmapFactory.decodeByteArray(data, 0, data.length);
            imgAvatarSong.setImageBitmap(bitmap_disc);

        } else {
            if (disc.equals("songs")){
                imgAvatarSong.setImageResource(R.drawable.disc_song);
            }
            else if (disc.equals("playlists")){
                imgAvatarSong.setImageResource(R.drawable.disc_playlist);
            }
            else{
                imgAvatarSong.setImageResource(R.drawable.disc_favorite);
            }
        }

        mmr.release();
    }
}
