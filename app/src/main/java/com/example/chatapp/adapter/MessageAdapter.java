package com.example.chatapp.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.model.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;
    public static final String YOU_UNSENT_A_MESSAGE = "You unsent a message";
    public static final String WARNING_DELETE_MESSAGE = "Do you want to delete this message?";
    public static final String MSG_TYPE_IMAGE = "image";

    private Context mContext;
    private List<Chat> mChats;
    private String image_url;
    String userid, username;

    FirebaseUser firebaseUser;

    public MessageAdapter(Context mContext, List<Chat> mChats, String image_url, String userid, String username) {
        this.mContext = mContext;
        this.mChats = mChats;
        this.image_url = image_url;
        this.userid = userid;
        this.username = username;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new MessageAdapter.ViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        Chat chat = mChats.get(position);

        // display image view if message type is image, otherwise display textview
        if (chat.getType().equals(MSG_TYPE_IMAGE) && !chat.getMessage().equals(YOU_UNSENT_A_MESSAGE)) {
            holder.msg_img.setVisibility(View.VISIBLE);
            holder.show_message.setVisibility(View.GONE);
            Glide.with(mContext).load(chat.getMessage()).into(holder.msg_img);
        } else {
            holder.msg_img.setVisibility(View.GONE);
            holder.show_message.setVisibility(View.VISIBLE);
        }

        displayNameOfUserDeletedMessage(chat);

        loadProfileImage(holder, chat);

        eventDisplayTimeSentMessage(holder, chat);

        eventDeleteMessage(holder);

        displayStatusMessage(holder, position, chat);
    }

    private void displayNameOfUserDeletedMessage(@NonNull Chat chat) {
        if (chat.getMessage().equals(YOU_UNSENT_A_MESSAGE)) {
            if (!firebaseUser.getUid().equals(chat.getSender())) {
                chat.setMessage(userid + " unsent a message");
            }
        }
    }

    private void displayStatusMessage(@NonNull ViewHolder holder, int position, Chat chat) {
        if (position == mChats.size() - 1) {
            if (chat.getIsSeen().equals("1")) {
                holder.txt_seen.setText("Seen");
            } else {
                holder.txt_seen.setText("Delivered");
            }
        } else {
            holder.txt_seen.setVisibility(View.GONE);
        }
    }

    private void eventDeleteMessage(@NonNull ViewHolder holder) {
        holder.messageLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // show confirm dialog delete msg
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Delete");
                builder.setMessage(WARNING_DELETE_MESSAGE);

                // delete button
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteMessage(holder.getAdapterPosition());
                    }
                });

                // cancel button
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // dismiss dialog
                        dialogInterface.dismiss();
                    }
                }).show();
                return true;
            }
        });
    }

    private void eventDisplayTimeSentMessage(@NonNull ViewHolder holder, Chat chat) {
        holder.messageLayout.setOnClickListener(new View.OnClickListener() {
            boolean clickMessage = false;

            @Override
            public void onClick(View view) {
                if (!clickMessage) {
                    holder.timestamp.setVisibility(View.VISIBLE);
                    DateFormat simple = new SimpleDateFormat("MMM d, h:mm a");
                    long timeStamp = Long.parseLong(chat.getTime());
                    Date date = new Date(timeStamp);
                    holder.timestamp.setText(simple.format(date));
                    clickMessage = true;
                } else {
                    holder.timestamp.setVisibility(View.GONE);
                    clickMessage = false;
                }
            }
        });
    }

    private void loadProfileImage(@NonNull ViewHolder holder, Chat chat) {
        holder.show_message.setText(chat.getMessage());
        if ("default".equals(image_url)) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(image_url).into(holder.profile_image);
        }
    }

    /*
     * Logic:
     * get timestamp of clicked message
     * find in DB and delete */
    private void deleteMessage(int position) {
        String msgTimestamp = mChats.get(position).getTime();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = reference.orderByChild("time").equalTo(msgTimestamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);
                    if (chat.getSender().equals(firebaseUser.getUid())) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "You unsent a message");
                        dataSnapshot.getRef().updateChildren(hashMap);
                    } else {
                        Toast.makeText(mContext, "You can delete only your message", Toast.LENGTH_SHORT).show();
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
        return mChats.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView show_message, txt_seen, timestamp;
        public ImageView profile_image, msg_img;
        RelativeLayout messageLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            msg_img = itemView.findViewById(R.id.msg_img);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            timestamp = itemView.findViewById(R.id.timestamp);
            messageLayout = itemView.findViewById(R.id.messageLayout);
        }
    }

    @Override
    public int getItemViewType(int position) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        // nếu người đang login gửi, hiển thị tin nhắn bên phải
        if (mChats.get(position).getSender().equals(firebaseUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}
