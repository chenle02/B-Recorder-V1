package com.chenboda01.brecorderv1;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    MediaRecorder recorder;
    MediaPlayer player;
    File dir,currentFile;
    boolean recording=false;
    TextView status,title;
    LinearLayout list;
    long startMs=0;
    Handler h=new Handler();
    Runnable tick=new Runnable(){public void run(){if(recording){status.setText("Recording " + ((System.currentTimeMillis()-startMs)/1000) + "s");h.postDelayed(this,500);}}};

    public void onCreate(Bundle b){
        super.onCreate(b);
        dir=new File(getFilesDir(),"records");dir.mkdirs();
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);}        
        build();
        refresh();
    }

    void build(){
        ScrollView sv=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22,22,22,22);
        root.setBackgroundColor(Color.rgb(7,19,30));
        sv.addView(root);
        title=txt("B-Recorder V1",30,Color.WHITE,true);
        status=txt("Ready",18,Color.rgb(80,255,157),true);
        root.addView(title);root.addView(status);
        LinearLayout row=row();
        row.addView(btn("Record",v->startRec()));
        row.addView(btn("Stop",v->stopRec()));
        root.addView(row);
        list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        setContentView(sv);
    }

    TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(4,8,4,8);if(bold)t.setTypeface(null,1);return t;}
    Button btn(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(l);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(58),1);p.setMargins(5,5,5,5);b.setLayoutParams(p);return b;}
    LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);return r;}
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}

    void startRec(){
        if(recording)return;
        try{
            String name="rec_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".m4a";
            currentFile=new File(dir,name);
            recorder=new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(currentFile.getAbsolutePath());
            recorder.prepare();recorder.start();
            recording=true;startMs=System.currentTimeMillis();tick.run();
        }catch(Exception e){status.setText("Record failed. Permission?");}
    }

    void stopRec(){
        if(!recording)return;
        try{recorder.stop();}catch(Exception e){}
        try{recorder.release();}catch(Exception e){}
        recorder=null;recording=false;status.setText("Saved");refresh();
    }

    void play(File f){
        try{
            if(player!=null){player.stop();player.release();}
            player=new MediaPlayer();player.setDataSource(f.getAbsolutePath());player.prepare();player.start();status.setText("Playing " + f.getName());
        }catch(Exception e){status.setText("Play failed");}
    }

    void rename(File f){
        final EditText input=new EditText(this);input.setText(f.getName().replace(".m4a",""));
        new AlertDialog.Builder(this).setTitle("Rename").setView(input).setPositiveButton("OK",(d,w)->{String n=input.getText().toString().trim();if(n.length()==0)return;if(!n.endsWith(".m4a"))n+=".m4a";f.renameTo(new File(dir,n));refresh();}).setNegativeButton("Cancel",null).show();
    }

    void del(File f){f.delete();refresh();}

    void refresh(){
        list.removeAllViews();
        File[] files=dir.listFiles((d,n)->n.endsWith(".m4a"));
        if(files==null||files.length==0){list.addView(txt("No recordings yet.",16,Color.LTGRAY,false));return;}
        Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
        for(File f:files){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(12,12,12,12);box.setBackgroundColor(Color.rgb(19,40,56));TextView name=txt(f.getName(),17,Color.WHITE,true);box.addView(name);LinearLayout r=row();r.addView(btn("Play",v->play(f)));r.addView(btn("Rename",v->rename(f)));r.addView(btn("Delete",v->del(f)));box.addView(r);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,8,0,8);list.addView(box,p);}    
    }

    protected void onPause(){super.onPause();if(recording)stopRec();if(player!=null){try{player.stop();player.release();}catch(Exception e){}}}
}
