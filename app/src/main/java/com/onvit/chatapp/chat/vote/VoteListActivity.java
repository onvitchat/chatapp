package com.onvit.chatapp.chat.vote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.model.Vote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class VoteListActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private String toRoom, uid;
    private RecyclerView recyclerView;
    List<Vote> voteList = new ArrayList<>();
    ArrayList<User> userList;
    private LinearLayout noVoteLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_list);
        noVoteLayout = findViewById(R.id.no_vote_layout);
        toolbar = findViewById(R.id.chat_toolbar);
        toolbar.setBackgroundResource(R.color.notice);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        String chatName;
        toRoom = getIntent().getStringExtra("room");
        chatName = toRoom +"투표목록";
        actionBar.setTitle(chatName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        userList = getIntent().getParcelableArrayListExtra("userList");

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FloatingActionButton make = findViewById(R.id.plus_vote);
        make.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VoteListActivity.this, VoteRegistrationActivity.class);
                intent.putExtra("room", toRoom);
                intent.putParcelableArrayListExtra("userList", userList);
                getIntent().putExtra("on", "on");
                startActivity(intent);
                overridePendingTransition(R.anim.fromleft, R.anim.toright);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getIntent().removeExtra("on");
        Map<String, Object> map = new HashMap<>();
        map.put(uid, true);
        FirebaseDatabase.getInstance().getReference().child("groupChat").child(toRoom).child("users").updateChildren(map);

        FirebaseDatabase.getInstance().getReference().child("Vote").child(toRoom).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                voteList.clear();
                for(DataSnapshot i : dataSnapshot.getChildren()){
                    Vote v = i.getValue(Vote.class);
                    v.setKey(i.getKey());

                    Map<String, Object> map = v.getContent();
                    Set<String> keySet = map.keySet();
                    Map<String,Object> list = new HashMap<>();
                    for(String k : keySet){
                        list = (Map<String, Object>) map.get(k);
                        break;
                    }
                    int f = 0;
                    Set<String> set = list.keySet();
                    for(String m : set){
                        if(m.equals(uid)){
                            f++;
                            break;
                        }
                    }
                    if(f==0){
                        continue;
                    }
                    voteList.add(v);
                }
                if(voteList.size()==0){
                    noVoteLayout.setVisibility(View.VISIBLE);
                }
                Collections.sort(voteList);
                recyclerView = findViewById(R.id.vote_recycler);
                recyclerView.setLayoutManager(new LinearLayoutManager(VoteListActivity.this));
                VoteListRecyclerAdapter voteListRecyclerAdapter = new VoteListRecyclerAdapter();
                recyclerView.setAdapter(voteListRecyclerAdapter);
                voteListRecyclerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (getIntent().getStringExtra("on") == null) {
            Map<String, Object> map = new HashMap<>();
            map.put(uid, false);
            FirebaseDatabase.getInstance().getReference().child("groupChat").child(toRoom).child("users").updateChildren(map);
            FirebaseDatabase.getInstance().getReference().child("lastChat").child(toRoom).child("existUsers").child(uid).child("exitTime").setValue(System.currentTimeMillis());
        }
    }
    //뒤로가기 눌렀을때
    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromright, R.anim.toleft);//화면 사라지는 방향
    }

    //툴바에 뒤로가기 버튼
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }



    class VoteListRecyclerAdapter extends RecyclerView.Adapter<VoteListRecyclerAdapter.VoteViewHolder> {
        Map<String, Boolean> size = new HashMap<>();
        SimpleDateFormat sd = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        int flag = 0;
        int over = 0;
        int ing = 0;
        public VoteListRecyclerAdapter(){
            sd.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        }

        @NonNull
        @Override
        public VoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vote, parent, false);
            return new VoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final VoteViewHolder holder, final int position) {
            size.clear();
            flag = 0;
            holder.ing.setVisibility(View.GONE);
            holder.title.setText(voteList.get(position).getTitle());
            holder.pCount.setCompoundDrawablesWithIntrinsicBounds( 0, 0, 0, 0);
            Map<String, Object> map = voteList.get(position).getContent();
            Set<String> keys = map.keySet();
            Iterator<String> it = keys.iterator();
            while (it.hasNext()) {
                String key1 = it.next();
                Map<String, Object> map2 = (Map<String, Object>) map.get(key1);
                Set<String> keys2 = map2.keySet();
                Iterator<String> it2 = keys2.iterator();
                while (it2.hasNext()) {
                    String key2 = it2.next();
                    boolean b = (boolean) map2.get(key2);
                    if(b){
                        size.put(key2, true);
                        if(key2.equals(uid)){
                            flag = 1;
                        }
                    }
                }
            }
            final Date date = new Date(voteList.get(position).getDeadline());
            final String end = sd.format(date);
            String join;
            if(flag==0){
                join = size.size()+"명 참여 / 참여안함";
            }else{
                join = size.size()+"명 참여 / 참여완료";
            }
            holder.pCount.setText(join);
            holder.time.setText(end+"에 마감");
            long today = new Date().getTime();
            if(today - voteList.get(position).getDeadline()>=0){
                ing++;
                if(ing==1){
                    holder.ing.setVisibility(View.VISIBLE);
                    holder.ing.setText("완료된 투표");
                    ing++;
                }
            }else{
                over++;
                if(over==1){
                    holder.ing.setVisibility(View.VISIBLE);
                    holder.ing.setText("진행중인 투표");
                }
                over++;
            }

            holder.linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String vote_key = voteList.get(position).getKey();
                    Intent intent = new Intent(VoteListActivity.this, VoteActivity.class);
                    intent.putParcelableArrayListExtra("userList", userList);
                    getIntent().putExtra("on","on");
                    intent.putExtra("key", vote_key);
                    intent.putExtra("room", toRoom);

                    Date d = new Date();
                    long todayTime = d.getTime();
                    long endDayTime = voteList.get(position).getDeadline();
                    if(endDayTime-todayTime<0){
                        intent.putExtra("flag", "over");
                    }else{
                        intent.putExtra("flag", "ing");
                    }
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return voteList.size();
        }


        private class VoteViewHolder extends RecyclerView.ViewHolder {
            TextView title, pCount, time, ing;
            LinearLayout linearLayout;

            public VoteViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.vote_title);
                pCount = v.findViewById(R.id.vote_person);
                time = v.findViewById(R.id.vote_time);
                ing = v.findViewById(R.id.ing_vote);
                linearLayout = v.findViewById(R.id.vote_layout);
            }
        }
    }
}
