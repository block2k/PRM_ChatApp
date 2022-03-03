package com.example.chatapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.GroupChatActivity;
import com.example.chatapp.R;
import com.example.chatapp.model.GroupChat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.ViewHolder> {

    private Context mContext;
    private List<GroupChat> groupChatList;

    public GroupChatAdapter(Context mContext, List<GroupChat> groupChatList) {
        this.mContext = mContext;
        this.groupChatList = groupChatList;
    }

    @NonNull
    @Override
    public GroupChatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.groups_chat_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupChatAdapter.ViewHolder holder, int position) {
        GroupChat groupChat = groupChatList.get(position);

        String groupId = groupChat.getGroupId();
        String groupIcon = groupChat.getGroupIcon();
        String groupTitle = groupChat.getGroupTitle();

        setGroupInfo(holder, groupIcon, groupTitle);

        getLastMessageGroupChat(holder, groupChat);

        // click open group message
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, GroupChatActivity.class);
                intent.putExtra("groupId", groupId);
                mContext.startActivity(intent);
            }
        });
    }

    private void getLastMessageGroupChat(@NonNull ViewHolder holder, GroupChat groupChat) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        // get last item (message) from child
        reference.child(groupChat.getGroupId()).child("Messages").limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // get data
                    String message = dataSnapshot.child("message").getValue().toString();
                    String timestamp = dataSnapshot.child("timestamp").getValue().toString();
                    String sender = dataSnapshot.child("sender").getValue().toString();
                    String type = dataSnapshot.child("type").getValue().toString();

                    // format time sent message
                    DateFormat simple = new SimpleDateFormat("MMM d, h:mm a");
                    long timeStamp = Long.parseLong(timestamp);
                    Date date = new Date(timeStamp);
                    timestamp = simple.format(date);

                    if (type.equals("text")) {
                        holder.last_msg.setText(message);
                    } else {
                        holder.last_msg.setText("photos");
                    }
                    // set data

                    // get info sender
                    DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Users");
                    reference1.orderByChild("id").equalTo(sender).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                String name = dataSnapshot1.child("username").getValue().toString();
                                holder.sender.setText(name + ": ");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setGroupInfo(@NonNull ViewHolder holder, String groupIcon, String groupTitle) {
        holder.group_name.setText(groupTitle);

        if ("default".equals(groupIcon)) {
            holder.group_image.setImageResource(R.drawable.ic_group);
        } else {
            Glide.with(mContext).load(groupIcon).into(holder.group_image);
        }
    }

    @Override
    public int getItemCount() {
        return groupChatList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView group_image;
        TextView group_name, sender, last_msg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            group_image = itemView.findViewById(R.id.group_profile_image);
            group_name = itemView.findViewById(R.id.group_sender);
            sender = itemView.findViewById(R.id.sender);
            last_msg = itemView.findViewById(R.id.group_message);
        }
    }
}
