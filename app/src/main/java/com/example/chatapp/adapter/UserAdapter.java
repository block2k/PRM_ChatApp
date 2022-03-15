package com.example.chatapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.R;
import com.example.chatapp.model.Chat;
import com.example.chatapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private Context mContext;
    private List<User> mUsers;
    private boolean ischat;

    String lastMessage;

    public static final String YOU_UNSENT_A_MESSAGE = "You unsent a message";


    public UserAdapter(Context mContext, List<User> mUsers, boolean ischat) {
        this.mContext = mContext;
        this.mUsers = mUsers;
        this.ischat = ischat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new UserAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        User user = mUsers.get(position);

        setInfoUserWhoChatting(holder, user);

        checkUserIsBlocked(holder, firebaseUser, user);

        loadUserChatting(holder, user);

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, MessageActivity.class);
            intent.putExtra("userid", user.getId());
            intent.putExtra("blocked", user.getBlocked());
            mContext.startActivity(intent);
        });
    }

    private void loadUserChatting(@NonNull ViewHolder holder, User user) {
        if (ischat) {
            // display last message if in tab chats
            lastMessage(user, holder.last_msg);

            // display number of message not read
            getNumberOfMessageNotRead(user.getId(), holder.numberOfMessageNotRead);

            // display status if in tab chats
            if (user.getStatus().equals("online")) {
                holder.img_on.setVisibility(View.VISIBLE);
                holder.img_off.setVisibility(View.GONE);
            } else {
                holder.img_on.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
        } else {
            holder.last_msg.setVisibility(View.GONE);
            holder.img_on.setVisibility(View.GONE);
            holder.img_off.setVisibility(View.GONE);
        }
    }

    private void setInfoUserWhoChatting(@NonNull ViewHolder holder, User user) {
        holder.username.setText(user.getUsername());

        if ("default".equals(user.getImageURL())) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }
    }

    private void checkUserIsBlocked(@NonNull ViewHolder holder, FirebaseUser firebaseUser, User user) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseUser.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(user.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    if (dataSnapshot.exists()) {
                        holder.block_user.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView username, last_msg, numberOfMessageNotRead;
        public ImageView profile_image, block_user;
        private ImageView img_on;
        private ImageView img_off;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.group_sender);
            profile_image = itemView.findViewById(R.id.profile_image);
            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
            last_msg = itemView.findViewById(R.id.group_message);
            numberOfMessageNotRead = itemView.findViewById(R.id.number_of_chat_is_not_seen);
            block_user = itemView.findViewById(R.id.block_user);
        }
    }

    //check last message
    private void lastMessage(User user, TextView last_msg) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
//        lastMessage = "default";
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(user.getId()) ||
                            chat.getReceiver().equals(user.getId()) && chat.getSender().equals(firebaseUser.getUid())) {
                        if (chat.getMessage().equals(YOU_UNSENT_A_MESSAGE)) {
                            if (!firebaseUser.getUid().equals(chat.getSender())) {
                                chat.setMessage(user.getUsername() + " unsent a message");
                            }
                        } else if (chat.getMessage().startsWith("https://")) {
                            chat.setMessage(user.getUsername() + " sent you a photo");
                        }
                        lastMessage = chat.getMessage();
                    }
                }
//                switch (lastMessage) {
//                    case "default":
//                        last_msg.setText("No message");
//                        break;
//                    default:
                last_msg.setText(lastMessage);
//                }
//                lastMessage = "default";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getNumberOfMessageNotRead(String userid, TextView textView) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);

                    if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid) && chat.getIsSeen().equals("0") ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(firebaseUser.getUid()) && chat.getIsSeen().equals("0")) {
                        count++;
                    }

                    if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid)) {
                        textView.setVisibility(View.VISIBLE);
                        if (count == 0) {
                            textView.setVisibility(View.GONE);
                        }
                        textView.setText(String.valueOf(count));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
