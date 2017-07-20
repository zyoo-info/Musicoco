package com.duan.musicoco.service;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.duan.musicoco.aidl.PlayControlImpl;
import com.duan.musicoco.aidl.Song;
import com.duan.musicoco.app.manager.MediaManager;
import com.duan.musicoco.db.DBMusicocoController;
import com.duan.musicoco.db.DBSongInfo;
import com.duan.musicoco.db.MainSheetHelper;
import com.duan.musicoco.preference.PlayPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DuanJiaNing on 2017/7/12.
 */

class ServiceInit {

    private final PlayControlImpl control;
    private final MediaManager manager;
    private final PlayPreference preference;
    private final DBMusicocoController dbController;

    private Context context;

    public ServiceInit(Context context, PlayControlImpl control, MediaManager manager, PlayPreference preference, DBMusicocoController dbController) {
        this.context = context;
        this.control = control;
        this.manager = manager;
        this.preference = preference;
        this.dbController = dbController;

    }

    public void start() {

        initData();

        initPlayList();

        initPlayMode();

        initCurrentSong();

    }

    //恢复上次播放状态
    private void initCurrentSong() {
        try {
            Song song = null;
            PlayPreference.CurrentSong cur = preference.getSong();
            if (cur != null && cur.path != null) {
                song = new Song(cur.path);
            }

            List<Song> songs = control.getPlayList();

            if (songs != null && songs.size() > 0) {
                if (song == null) {  //配置文件没有保存【最后播放曲目】信息（通常为第一次打开应用）
                    song = songs.get(0);
                } else { //配置文件有保存
                    if (!songs.contains(song)) { //确认服务端有此歌曲
                        song = songs.get(0);
                    }
                }

                // songChanged 将被回调
                control.setCurrentSong(song);
                Log.d("musicoco service init", "onCreate: current song: " + song.path);

                int pro = cur.progress;
                if (pro >= 0) {
                    control.seekTo(pro);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //设置循环模式
    private void initPlayMode() {
        int cm = preference.getPlayMode();
        control.setPlayMode(cm);
    }

    //设置播放列表
    // 配置文件无法跨进程共享，同步工作由客户端负责，服务端只在首次启动时读取
    private void initPlayList() {
        try {

            int sheetID = preference.getSheetID();
            List<DBSongInfo> list;
            if (sheetID < 0) {
                MainSheetHelper msh = new MainSheetHelper(context, dbController);
                list = msh.getMainSheetSongInfo(sheetID);
            } else {
                list = dbController.getSongInfos(sheetID);
            }
            control.setPlayList(getSongs(list), 0, sheetID);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private List<Song> getSongs(List<DBSongInfo> dbSongInfos) {
        List<Song> songs = new ArrayList<>();
        for (DBSongInfo info : dbSongInfos) {
            Song song = new Song(info.path);
            songs.add(song);
        }
        return songs;
    }

    private void initData() {
        manager.refreshData();
    }
}
