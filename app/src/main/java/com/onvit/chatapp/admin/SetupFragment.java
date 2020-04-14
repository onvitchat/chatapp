package com.onvit.chatapp.admin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.onvit.chatapp.LoginActivity;
import com.onvit.chatapp.MainActivity;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.UserMap;
import com.onvit.chatapp.util.PreferenceManager;
import com.onvit.chatapp.util.Utiles;

import java.util.HashMap;
import java.util.Map;

public class SetupFragment extends Fragment implements View.OnClickListener {
    private Toolbar chatToolbar;
    private AppCompatActivity activity;
    private User user;

    public SetupFragment(User u) {
        this.user = u;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup, container, false);
        chatToolbar = view.findViewById(R.id.chat_toolbar);
        activity = (MainActivity) getActivity();
        activity.setSupportActionBar(chatToolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setTitle("설정");

        TextView logout = view.findViewById(R.id.logout);
        TextView admin = view.findViewById(R.id.admin);
        final Switch notify = view.findViewById(R.id.notify);
        TextView invite = view.findViewById(R.id.invite);

        logout.setOnClickListener(this);
        admin.setOnClickListener(this);
        invite.setOnClickListener(this);

        notify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).getInt("vibrate", 0) == 0) {
                    activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).edit().putInt("vibrate", 1).apply();
                    notify.setChecked(false);
                    Utiles.customToast(activity, "앱의 알림이 해제되었습니다.").show();
                } else {
                    activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).edit().putInt("vibrate", 0).apply();
                    notify.setChecked(true);
                    Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(700);
                    Utiles.customToast(activity, "앱의 알림이 설정되었습니다.").show();
                }

            }
        });

        if (activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).getInt("vibrate", 0) == 0) {
            notify.setChecked(true);
        } else {
            notify.setChecked(false);
        }

        return view;
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.logout:
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("대한지역병원협의회");
                builder.setMessage("로그아웃을 하시겠습니까?");
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        Map<String, Object> map = new HashMap<>();
                        map.put("pushToken", "");
                        FirebaseDatabase.getInstance().getReference().child("Users").child(uid).updateChildren(map);
                        NotificationManagerCompat.from(activity).cancelAll();
                        UserMap.clearApp();
                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.putExtra("logOut", "logOut");
                        PreferenceManager.clear(activity);
                        startActivity(intent);
                        activity.finish();
                    }
                }).setNegativeButton("취소",null);
                AlertDialog a = builder.create();
                a.show();
                break;
            case R.id.admin:
                intent = new Intent(activity, AdminActivity.class);
                startActivity(intent);
                break;
            case R.id.invite:
                intent = new Intent(activity, InviteActivity.class);
                startActivity(intent);
                break;
        }
    }
}
