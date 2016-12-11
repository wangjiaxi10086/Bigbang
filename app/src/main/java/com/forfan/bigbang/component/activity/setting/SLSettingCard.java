package com.forfan.bigbang.component.activity.setting;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.forfan.bigbang.R;
import com.forfan.bigbang.baseCard.AbsCard;
import com.forfan.bigbang.component.activity.whitelist.SelectionDbHelper;
import com.forfan.bigbang.util.AESUtils;
import com.forfan.bigbang.util.ConstantUtil;
import com.forfan.bigbang.util.IOUtil;
import com.forfan.bigbang.util.LogUtil;
import com.forfan.bigbang.util.NativeHelper;
import com.forfan.bigbang.util.ToastUtil;
import com.forfan.bigbang.view.Dialog;
import com.forfan.bigbang.view.DialogFragment;
import com.forfan.bigbang.view.SimpleDialog;
import com.shang.commonjar.contentProvider.SPHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;

import static com.forfan.bigbang.util.ConstantUtil.BROADCAST_BIGBANG_MONITOR_SERVICE_MODIFIED;
import static com.forfan.bigbang.util.ConstantUtil.BROADCAST_CLIPBOARD_LISTEN_SERVICE_MODIFIED;
import static com.shang.commonjar.contentProvider.SPHelperImpl.MAINSPNAME;


/**
 * Created by penglu on 2015/11/23.
 */
public class SLSettingCard extends AbsCard {

