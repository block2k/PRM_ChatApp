package com.example.chatapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.adapter.GroupMessageAdapter;
import com.example.chatapp.model.GroupMessage;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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

public class GroupChatActivity extends AppCompatActivity {
    private String groupId, myGroupRole;

    private CircleImageView group_icon;
    private TextView group_name;
    private RecyclerView recyclerView;
    private ImageButton btn_send_image, btn_send, btn_add_member;
    private EditText text_send;

    private FirebaseAuth firebaseAuth;

    private List<GroupMessage> groupMessageList;
    private GroupMessageAdapter groupMessageAdapter;

    // upload image, send image
    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(GroupChatActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                startActivity(new Intent(GroupChatActivity.this, MainActivity.class));

            }
        });

        bindingView();

        // display recycler view
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");

        firebaseAuth = FirebaseAuth.getInstance();

        btn_send.setOnClickListener(this::onClickButtonSendMessage);
        btn_send_image.setOnClickListener(this::onClickButtonSendImage);
        btn_add_member.setOnClickListener(this::onClickButtonAddMember);

        loadGroupInfo();

        loadGroupMessage();
    }

    private void onClickButtonAddMember(View view) {
        Intent intent = new Intent(GroupChatActivity.this, AddMemberGroupActivity.class);
        intent.putExtra("groupId", groupId);
        startActivity(intent);
    }


    private void onClickButtonSendImage(View view) {
        openImage();
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

                        sendMessage(mUri, "image");

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

    private void loadGroupMessage() {
        groupMessageList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupMessageList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    GroupMessage model = dataSnapshot.getValue(GroupMessage.class);
                    groupMessageList.add(model);
                }

                groupMessageAdapter = new GroupMessageAdapter(getApplicationContext(), groupMessageList);
                recyclerView.setAdapter(groupMessageAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void onClickButtonSendMessage(View view) {
        String message = text_send.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, Const.WARNING_SEND_MESSAGE, Toast.LENGTH_SHORT).show();
            return;
        }
        sendMessage(message, "text");
    }

    private void sendMessage(String message, String messageType) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", firebaseAuth.getUid());
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("type", messageType);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Messages").child(timestamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                // send message success
                text_send.setText("");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // send message fail
                Toast.makeText(GroupChatActivity.this, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void loadGroupInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.orderByChild("groupId").equalTo(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String groupTitle = dataSnapshot.child("groupTitle").getValue().toString();
                    String groupDescription = dataSnapshot.child("groupDescription").getValue().toString();
                    String groupIcon = dataSnapshot.child("groupIcon").getValue().toString();
                    String timestamp = dataSnapshot.child("timestamp").getValue().toString();
                    String createdBy = dataSnapshot.child("createdBy").getValue().toString();

                    group_name.setText(groupTitle);
                    if ("default".equals(groupIcon)) {
                        group_icon.setImageResource(R.drawable.ic_launcher_foreground);
                    } else {
                        Glide.with(getApplicationContext()).load(groupIcon).into(group_icon);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void bindingView() {
        recyclerView = findViewById(R.id.recycler_view);
        group_icon = findViewById(R.id.profile_image);
        group_name = findViewById(R.id.group_sender);
        btn_send_image = findViewById(R.id.btn_send_image);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);
        btn_add_member = findViewById(R.id.btn_add_member);
    }
}