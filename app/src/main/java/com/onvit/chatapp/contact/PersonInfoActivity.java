package com.onvit.chatapp.contact;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.R;
import com.onvit.chatapp.SignUpActivity;
import com.onvit.chatapp.chat.BigPictureActivity;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.UserMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonInfoActivity extends AppCompatActivity implements View.OnClickListener {
    private Toolbar infoToolbar;
    private String info;
    private User user;
    private TextView name;
    private TextView tel;
    private TextView hospital;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_info);
        name = findViewById(R.id.name);
        tel = findViewById(R.id.tel);
        hospital = findViewById(R.id.hospital);
        uid = getIntent().getStringExtra("info");
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseDatabase.getInstance().getReference().child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    String uid2 = UserMap.getUid();
                    if (user.getUid().equals(uid2)) {
                        info = "내 정보";
                    } else {
                        info = user.getUserName() + "님의 정보";
                    }
                    infoToolbar = findViewById(R.id.chat_toolbar);
                    setSupportActionBar(infoToolbar);
                    ActionBar actionBar = getSupportActionBar();
                    if(actionBar!=null){
                        actionBar.setTitle(info);
                        actionBar.setDisplayHomeAsUpEnabled(true);
                    }
                    String newTel = user.getTel().substring(0, 3) + "-" + user.getTel().substring(3, 7) + "-" + user.getTel().substring(7);
                    String nameText = user.getUserName();
                    String telText = newTel;
                    String hospitalText = user.getHospital();
                    name.setText(nameText);
                    tel.setText(telText);
                    hospital.setText(hospitalText);

                    CircleImageView imageView = findViewById(R.id.info_img);
                    //사진에 곡률넣음.
                    if (user.getUserProfileImageUrl().equals("noImg")) {
                        Glide.with(PersonInfoActivity.this).load(R.drawable.standard_profile).into(imageView);
                    } else {
                        Glide.with(PersonInfoActivity.this).load(user.getUserProfileImageUrl()).into(imageView);
                    }
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(PersonInfoActivity.this, BigPictureActivity.class);
                            intent.putExtra("name", name.getText().toString());
                            intent.putExtra("uri", user.getUserProfileImageUrl());
                            startActivity(intent);
                        }
                    });


                    LinearLayout modify = findViewById(R.id.info_modi);
                    LinearLayout call = findViewById(R.id.info_call);
                    if (user.getUid().equals(uid2)) {
                        modify.setVisibility(View.VISIBLE);
                        modify.setOnClickListener(PersonInfoActivity.this);
                    } else {
                        call.setVisibility(View.VISIBLE);
                        call.setOnClickListener(PersonInfoActivity.this);
                    }
                } else {
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromtop, R.anim.tobottom);//화면 사라지는 방향
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.info_modi:
                intent = new Intent(PersonInfoActivity.this, SignUpActivity.class);
                intent.putExtra("modify", user);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
            case R.id.info_call:
                intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + user.getTel()));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                break;
        }
    }
}
