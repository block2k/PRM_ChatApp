package com.example.chatapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.model.GroupMessage;
import com.google.firebase.auth.FirebaseAuth;
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

public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.ViewHolder> {
    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private List<GroupMessage> groupMessageList;

    private FirebaseAuth firebaseAuth;

    public GroupMessageAdapter(Context context, List<GroupMessage> groupMessageList) {
        this.context = context;
        this.groupMessageList = groupMessageList;
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public GroupMessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.group_chat_item_right, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.group_chat_item_left, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull GroupMessageAdapter.ViewHolder holder, int position) {
        GroupMessage model = groupMessageList.get(position);

        // set info message group chat
        loadMessageFromGroupChat(holder, model);

        clickDisplayTimeMessage(holder);
    }

    private void clickDisplayTimeMessage(@NonNull ViewHolder holder) {
        holder.group_message.setOnClickListener(new View.OnClickListener() {
            boolean clickMessage = false;
            @Override
            public void onClick(View view) {
                if (!clickMessage) {
                    holder.timestamp.setVisibility(View.VISIBLE);
                    clickMessage = true;
                } else {
                    holder.timestamp.setVisibility(View.GONE);
                    clickMessage = false;
                }
            }
        });
        holder.message_image.setOnClickListener(new View.OnClickListener() {
            boolean clickMessage = false;
            @Override
            public void onClick(View view) {
                if (!clickMessage) {
                    holder.timestamp.setVisibility(View.VISIBLE);
                    clickMessage = true;
                } else {
                    holder.timestamp.setVisibility(View.GONE);
                    clickMessage = false;
                }
            }
        });
    }

    private void loadMessageFromGroupChat(@NonNull ViewHolder holder, GroupMessage model) {
        // if message type is image
        if (model.getType().equals("text")) {
            holder.group_message.setText(model.getMessage());
        } else {
            holder.group_message.setVisibility(View.GONE);
            holder.message_image.setVisibility(View.VISIBLE);
            Glide.with(context).load(model.getMessage()).into(holder.message_image);
        }

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("id").equalTo(model.getSender()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String name = dataSnapshot.child("username").getValue().toString();
                    String imageURL = dataSnapshot.child("imageURL").getValue().toString();

                    holder.group_sender.setText(name);
                    if ("default".equals(imageURL)) {
                        holder.group_profile_image.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        Glide.with(context).load(imageURL).into(holder.group_profile_image);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

//        set time message
        DateFormat simple = new SimpleDateFormat("MMM d, h:mm a");
        long timeStamp = Long.parseLong(model.getTimestamp());
        Date date = new Date(timeStamp);
        holder.timestamp.setText(simple.format(date));
    }

    @Override
    public int getItemCount() {
        return groupMessageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // nếu người đang login gửi, hiển thị tin nhắn bên phải
        if (groupMessageList.get(position).getSender().equals(firebaseAuth.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView group_profile_image;
        TextView group_sender, group_message, timestamp;
        ImageView message_image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            group_profile_image = itemView.findViewById(R.id.group_profile_image);
            group_sender = itemView.findViewById(R.id.group_sender);
            group_message = itemView.findViewById(R.id.group_message);
            timestamp = itemView.findViewById(R.id.timestamp);
            message_image = itemView.findViewById(R.id.message_image);
        }
    }
}
