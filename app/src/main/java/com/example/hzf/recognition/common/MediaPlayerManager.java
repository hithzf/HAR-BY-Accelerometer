package com.example.hzf.recognition.common;

import android.media.MediaPlayer;

import com.example.hzf.recognition.R;

/**
 * Created by hzf on 2018/5/5.
 */

public class MediaPlayerManager {
    //播放音频API类：MediaPlayer
    private static MediaPlayer mMediaPlayer;
    //是否暂停
    private static boolean isPause;

    /**
     * filePath：文件路径
     *  播放声音
     */
    public static void playSound(String filePath, MediaPlayer.OnCompletionListener onCompletionListener) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            //设置一个error监听器
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                    mMediaPlayer.reset();
                    return false;
                }
            });
        } else {
            mMediaPlayer.reset();
        }

        try {
            mMediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(filePath);
            mMediaPlayer.setOnCompletionListener(onCompletionListener);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            LogUtil.d("MediaPlayerManager", "start() has executed...");
        } catch (Exception e) {

        }
    }

    /**
     *  暂停播放
     */
    public static void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) { //正在播放的时候
            mMediaPlayer.pause();
            isPause = true;
        }
    }

    /**
     *  重新播放
     */
    public static void resume() {
        if (mMediaPlayer != null && isPause) {
            mMediaPlayer.start();
            isPause = false;
        }
    }

    /**
     *  释放操作
     */
    public static void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            LogUtil.d(MediaPlayerManager.class.getSimpleName(), "release() has executed");
        }
    }
}