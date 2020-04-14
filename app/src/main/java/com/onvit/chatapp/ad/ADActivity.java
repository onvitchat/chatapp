package com.onvit.chatapp.ad;

import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.onvit.chatapp.R;

public class ADActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad);

        PhotoView imageView = findViewById(R.id.ad_img_view);
        String img = getIntent().getStringExtra("ad");
        Glide.with(this).load(Uri.parse(img)).placeholder(R.drawable.ic_shopping).apply(new RequestOptions().fitCenter()).into(imageView);
    }
}
