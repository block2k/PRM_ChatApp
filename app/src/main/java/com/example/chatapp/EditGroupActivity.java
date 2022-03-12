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

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditGroupActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference reference;

    private String groupId;

    private CircleImageView group_icon;
    private EditText edt_group_title, edt_group_description;
    private FloatingActionButton btn_edit_group;

    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Edit Group Info");

        groupId = getIntent().getStringExtra("groupId").toString();

        bindingView();

        storageReference = FirebaseStorage.getInstance().getReference("uploads");


        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        group_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });

        btn_edit_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateGroupInfo();
            }
        });
        loadGroupInfo();
    }

    private void updateGroupInfo() {
        String groupTitle = edt_group_title.getText().toString().trim();
        String groupDescription = edt_group_description.getText().toString().trim();

        if (TextUtils.isEmpty(groupTitle)) {
            Toast.makeText(this, "Group title is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imageUri == null) {
            progressDialog.setMessage("Updating Group Info");
            progressDialog.dismiss();

            // update group without icon
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("groupTitle", groupTitle);
            hashMap.put("groupDescription", groupDescription);

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
            reference.child(groupId).updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            progressDialog.dismiss();
                            Toast.makeText(EditGroupActivity.this, "Update group info successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(EditGroupActivity.this, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
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

                        // update group without icon
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("groupTitle", groupTitle);
                        hashMap.put("groupDescription", groupDescription);
                        hashMap.put("groupIcon", mUri);

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
                        reference.child(groupId).updateChildren(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        progressDialog.dismiss();
                                        Toast.makeText(EditGroupActivity.this, "Update group info successfully", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(EditGroupActivity.this, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

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
        }
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

    private void loadGroupInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.orderByChild("groupId").equalTo(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String groupName = dataSnapshot.child("groupTitle").getValue().toString();
                    String groupDescription = dataSnapshot.child("groupDescription").getValue().toString();
                    String groupIcon = dataSnapshot.child("groupIcon").getValue().toString();

                    // set info group
                    if ("default".equals(groupIcon)) {
                        group_icon.setImageResource(R.drawable.ic_launcher_foreground);
                    } else {
                        Glide.with(getApplicationContext()).load(groupIcon).into(group_icon);
                    }
                    edt_group_title.setText(groupName);
                    edt_group_description.setText(groupDescription);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            group_icon.setImageURI(imageUri);
        }
    }

    private void bindingView() {
        group_icon = findViewById(R.id.group_icon);
        edt_group_title = findViewById(R.id.edt_group_title);
        edt_group_description = findViewById(R.id.edt_group_description);
        btn_edit_group = findViewById(R.id.btn_create_group);
    }
}