    public SLSettingCard(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {
        mContext = context;

        LayoutInflater.from(context).inflate(R.layout.card_sl_setting, this);

        findViewById(R.id.default_setting_rl).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDefaultDialog();
            }
        });

        findViewById(R.id.save_setting_rl).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDialog();
            }
        });

        findViewById(R.id.load_setting_rl).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadDialog();
            }
        });

    }

    private void showLoadDialog() {
        SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.SimpleDialogLight) {

            @Override
            protected void onBuildDone(Dialog dialog) {
                dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                super.onBuildDone(dialog);
            }

            @Override
            public void onPositiveActionClicked(DialogFragment fragment) {
                // 这里是保持开启
                super.onPositiveActionClicked(fragment);
                loadSettings();
            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onCancel(dialog);
            }
        };
        builder.message(mContext.getString(R.string.load_setting_tips))
                .positiveAction(mContext.getString(R.string.confirm))
                .negativeAction(mContext.getString(R.string.cancel));
        DialogFragment fragment = DialogFragment.newInstance(builder);
        fragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), null);
    }


    private void showSaveDialog() {
        SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.SimpleDialogLight) {

            @Override
            protected void onBuildDone(Dialog dialog) {
                dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                super.onBuildDone(dialog);
            }

            @Override
            public void onPositiveActionClicked(DialogFragment fragment) {
                // 这里是保持开启
                super.onPositiveActionClicked(fragment);
                saveSettings();
            }

            @Override
            public void onNegativeActionClicked(DialogFragment fragment) {
                super.onNegativeActionClicked(fragment);
                saveOCR();
            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onCancel(dialog);
            }
        };
        builder.message(mContext.getString(R.string.save_setting_tips))
                .positiveAction(mContext.getString(R.string.save_other))
                .negativeAction(mContext.getString(R.string.only_save_ocr))
                .neutralAction(mContext.getString(R.string.cancel));
        DialogFragment fragment = DialogFragment.newInstance(builder);
        fragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), null);
    }


    private void showDefaultDialog() {
        SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.SimpleDialogLight) {

            @Override
            protected void onBuildDone(Dialog dialog) {
                dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                super.onBuildDone(dialog);
            }

            @Override
            public void onPositiveActionClicked(DialogFragment fragment) {
                // 这里是保持开启
                defaultSettings();
                super.onPositiveActionClicked(fragment);
            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onCancel(dialog);
            }
        };
        builder.message(mContext.getString(R.string.default_setting_tips))
                .positiveAction(mContext.getString(R.string.confirm))
                .negativeAction(mContext.getString(R.string.cancel));
        DialogFragment fragment = DialogFragment.newInstance(builder);
        fragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), null);
    }

    private void defaultSettings() {
        String ocr = SPHelper.getString(ConstantUtil.DIY_OCR_KEY, "");
        SPHelper.clear();
        SPHelper.save(ConstantUtil.DIY_OCR_KEY, ocr);
        SelectionDbHelper helper=new SelectionDbHelper(mContext);
        helper.deleteAll();

        mContext.sendBroadcast(new Intent(ConstantUtil.EFFECT_AFTER_REBOOT_BROADCAST));

        ToastUtil.show(R.string.effect_after_reboot);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Process.killProcess(Process.myPid());
            }
        },1500);
    }

    private void saveSettings(){
        String ocr = SPHelper.getString(ConstantUtil.DIY_OCR_KEY, "");
        SPHelper.save(ConstantUtil.DIY_OCR_KEY, "");

        File file = mContext.getFilesDir();
        File dbDir=new File(file.getParentFile(),"databases");
        File spDir=new File(file.getParentFile(),"shared_prefs");

        File desDir= new File(Environment.getExternalStorageDirectory()+File.separator+"quannengfenci/backup");

        copyFile(dbDir.getAbsolutePath(),new File(desDir,"databases").getAbsolutePath());
        copyFile(spDir.getAbsolutePath()+File.separator+"BigBang_sp_main.xml",new File(desDir,"shared_prefs").getAbsolutePath()+File.separator+"BigBang_sp_main.xml");

        SPHelper.save(ConstantUtil.DIY_OCR_KEY, ocr);
    }

    private void saveOCR(){
        String ocr = SPHelper.getString(ConstantUtil.DIY_OCR_KEY, "");
        String imei= NativeHelper.getImei(mContext);
        String cpu=NativeHelper.getCpuAbi();
        LogUtil.d("ocr="+ocr);
        LogUtil.d("imei="+imei);
        LogUtil.d("cpu="+cpu);

        File desOCRFile= new File(Environment.getExternalStorageDirectory()+File.separator+"quannengfenci/backup/OCR/ocr.txt");
        desOCRFile.getParentFile().mkdirs();
        String ocrEncrypt = AESUtils.encrypt(imei+cpu,ocr);
        InputStream inputStream=new ByteArrayInputStream(ocrEncrypt.getBytes());
        try {
            IOUtil.saveToFile(inputStream,desOCRFile);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void loadSettings(){
        String toast=mContext.getString(R.string.effect_after_reboot);
        File file = mContext.getFilesDir();
        File dbDir=new File(file.getParentFile(),"databases");
        File spDir=new File(file.getParentFile().getAbsolutePath()+"/shared_prefs/BigBang_sp_main.xml");

        File desDbDir= new File(Environment.getExternalStorageDirectory()+File.separator+"quannengfenci/backup/databases");
        File desSpFile= new File(Environment.getExternalStorageDirectory()+File.separator+"quannengfenci/backup/shared_prefs/BigBang_sp_main.xml");

        if (desDbDir.exists()) {
            deleteDirs(dbDir.getAbsolutePath());
            copyFile(desDbDir.getAbsolutePath(),dbDir.getAbsolutePath());
        }

        String ocrOrigin = SPHelper.getString(ConstantUtil.DIY_OCR_KEY, "");
        if (desSpFile.exists()) {
            SPHelper.clear();
            deleteDirs(spDir.getAbsolutePath());
            copyFile(desSpFile.getAbsolutePath(),spDir.getAbsolutePath());
        }


        String imei= NativeHelper.getImei(mContext);
        String cpu=NativeHelper.getCpuAbi();
        LogUtil.d("imei="+imei);
        LogUtil.d("cpu="+cpu);

        File desOCRFile= new File(Environment.getExternalStorageDirectory()+File.separator+"quannengfenci/backup/OCR/ocr.txt");
        if (!desOCRFile.exists()){
            return;
        }
        String ocrBackup=null;
        try {
            String ocrEncrypt = IOUtil.readString(desOCRFile);
            ocrBackup = AESUtils.decrypt(imei+cpu,ocrEncrypt);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String ocr="";
        if (!TextUtils.isEmpty(ocrOrigin)){
            ocr=ocrOrigin;
            toast=mContext.getString(R.string.restore_ocr_origin)+toast;
        }else if (!TextUtils.isEmpty(ocrBackup)){
            ocr=ocrBackup;
            toast=mContext.getString(R.string.restore_ocr_back)+toast;
        }
        saveOcrKeyWithSP(ocr);

        mContext.sendBroadcast(new Intent(ConstantUtil.EFFECT_AFTER_REBOOT_BROADCAST));
        ToastUtil.show(toast);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Process.killProcess(Process.myPid());
            }
        },1500);
    }

    private void saveOcrKeyWithSP(String ocrKey){
        SharedPreferences sp = mContext.getSharedPreferences(MAINSPNAME, Context.MODE_PRIVATE);
        sp.edit().putString(ConstantUtil.DIY_OCR_KEY,ocrKey).commit();
    }


    private void deleteDirs(String themePath){
        LinkedList<File> themeLinkedList=new LinkedList<File>();
        File themeDir=new File(themePath);
        if (!themeDir.exists()) {
            return;
        }else if (themeDir.isDirectory()){
            themeLinkedList.addAll(Arrays.asList(themeDir.listFiles()));
            while(!themeLinkedList.isEmpty())
                deleteContent(themeLinkedList.pollLast());
        }else {
            themeDir.delete();
        }
    }

    private void deleteContent(File file){
        LinkedList<File> themeLinkedList=new LinkedList<File>();
        if (file.isDirectory()) {
            themeLinkedList.addAll(Arrays.asList(file.listFiles()));
            while (!themeLinkedList.isEmpty()) {
                File subFile=themeLinkedList.pollLast();
                deleteContent(subFile);
            }
        }
        file.delete();
    }

    private void copyFile(String srcPath,String desPath){
        File srcDir=new File(srcPath);
        File desDir=new File(desPath);
        if (!srcDir.exists()){
            return;
        }
        if (srcDir.isDirectory()) {
            desDir.mkdirs();
            File[] files=srcDir.listFiles();
            for (int i=0;i<files.length;i++){
                File file = files[i];
                File des=new File(desDir,file.getName());
                copyFile(file.getAbsolutePath(),des.getAbsolutePath());
            }
        }else {
            desDir.getParentFile().mkdirs();
            IOUtil.copy(srcPath,desPath);
        }
    }

}
