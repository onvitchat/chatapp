package com.onvit.chatapp.chat;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.MainActivity;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.Img;
import com.onvit.chatapp.model.LastChat;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.UserMap;
import com.onvit.chatapp.util.Utiles;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;
import me.leolin.shortcutbadger.ShortcutBadger;

public class ChatFragment extends Fragment {
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM.dd", Locale.KOREA);
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm", Locale.KOREA);
    private ChatRecyclerViewAdapter chatRecyclerViewAdapter;
    private AppCompatActivity activity;
    private List<LastChat> chatModels = new ArrayList<>();
    private String uid;// 클라이언트uid
    private ToggleButton btn;
    private List<ChatModel.Comment> newComments = new ArrayList<>();
    private AlertDialog dialog;
    private Map<String, User> users = new HashMap<>();
    private ValueEventListener valueEventListener;
    private long allBadgeCount;
    private long startTime;
    private ArrayList<User> userInfoList = new ArrayList<>();
    public ChatFragment() {
        Log.d("프래그먼트", "ChatFragment");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("프래그먼트", "onCreateView");
        final View view = inflater.inflate(R.layout.fragment_chat, container, false);
        Toolbar chatToolbar = view.findViewById(R.id.chat_toolbar);
        activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(chatToolbar);
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle("단체 채팅");
            }
        }
        uid = UserMap.getUid();
        btn = view.findViewById(R.id.vibrate_btn);
        CircleImageView creatChat = view.findViewById(R.id.plus_chat);
        users = UserMap.getInstance();
        creatChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SelectPeopleActivity.class);
                intent.putExtra("uid", uid);
                ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromleft, R.anim.toright);
                startActivity(intent, activityOptions.toBundle());
            }
        });
        UserMap.setComments(newComments);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btn.isChecked()) {
                    btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_notifications_vibrate));
                    activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).edit().putInt("vibrate", 0).apply();
                    Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(700);
                    Utiles.customToast(getActivity(), "앱의 알림이 설정되었습니다.").show();
                } else {
                    btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_notifications_no_vibrate));
                    activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).edit().putInt("vibrate", 1).apply();
                    Utiles.customToast(getActivity(), "앱의 알림이 해제되었습니다.").show();
                }
            }
        });
        if (activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).getInt("vibrate", 0) == 0) {
            btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_notifications_vibrate));
            btn.setChecked(true);
        } else {
            btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_notifications_no_vibrate));
            btn.setChecked(false);
        }
        chatRecyclerViewAdapter = new ChatRecyclerViewAdapter();
        RecyclerView recyclerView = view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(chatRecyclerViewAdapter);
        LinearLayoutManager manager = new LinearLayoutManager(inflater.getContext());
        manager.setReverseLayout(true);
        manager.setStackFromEnd(true);
        recyclerView.setLayoutManager(manager);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("프래그먼트", "onResume");
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) { // 해당되는 chatrooms들의 키값들이 넘어옴.
                Log.d("chatFragment", "ㅇㅇ");
                allBadgeCount = 0;
                ArrayList<LastChat> chat = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    LastChat l = snapshot.getValue(LastChat.class);
                    Log.d("chatFragment", l.toString());
                    if (l.getExistUsers().get(uid) != null) {
                        chat.add(l);
                        allBadgeCount += l.getExistUsers().get(uid).getUnReadCount();
                    }
                }
                chatModels = (ArrayList<LastChat>) chat.clone();
                chatRecyclerViewAdapter.notifyDataSetChanged();
                //fcm눌러서 들어왓을때
                if (activity.getIntent().getStringExtra("tag") != null) {
                    String room = activity.getIntent().getStringExtra("tag");
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    View noticeView = LayoutInflater.from(getActivity()).inflate(R.layout.access, null);
                    builder.setView(noticeView);
                    dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setCancelable(false);
                    dialog.show();
                    UserMap.clearComments();
                    LastChat lastChat = new LastChat();
                    lastChat.setChatName(room);
                    int index = chatModels.indexOf(lastChat);
                    long initTime = chatModels.get(index).getExistUsers().get(uid).getInitTime();
                    long exitTime = chatModels.get(index).getExistUsers().get(uid).getExitTime();
                    activity.getIntent().removeExtra("tag");
                    getMessage(room, initTime, exitTime);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("lastChat").orderByChild("timestamp").addValueEventListener(valueEventListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("프래그먼트", "onStart");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("프래그먼트", "onPause");
        if (valueEventListener != null) {
            databaseReference.child("lastChat").removeEventListener(valueEventListener); // 이벤트 제거.
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("프래그먼트", "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("프래그먼트", "onDestroy");

    }


    private void getMessage(final String room, final long initTime, final long exitTime) {
        UserMap.setInit(initTime);

        databaseReference.child("groupChat").child(room).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userInfoList.clear();
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    if (!item.getKey().equals(uid)) {
                        userInfoList.add(users.get(item.getKey()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Map<String, Object> map = new HashMap<>();
        map.put(uid, true);
        databaseReference.child("groupChat").child(room).child("users").updateChildren(map);
        final long onTime = System.currentTimeMillis() + 300;
        // 방최초접속한 이후의 채팅들을 불러옴.
        databaseReference.child("groupChat").child(room).child("comments").orderByChild("timestamp").startAt(initTime)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                        long midTime = System.currentTimeMillis();
//                        Log.d("중간시간차이", ( midTime- startTime) / 1000 + "초");
                        Log.d("중간시간차이", (midTime - startTime) + "초");
                        for (DataSnapshot i : dataSnapshot.getChildren()) {
                            ChatModel.Comment c = i.getValue(ChatModel.Comment.class);
                            c.setKey(i.getKey());
                            newComments.add(c);
                        }
                        goChatRoom(room, exitTime, onTime);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void goChatRoom(String toRoom, long exitTime, long onTime) {
        Intent intent = new Intent(getActivity(), GroupMessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("toRoom", toRoom); // 방이름
        intent.putExtra("chatCount", newComments.size());// 채팅숫자
        intent.putExtra("exitTime", exitTime);
        intent.putExtra("onTime", onTime);
        intent.putParcelableArrayListExtra("userInfo", userInfoList);
        UserMap.setComments(newComments);
        ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(getActivity(), R.anim.frombottom, R.anim.totop);
        long endTime = System.currentTimeMillis();
//        Log.d("시간차이", (endTime - startTime) / 1000 + "초");
//        Log.d("시간차이", (endTime - startTime) + "초");
        Log.d("시간차이", newComments.size() + "개");
        Map<String, Object> map = new HashMap<>();
        map.put("existUsers/" + uid + "/unReadCount", 0);
        databaseReference.child("lastChat").child(toRoom).updateChildren(map);
        startActivity(intent, activityOptions.toBundle());
        dialog.dismiss();
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatRecyclerViewAdapter.ChatViewHolder> {
        private ChatRecyclerViewAdapter() {

        }

        @NonNull
        @Override
        public ChatRecyclerViewAdapter.ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ChatRecyclerViewAdapter.ChatViewHolder holder, final int position) {
            //position0번 부터 붙음
            Log.d("프래그먼트", "onBindViewHolder");
            holder.textView_count.setVisibility(View.INVISIBLE);

            holder.textView_title.setText(chatModels.get(position).getChatName());

            String userCount = chatModels.get(position).getExistUsers().size() + "";

            holder.textView_user_count.setText(userCount);
            //마지막으로 보낸 메세지

            String lastChat = chatModels.get(position).getLastChat();
            //방이 만들어지고 채팅친 내역이 없으면
            if (chatModels.get(position).getTimestamp() == 0) {
                holder.textView_timestamp.setVisibility(View.INVISIBLE);
                holder.textView_timestamp2.setVisibility(View.INVISIBLE);
                holder.textView_last_message.setText("");
            } else {
                if (chatModels.get(position).getExistUsers().get(uid).getInitTime() > chatModels.get(position).getTimestamp()) {
                    holder.textView_last_message.setText("");
                    holder.textView_timestamp.setText("");
                    holder.textView_timestamp2.setText("");
                } else {
                    holder.textView_last_message.setText(lastChat);
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                    simpleDateFormat2.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                    long unixTime = chatModels.get(position).getTimestamp();
                    Date date = new Date(unixTime);
                    Date date2 = new Date();
                    SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHHmm", Locale.KOREA);
                    sd.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                    String dS = sd.format(date);
                    String dS2 = sd.format(date2);
                    holder.textView_timestamp.setVisibility(View.VISIBLE);
                    holder.textView_timestamp2.setVisibility(View.VISIBLE);
                    if (dS2.substring(0, 8).equals(dS.substring(0, 8))) {
                        if (Integer.parseInt(dS.substring(8)) < 1200) {
                            holder.textView_timestamp.setText("오전");
                            holder.textView_timestamp2.setText(simpleDateFormat2.format(date));
                        } else {
                            holder.textView_timestamp.setText("오후");
                            holder.textView_timestamp2.setText(simpleDateFormat2.format(date));
                        }
                    } else {
                        holder.textView_timestamp.setText(simpleDateFormat.format(date));
                        holder.textView_timestamp2.setText(simpleDateFormat2.format(date));
                    }
                }
            }

            //안읽은 메세지 숫자
            final long count = chatModels.get(position).getExistUsers().get(uid).getUnReadCount();
            holder.textView_count.setText(count+"");
            if (count==0){
                holder.textView_count.setVisibility(View.INVISIBLE);
            }else{
                holder.textView_count.setVisibility(View.VISIBLE);
            }


            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    //중복클릭방지
                    if(Utiles.blockDoubleClick()){
                        return;
                    }
                    //채팅방들어갈때 안읽은 메세지들 모두 읽음으로 처리해서 넘어감.
                    UserMap.clearComments();
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    View noticeView = LayoutInflater.from(getContext()).inflate(R.layout.access, null);
                    builder.setView(noticeView);
                    dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setCancelable(false);
                    dialog.show();
                    startTime = System.currentTimeMillis();
                    long exitTime = chatModels.get(position).getExistUsers().get(uid).getExitTime();
                    long initTime = chatModels.get(position).getExistUsers().get(uid).getInitTime();
                    int badge = (int) (allBadgeCount - count);
                    Log.d("카운트", "all :" +allBadgeCount+", cut :"+count+", result :"+badge);
                    ShortcutBadger.applyCount(getContext(),badge);
                    getMessage(chatModels.get(position).getChatName(), initTime, exitTime);
                }

            });

        }

        @Override
        public int getItemCount() {
            return chatModels.size();
        }

        private class ChatViewHolder extends RecyclerView.ViewHolder {
            private TextView textView_title;
            private TextView textView_last_message;
            private TextView textView_timestamp;
            private TextView textView_timestamp2;
            private TextView textView_count;
            private TextView textView_user_count;

            private ChatViewHolder(View view) {
                super(view);
                textView_title = view.findViewById(R.id.chatitem_textview_title);
                textView_last_message = view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_timestamp = view.findViewById(R.id.chatitem_textview_timestamp);
                textView_timestamp2 = view.findViewById(R.id.chatitem_textview_timestamp2);
                textView_count = view.findViewById(R.id.chatitem_textview_count);
                textView_user_count = view.findViewById(R.id.user_count);
            }
        }
    }
}