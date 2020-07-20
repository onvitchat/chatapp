package com.onvit.chatapp;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.onvit.chatapp.ad.ShoppingFragment;
import com.onvit.chatapp.admin.SetupFragment;
import com.onvit.chatapp.chat.ChatFragment;
import com.onvit.chatapp.chat.SelectGroupChatActivity;
import com.onvit.chatapp.contact.PeopleFragment;
import com.onvit.chatapp.model.LastChat;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.notice.NoticeFragment;
import com.onvit.chatapp.util.PreferenceManager;
import com.onvit.chatapp.util.UserMap;
import com.onvit.chatapp.util.Utiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final static int PERMISSION_REQUEST_CODE = 1000;
    BottomNavigationMenuView bottomNavigationMenuView;
    BottomNavigationView bottomNavigationView;
    private User user;
    private String uid;
    private ValueEventListener valueEventListener;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private Fragment chatFragment, peopleFragment, noticeFragment, adFragment, setFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        //유저없으면 로그인 페이지로
        if (firebaseAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            firebaseAuth.signOut();
            startActivity(intent);
            finish();
        }
        user = getIntent().getParcelableExtra("user");
        //uid설정
        uid = UserMap.getUid();

        //uid없으면 받아와서 설정.
        if (uid == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                uid = user.getUid();
                UserMap.setUid(uid);
            } else {
                Utiles.customToast(MainActivity.this, "인증오류").show();
                UserMap.clearApp();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra("logOut", "logOut");
                PreferenceManager.clear(MainActivity.this);
                startActivity(intent);
                finish();
            }
        }

        //개인정보 저장.
        PreferenceManager.setString(MainActivity.this, "name", user.getUserName());
        PreferenceManager.setString(MainActivity.this, "hospital", user.getHospital());
        PreferenceManager.setString(MainActivity.this, "phone", user.getTel());
        PreferenceManager.setString(MainActivity.this, "uid", user.getUid());


        noticeFragment = new NoticeFragment();
        chatFragment = new ChatFragment();
        peopleFragment = new PeopleFragment();
        adFragment = new ShoppingFragment();
        setFragment = new SetupFragment();

        getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        bottomNavigationView = findViewById(R.id.mainActivity_bottomNavigationView);
        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, new NoticeFragment()).commitAllowingStateLoss();


        //바텀네비게이션에 메뉴를 붙임.
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_notice:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, noticeFragment).commitAllowingStateLoss();
                        bottomNavigationMenuView.getChildAt(0).setEnabled(false);
                        return true;
                    case R.id.action_people:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, peopleFragment).commitAllowingStateLoss();
                        bottomNavigationMenuView.getChildAt(1).setEnabled(false);
                        return true;
                    case R.id.action_chat:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, chatFragment).commitAllowingStateLoss();
                        bottomNavigationMenuView.getChildAt(2).setEnabled(false);
                        return true;
                    case R.id.action_account:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, adFragment).commitAllowingStateLoss();
                        bottomNavigationMenuView.getChildAt(3).setEnabled(false);
                        return true;
                    case R.id.action_setup:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, setFragment).commitAllowingStateLoss();
                        bottomNavigationMenuView.getChildAt(4).setEnabled(false);
                        return true;

                }
                return false;
            }
        });
        bottomNavigationMenuView = (BottomNavigationMenuView) bottomNavigationView.getChildAt(0);


        //공유 텍스트 or 이미지 처리.
        if (getIntent().getStringExtra("text") != null || getIntent().getParcelableExtra("shareUri") != null) {
            String text = getIntent().getStringExtra("text");
            Uri uri = getIntent().getParcelableExtra("shareUri");
            String filePath = getIntent().getStringExtra("filePath");
            Intent intent1 = new Intent(MainActivity.this, SelectGroupChatActivity.class);
            intent1.putExtra("text", text);
            intent1.putExtra("shareUri", uri);
            intent1.putExtra("filePath", filePath);
            bottomNavigationView.setSelectedItemId(R.id.action_chat);
            startActivity(intent1);
        }

        requestPermission();
        passPushTokenToServer();

    }

    private void requestPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        final ArrayList<String> arrayPermission = new ArrayList<>();

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            arrayPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            arrayPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            arrayPermission.add(Manifest.permission.CAMERA);
        }
        if (arrayPermission.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View request = inflater.inflate(R.layout.request_permission_check, null);
            Button button = request.findViewById(R.id.ok);
            builder.setView(request);
            final AlertDialog a = builder.create();
            a.setCanceledOnTouchOutside(false);
            a.setCancelable(false);
            a.show();
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    a.dismiss();
                    String[] strArray = new String[arrayPermission.size()];
                    strArray = arrayPermission.toArray(strArray);
                    ActivityCompat.requestPermissions(MainActivity.this, strArray, PERMISSION_REQUEST_CODE);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length < 1) {
                    Utiles.customToast(this, "권한을 받아오는데 실패하였습니다.").show();
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                    return;
                }
                for (int i = 0; i < grantResults.length; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
                        if (!showRationale) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("앱의 원활한 사용을 위해 권한을 허용해야 합니다. 앱 정보로 이동합니다.\n [저장공간]권한을 허용해주세요.");
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("앱의 원활한 사용을 위해 권한을 허용해야 합니다.");
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermission();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        }
                    } else {
                        Utiles.customToast(this, "권한을 허용하였습니다.").show();
                        // Initialize 코드
                    }
                }


            }
            break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void passPushTokenToServer() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(MainActivity.this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String token = instanceIdResult.getToken();
                user.setPushToken(token);
                FirebaseDatabase.getInstance().getReference().child("Users").child(uid).setValue(user);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        requestNotificationPolicyAccess();

        //fcm눌러서 들어왓을때.
        if (getIntent().getStringExtra("tag") != null) {
            if (!getIntent().getStringExtra("tag").equals("notice")) {
                bottomNavigationView.setSelectedItemId(R.id.action_chat);
            } else {
                getIntent().removeExtra("tag");
            }
        }


        //바텀네이게이션 채팅방 메뉴에 안읽은 메세지 개수 표시.
        View v = bottomNavigationMenuView.getChildAt(2);
        BottomNavigationItemView itemView = (BottomNavigationItemView) v;
        final View badge = LayoutInflater.from(this).inflate(R.layout.notification_badge, itemView, true);
        final TextView badgeView = badge.findViewById(R.id.badge);
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("뱃지", "dd");
                int count = 0;
                for (final DataSnapshot item : dataSnapshot.getChildren()) {
                    final LastChat lastChat = item.getValue(LastChat.class);
                    if (lastChat.getExistUsers().get(uid) != null) {
                        count += lastChat.getExistUsers().get(uid).getUnReadCount();
                        Log.d("뱃지", count + "dd");
                    } else {
                        count += 0;
                    }
                }
                if (count > 0) {
                    String c = count + "";
                    Log.d("뱃지", c);
                    badgeView.setText(c);
                    badgeView.setVisibility(View.VISIBLE);
                } else {
                    badgeView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("lastChat").addValueEventListener(valueEventListener);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.logout) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("대한지역병원협의회");
            builder.setMessage("로그아웃을 하시겠습니까?");
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("pushToken", "");
                    FirebaseDatabase.getInstance().getReference().child("Users").child(uid).updateChildren(map);
                    NotificationManagerCompat.from(MainActivity.this).cancelAll();
                    UserMap.clearApp();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.putExtra("logOut", "logOut");
                    PreferenceManager.clear(MainActivity.this);
                    startActivity(intent);
                    finish();
                }
            }).setNegativeButton("취소", null);
            AlertDialog a = builder.create();
            a.show();

        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReference.child("lastChat").removeEventListener(valueEventListener); // 이벤트 제거.
        }
    }

    private void requestNotificationPolicyAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !isNotificationPolicyAccessGranted()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("앱 설정으로 이동합니다. \n[방해금지권한]을 허용해주세요.");
            builder.setMessage("해당 기종은 알림기능사용을 위해 해당 권한이 필요합니다.");
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(android.provider.Settings.
                            ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    private boolean isNotificationPolicyAccessGranted() {
        NotificationManager notificationManager = (NotificationManager)
                MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return true;
    }
}
