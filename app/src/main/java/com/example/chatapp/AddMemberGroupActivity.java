package com.example.chatapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.adapter.AddMemberGroupAdapter;
import com.example.chatapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddMemberGroupActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    private EditText search_users;

    private FirebaseAuth firebaseAuth;

    private String groupId, myGroupRole;

    private Toolbar toolbar;

    private List<User> userList;

    private AddMemberGroupAdapter addMemberGroupAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member_group);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add member");

        firebaseAuth = FirebaseAuth.getInstance();
        groupId = getIntent().getStringExtra("groupId");

        bindingView();

        eventSearchUser();

        loadGroupInfo();
    }

    private void eventSearchUser() {
        search_users.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchUser(search_users.getText().toString().trim().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void searchUser(String text) {
        userList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);

                    if (!user.getId().equals(firebaseAuth.getUid())) {
                        if (user.getUsername().toLowerCase().contains(text.toLowerCase())
                                || user.getEmail().toLowerCase().contains(text.toLowerCase())) {
                            userList.add(user);
                        }
                    }
                }

                addMemberGroupAdapter = new AddMemberGroupAdapter(AddMemberGroupActivity.this, userList, groupId, myGroupRole);
                recyclerView.setAdapter(addMemberGroupAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadGroupInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups");
        reference.orderByChild("groupId").equalTo(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
//                    String groupId = dataSnapshot.child("groupId").getValue().toString();
                    String groupTitle = dataSnapshot.child("groupTitle").getValue().toString();
                    String groupDes = dataSnapshot.child("groupDescription").getValue().toString();
                    String groupIcon = dataSnapshot.child("groupIcon").getValue().toString();
                    String createdBy = dataSnapshot.child("createdBy").getValue().toString();

                    reference1.child(groupId).child(AddMemberGroupAdapter.PARTICIPANTS_NODE).child(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                myGroupRole = snapshot.child("role").getValue().toString();
                                toolbar.setTitle(groupTitle + " (" + myGroupRole + ")");
                                searchUser(search_users.getText().toString());
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

    private void bindingView() {
        recyclerView = findViewById(R.id.recycler_view);
        search_users = findViewById(R.id.search_users);
    }
}