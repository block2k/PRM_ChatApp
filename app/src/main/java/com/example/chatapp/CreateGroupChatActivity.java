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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateGroupChatActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference reference;

    private CircleImageView group_icon;
    private EditText edt_group_title, edt_group_description;
    private FloatingActionButton btn_create_group;

    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create Group");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        toolbar.setSubtitle(firebaseUser.getEmail());

        bindingView();

        // select group image
        group_icon.setOnClickListener(this::onClickGroupIcon);

        // create group
        btn_create_group.setOnClickListener(this::onClickCreateGroup);
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void processCreateGroupHaveImage(String timestamp, String groupTitle, String groupDescription, Uri imageUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading");
        progressDialog.show();
        if (imageUri != null) {
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

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

                        processCreateGroup(timestamp, groupTitle, groupDescription, mUri);

                        progressDialog.dismiss();
                    } else {
                        Toast.makeText(getApplicationContext(), "Upload failed!", Toast.LENGTH_SHORT).show();
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
            group_icon.setImageURI(imageUri);
        }
    }

    private void onClickCreateGroup(View view) {
        createGroupChat();
    }

    private void createGroupChat() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating Group");

        String groupTitle = edt_group_title.getText().toString().trim();
        String groupDescription = edt_group_description.getText().toString().trim();

        if (TextUtils.isEmpty(groupTitle)) {
            Toast.makeText(this, "You must enter group title", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        String timestamp = String.valueOf(System.currentTimeMillis());

        if (imageUri == null) {
            // if user doesn't select image icon for group
            processCreateGroup(timestamp, groupTitle, groupDescription, "default");
        } else {
            // create group with image icon
            processCreateGroupHaveImage(timestamp, groupTitle, groupDescription, imageUri);
        }

    }

    private void processCreateGroup(String timestamp, String groupTitle, String groupDescription, String groupIcon) {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put("groupId", timestamp);
        hashMap.put("groupTitle", groupTitle);
        hashMap.put("groupDescription", groupDescription);
        hashMap.put("groupIcon", groupIcon);
        hashMap.put("timestamp", timestamp);
        hashMap.put("createdBy", firebaseUser.getUid());

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(timestamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                // add logged user to the group
                HashMap<String, String> hashMap1 = new HashMap<>();
                hashMap1.put("uid", firebaseAuth.getUid());
                hashMap1.put("role", "creator");
                hashMap1.put("timestamp", timestamp);

                DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups");
                reference1.child(timestamp).
                        child("Participants").
                        child(firebaseAuth.getUid()).
                        setValue(hashMap1).
                        addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                // Participants added success
                                progressDialog.dismiss();
                                Toast.makeText(CreateGroupChatActivity.this, "Group created", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(CreateGroupChatActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Participants added fail
                        progressDialog.dismiss();
                        Toast.makeText(CreateGroupChatActivity.this, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(CreateGroupChatActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onClickGroupIcon(View view) {
        openImage();
    }

    private void bindingView() {
        group_icon = findViewById(R.id.group_icon);
        edt_group_title = findViewById(R.id.edt_group_title);
        edt_group_description = findViewById(R.id.edt_group_description);
        btn_create_group = findViewById(R.id.btn_create_group);
    }
}