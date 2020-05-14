package com.onvit.chatapp.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class UserMap {
    private static HashMap<String, User> userMap;
    private static List<User> userList;
    private static List<ChatModel.Comment> newComments;
    private static ValueEventListener valueEventListener;
    private static String uid;
    private static long init;

    public static void setInit(long i){
        init = i;
    }

    public static long getInit(){
        return init;
    }

    // ===================내 uid =======================//
    public static String getUid() {
        return uid;
    }

    public static void setUid(String uid2) {
        uid = uid2;
    }
    // ================================================//
    // ================ app 전체 사용자 정보 =================//
    public static void getUserMap() {
        final long startTime;

        startTime = System.currentTimeMillis();
        if (userMap == null) {
            userMap = new HashMap<>();
        }
        if (userList == null) {
            userList = new ArrayList<>();
        }
        //이름순으로 받아옴.
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                User my = new User();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    User user = dataSnapshot1.getValue(User.class);
                    userMap.put(dataSnapshot1.getKey(), user);
                    if(user.getUid().equals(uid)){
                        my = user;
                    }else{
                        userList.add(user);
                    }
                }
                userList.add(0,my);
                long endTime = System.currentTimeMillis();

                Log.d("유저시간차이", (endTime - startTime) / 1000 + "초");
                Log.d("유저시간차이", (endTime - startTime) + "초");
                Log.d("유저시간차이", userMap.size() + "명");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        FirebaseDatabase.getInstance().getReference().child("Users").orderByChild("userName").addValueEventListener(valueEventListener);
    }
    public static HashMap<String, User> getInstance() {
        return userMap;
    }

    public static List<User> getUserList() {
        return userList;
    }

    // ================================================================================================ //


    // ==============================채팅방 진입시 해당 채팅방 내용 =================================== //

    public static void clearComments() {
        if (newComments != null) {
            newComments.clear();
        }
    }

    public static List<ChatModel.Comment> getComments() {
        return newComments;
    }

    public static void setComments(List<ChatModel.Comment> comments) {
        newComments = comments;
    }

    // ====================================================================================== //

    // =====================================모든 정보 초기화 =========================================== //
    public static void clearApp() {
        if (valueEventListener != null) {
            FirebaseDatabase.getInstance().getReference().child("Users").removeEventListener(valueEventListener);
        }
        if (userList != null) {
            userList.clear();
        }
        if (userMap != null) {
            userMap.clear();
        }
        if (newComments != null) {
            newComments.clear();
        }
        if (uid != null) {
            uid = null;
        }
    }
}
