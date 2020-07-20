package com.onvit.chatapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.PreferenceManager;
import com.onvit.chatapp.util.UserMap;
import com.onvit.chatapp.util.Utiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAuth firebaseAuth;
    private String text = null;
    private Uri uri = null;
    private String filePath;
    private int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        firebaseAuth = FirebaseAuth.getInstance();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0) // 한시간에 최대 한번 요청할 수 있음. 한시간의 캐싱타임을 가짐.
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        accessFirebase();
    }

    private void accessFirebase() {
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            Boolean updated = task.getResult();
                            versionCheck();
                            Log.d("원격", "Config params updated: " + updated);
                        } else {
                            AlertDialog d = Utiles.createLoadingDialog(SplashActivity.this, "서버에 연결중입니다.");
                            i++;
                            if (i == 10) {
                                d.dismiss();
                                AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
                                builder.setMessage("현재 서버가 불안정합니다. 잠시후 다시 시도해 주세요.");
                                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                });
                                AlertDialog a = builder.create();
                                a.setCancelable(false);
                                a.setCanceledOnTouchOutside(false);
                                a.show();
                            }
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    accessFirebase();
                                }
                            }).start();
                        }
                    }
                });
    }

    void versionCheck() {
        long versionCode = mFirebaseRemoteConfig.getLong("version_code");
        String updateMessage = mFirebaseRemoteConfig.getString("update_message");

        String serverKey = mFirebaseRemoteConfig.getString("serverKey");
        PreferenceManager.setString(SplashActivity.this, "serverKey", serverKey);

        PackageInfo p = null;
        try {
            p = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        long version = 0;
        if (p != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                version = p.getLongVersionCode();
            } else {
                version = p.versionCode;
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("버전정보를 받아올 수 없습니다.").setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        }
        Log.d("버전코드", version + "");
        if (versionCode != version) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(updateMessage).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.onvit.chatapp")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.onvit.chatapp")));
                    } finally {
                        finish();
                    }
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        } else {
            createNotificationChannel();
            initSplash();

        }
    }

    private void createNotificationChannel() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //알림팝업띄우려고 한거.
            //채널지울때 지우고 지운아이디로 생성하면 지웠던게 다시 복구됨. 다른아이디를 주어야 새로 생성됨.
            if (notificationManager.getNotificationChannel(getString(R.string.vibrate)) != null) {
                if (notificationManager.getNotificationChannel(getString(R.string.vibrate)).getImportance() == NotificationManager.IMPORTANCE_LOW
                        || notificationManager.getNotificationChannel(getString(R.string.vibrate)).getImportance() == NotificationManager.IMPORTANCE_DEFAULT) {
                    notificationManager.deleteNotificationChannel(getString(R.string.vibrate));
                }
            }
            NotificationChannel channel = new NotificationChannel(getString(R.string.vibrate2),
                    "진동",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setVibrationPattern(new long[]{0, 500}); // 진동없애는거? 삭제하고 다시 깔아야 적용.
            channel.enableVibration(true);

            notificationManager.createNotificationChannel(channel);

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(getString(R.string.noVibrate),
                    "무음",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setVibrationPattern(new long[]{0}); // 진동없애는거? 삭제하고 다시 깔아야 적용.
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initSplash() {
        UserMap.clearApp();
        //로그인정보 없으면 로그인페이지로
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            firebaseAuth.signOut();
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            //로그인정보 있으면 상황에 따라 해당페이지로
        } else {
            final String uid = firebaseUser.getUid();
            deleteChat();
            Log.d("아이디", uid);

            FirebaseDatabase.getInstance().getReference().child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user == null) {
                        firebaseAuth.signOut();
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        UserMap.setUid(uid);
                        UserMap.getUserMap();
                        Intent intent = getIntent();
                        String action = intent.getAction();
                        String type = intent.getType();
                        if (Intent.ACTION_SEND.equals(action) && type != null) {
                            if ("text/plain".equals(type)) {
                                text = intent.getStringExtra(Intent.EXTRA_TEXT);
                                Intent intent1 = new Intent(SplashActivity.this, MainActivity.class);
                                intent1.putExtra("text", text);
                                intent1.putExtra("user", user);
                                intent1.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent1);
                                finish();
                            } else if (type.startsWith("image/")) {
                                Intent intent1 = new Intent(SplashActivity.this, MainActivity.class);
                                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                                final Uri convertUri = getConvertUri(uri);
                                if (convertUri != null) {
                                    intent1.putExtra("shareUri", convertUri);
                                    intent1.putExtra("filePath", filePath);
                                }
                                intent1.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent1.putExtra("user", user);
                                startActivity(intent1);
                                finish();
                            }
                        } else if (getIntent().getStringExtra("tag") != null) {
                            Intent intent2 = new Intent(SplashActivity.this, MainActivity.class);
                            intent2.putExtra("tag", getIntent().getStringExtra("tag"));
                            intent2.putExtra("user", user);
                            intent2.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent2);
                            finish();

                        } else {
                            Intent intent2 = new Intent(SplashActivity.this, MainActivity.class);
                            intent2.putExtra("user", user);
                            intent2.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent2);
                            finish();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    private Uri getConvertUri(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File file = getCacheDir();
            String fileName = System.currentTimeMillis() + ".jpeg";
            File tempFile = new File(file, fileName);
            OutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            getFilePath(tempFile.getAbsolutePath());
            return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", tempFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getFilePath(String absolutePath) {
        filePath = absolutePath;
    }


    private void deleteChat() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd", Locale.KOREA);
        final Date date = new Date();
        if (sdf.format(date).equals("01")) {
            long twoM = (24L * 60 * 60 * 1000 * 90);
            final long oldDate = date.getTime() - twoM;
            //두달지난거 삭제함.
            FirebaseDatabase.getInstance().getReference().child("groupChat").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        final String chatName = i.getKey();
                        if (chatName != null) {
                            FirebaseDatabase.getInstance().getReference().child("groupChat").child(chatName).child("comments")
                                    .orderByChild("timestamp")
                                    .endAt(oldDate).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Map<String, Object> map = new HashMap<>();
                                    List<String> deleteKey = new ArrayList<>();
                                    List<String> deleteKey2 = new ArrayList<>();
                                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                                        map.remove(item.getKey());
                                        ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                                        if(comment!=null){
                                            if (comment.getType().equals("img")) {
                                                deleteKey.add(item.getKey());
                                            }
                                            if (comment.getType().equals("file")) {
                                                int a = comment.getMessage().lastIndexOf("https");
                                                int b = comment.getMessage().substring(0, a).lastIndexOf(".");
                                                String ext = comment.getMessage().substring(0, a).substring(b + 1);
                                                deleteKey2.add(item.getKey() + "." + ext);
                                            }
                                        }
                                    }
                                    for (String d : deleteKey) {
                                        FirebaseStorage.getInstance().getReference().child("Image Files").child(chatName).child(d).delete();
                                    }
                                    for (String d : deleteKey2) {
                                        FirebaseStorage.getInstance().getReference().child("Document Files").child(chatName).child(d).delete();
                                    }
                                    FirebaseDatabase.getInstance().getReference().child("groupChat").child(chatName).child("comments").updateChildren(map);
                                    FirebaseDatabase.getInstance().getReference().child("Vote").child(chatName).updateChildren(map);
                                    Log.d(chatName + "지워진 채팅 수", dataSnapshot.getChildrenCount() + "개");
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }
}
