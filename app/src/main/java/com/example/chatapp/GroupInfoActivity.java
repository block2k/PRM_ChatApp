package com.example.chatapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.adapter.AddMemberGroupAdapter;
import com.example.chatapp.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupInfoActivity extends AppCompatActivity {

    private CircleImageView group_icon;
    private TextView textview_leave_group, textview_add_member, group_name, group_description, textview_edit_group, group_number_member;
    private RecyclerView recycler_view;

    private FirebaseAuth firebaseAuth;

    private String groupId, myGroupRole;

    private List<User> userList;
    private AddMemberGroupAdapter addMemberGroupAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Group Info");

        groupId = getIntent().getStringExtra("groupId").toString();
        firebaseAuth = FirebaseAuth.getInstance();

        bindingView();

        textview_add_member.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GroupInfoActivity.this, AddMemberGroupActivity.class);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
            }
        });

        textview_leave_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if user is user, leave group
                // if user is creator, delete group
                String dialogTitle = "";
                String dialogDescription = "";
                String positiveButtonTitle = "";
                if (myGroupRole.equals("creator")) {
                    dialogTitle = "Delete Group";
                    dialogDescription = "Are you sure want to Delete group?";
                    positiveButtonTitle = "DELETE";
                } else {
                    dialogTitle = "Leave Group";
                    dialogDescription = "Are you sure want to Leave group?";
                    positiveButtonTitle = "LEAVE";
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(GroupInfoActivity.this);
                builder.setTitle(dialogTitle)
                        .setMessage(dialogDescription)
                        .setPositiveButton(positiveButtonTitle, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (myGroupRole.equals("creator")) {
                                    deleteGroupChat();
                                } else {
                                    leaveGroupChat();
                                }
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
        });
        textview_edit_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GroupInfoActivity.this, EditGroupActivity.class);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
            }
        });

        loadGroupInfo();
        loadMyGroupRole();
    }

    private void leaveGroupChat() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child(AddMemberGroupAdapter.PARTICIPANTS_NODE).child(firebaseAuth.getUid()).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // leave group successfully
                        Toast.makeText(GroupInfoActivity.this, "Leave group successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(GroupInfoActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // leave group fail
                        Toast.makeText(GroupInfoActivity.this, "FAIL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteGroupChat() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(GroupInfoActivity.this, "Delete group successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(GroupInfoActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GroupInfoActivity.this, "FAIL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMyGroupRole() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child(AddMemberGroupAdapter.PARTICIPANTS_NODE).orderByChild("uid").equalTo(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    myGroupRole = dataSnapshot.child("role").getValue().toString();

                    if (myGroupRole.equals(AddMemberGroupAdapter.ROLE_MEMBER)) {
                        textview_edit_group.setVisibility(View.GONE);
                        textview_add_member.setVisibility(View.GONE);
                    } else if (myGroupRole.equals(AddMemberGroupAdapter.ROLE_ADMIN)) {
                        textview_edit_group.setVisibility(View.GONE);
                    } else if (myGroupRole.equals(AddMemberGroupAdapter.ROLE_CREATOR)) {
                        textview_leave_group.setText("Delete Group");
                    }
                }
                loadMemberGroup();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadMemberGroup() {
        userList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child(AddMemberGroupAdapter.PARTICIPANTS_NODE).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String uid = dataSnapshot.child("uid").getValue().toString();

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                    reference.orderByChild("id").equalTo(uid).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                User user = dataSnapshot.getValue(User.class);
                                userList.add(user);
                            }
                            group_number_member.setText(userList.size() + " members");
                            addMemberGroupAdapter = new AddMemberGroupAdapter(GroupInfoActivity.this, userList, groupId, myGroupRole);
                            recycler_view.setAdapter(addMemberGroupAdapter);
                            recycler_view.setLayoutManager(new LinearLayoutManager(GroupInfoActivity.this));
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
                    group_name.setText(groupName);
                    group_description.setText(groupDescription);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void bindingView() {
        group_icon = findViewById(R.id.group_icon);
        group_name = findViewById(R.id.group_name);
        group_description = findViewById(R.id.group_description);
        textview_edit_group = findViewById(R.id.textview_edit_group);
        textview_add_member = findViewById(R.id.textview_add_member);
        textview_leave_group = findViewById(R.id.textview_leave_group);
        recycler_view = findViewById(R.id.recycler_view);
        group_number_member = findViewById(R.id.group_number_member);
    }
}