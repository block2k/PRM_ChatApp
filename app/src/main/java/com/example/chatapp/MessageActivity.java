package com.example.chatapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.adapter.MessageAdapter;
import com.example.chatapp.model.Chat;
import com.example.chatapp.model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {
    CircleImageView profile_image, profile_image_typing;
    TextView username, block_user;

    FirebaseUser firebaseUser;
    DatabaseReference reference;

    Intent intent;

    ImageButton btn_send, btn_send_image;
    EditText text_send;

    MessageAdapter messageAdapter;
    List<Chat> mChat;

    RecyclerView recyclerView;

    ValueEventListener seenEventListener;

    String userid, userName;

    boolean isBlocked;

    RelativeLayout typingLayout, bottom;

    // upload image
    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        bindingView();

        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        intent = getIntent();
        userid = intent.getStringExtra("userid");
        isBlocked = intent.getStringExtra("blocked").equals("yes");

        //send message
        btn_send.setOnClickListener(this::onClickButtonSentMessage);

        // button send image
        btn_send_image.setOnClickListener(this::onClickButtonSentImage);

        eventTypingMessage();

        // block, unblock user
        block_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (block_user.getText().toString().equals("Block User")) {
                    blockUser(userid);
                } else {
                    unBlockUser(userid);
                }
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                username.setText(user.getUsername());
                userName = user.getUsername();
                // display image profile in chat
                if (user.getImageURL().equals("default")) {
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }

                // display typing status
                if (user.getTypingTo().equals(firebaseUser.getUid())) {
                    typingLayout.setVisibility(View.VISIBLE);
                    if (user.getImageURL().equals("default")) {
                        profile_image_typing.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image_typing);
                    }
                } else {
                    typingLayout.setVisibility(View.GONE);
                }

                loadMessage(firebaseUser.getUid(), userid, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        seenMessage(userid);

        // kiểm tra xem user đang đăng nhập có bị chặn bởi những user khác không
        checkLoggedUserIsBlockedByOtherUserOrNot();

        // kiểm tra xem user đang đăng nhập có chặn tin nhắn của user này không
        checkLoggedUserBlockedThisUser();
    }

    private void checkLoggedUserBlockedThisUser() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseUser.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(userid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    if (dataSnapshot.exists()) {
                        block_user.setText("Unblock User");
                        bottom.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void eventTypingMessage() {
        text_send.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() == 0) {
                    checkTypingStatus("noOne");
                } else {
                    checkTypingStatus(userid); //user id of receiver
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void checkLoggedUserIsBlockedByOtherUserOrNot() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(userid).child("BlockedUsers").orderByChild("uid").equalTo(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    if (dataSnapshot.exists()) {
                        bottom.setVisibility(View.GONE);
                        block_user.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void blockUser(String blockId) {
        // block user by adding uid of current to BlockedUser node

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", blockId);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseUser.getUid()).child("BlockedUsers").child(blockId).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                bottom.setVisibility(View.GONE);
                block_user.setText("Unblock User");
                Toast.makeText(getApplicationContext(), "Blocked user", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unBlockUser(String unBlockId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseUser.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(unBlockId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    if (dataSnapshot.exists()) {
                        dataSnapshot.getRef().removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                // unblock success
                                bottom.setVisibility(View.VISIBLE);
                                block_user.setText("Block User");
                                Toast.makeText(getApplicationContext(), "Unblocked user", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending");
        progressDialog.show();
        if (imageUri != null) {
            StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            uploadTask = fileReference.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri dowUri = task.getResult();
                        String mUri = dowUri.toString();

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("sender", firebaseUser.getUid());
                        hashMap.put("receiver", userid);
                        hashMap.put("message", mUri);
                        hashMap.put("time", String.valueOf(System.currentTimeMillis()));
                        hashMap.put("isSeen", "0");
                        hashMap.put("type", "image");

                        reference.child("Chats").push().setValue(hashMap);

                        progressDialog.dismiss();
                    } else {
                        Toast.makeText(getApplicationContext(), "Send failed!", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "No image selected!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(getApplicationContext(), "Send in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }

    private void seenMessage(String userId) {
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenEventListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Chat chat = dataSnapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userId)) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isSeen", "1");
                        dataSnapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadMessage(String sender, String receiver, String image_url) {
        mChat = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChat.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()
                ) {
                    Chat chat = dataSnapshot.getValue(Chat.class);

                    // get message between sender and receiver
                    if (chat.getReceiver().equals(receiver) && chat.getSender().equals(sender) ||
                            chat.getSender().equals(receiver) && chat.getReceiver().equals(sender)) {
                        mChat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, image_url, userName, userName);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String sender, String receiver, String message) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("time", String.valueOf(System.currentTimeMillis()));
        hashMap.put("isSeen", "0");
        hashMap.put("type", "text");

        reference.child("Chats").push().setValue(hashMap);

    }

    private void bindingView() {
        recyclerView = findViewById(R.id.recycler_view);
        profile_image = findViewById(R.id.profile_image);
        btn_send_image = findViewById(R.id.btn_send_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);
        typingLayout = findViewById(R.id.typing);
        block_user = findViewById(R.id.block_user);
        profile_image_typing = findViewById(R.id.profile_image_typing);
        bottom = findViewById(R.id.bottom);
    }

    private void checkOnlineStatus(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOnlineStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenEventListener);
        checkOnlineStatus("offline");
        checkTypingStatus("noOne");
    }

    private void onClickButtonSentImage(View view) {
        openImage();
    }

    private void onClickButtonSentMessage(View view) {
        String msg = text_send.getText().toString().trim();
        if (!"".equals(msg)) {
            sendMessage(firebaseUser.getUid(), userid, msg);
        } else {
            Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
        }
        text_send.setText("");
    }
}